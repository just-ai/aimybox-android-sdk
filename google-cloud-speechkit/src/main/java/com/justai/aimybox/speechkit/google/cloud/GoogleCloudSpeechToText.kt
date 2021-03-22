package com.justai.aimybox.speechkit.google.cloud

import android.Manifest
import androidx.annotation.RequiresPermission
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.rpc.ClientStream
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.auth.Credentials
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechkit.google.cloud.model.RecognitionModel
import com.justai.aimybox.speechtotext.SampleRate
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.collect
import java.util.*

@Suppress("unused")
@ExperimentalCoroutinesApi
class GoogleCloudSpeechToText(
    credentials: GoogleCloudCredentials,
    private val locale: Locale,
    private val config: Config = Config()
) : SpeechToText(), CoroutineScope {

    override val coroutineContext = Dispatchers.IO

    private val speechClient = createAuthorizedClient(credentials.credentials)

    private val audioRecorder = AudioRecorder(
        name = "Google Cloud Recognition",
        sampleRate = config.sampleRate.intValue,
        channelCount = config.channelCount
    )

    @InternalCoroutinesApi
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition() = produce<Result> {
        val stream = speechClient.streamingRecognizeCallable()
            .splitCall(CloudResponseObserver(channel))

        stream.sendRecognitionConfig()

        val audioData = audioRecorder.startRecordingBytes()

        launch {
            audioData.collect { data ->
                StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(data))
                    .build()
                    .let(stream::send)

                onAudioBufferReceived(data)
            }
            stream.closeSend()
        }

        invokeOnClose {
            stream.closeSend()
        }
    }

    override suspend fun stopRecognition() {
        audioRecorder.stopAudioRecording()
    }

    override suspend fun cancelRecognition() {
        coroutineContext.cancelChildrenAndJoin()
    }

    private fun ClientStream<StreamingRecognizeRequest>.sendRecognitionConfig() =
        StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(createRecognitionConfig())
            .build()
            .let(::send)

    private fun createRecognitionConfig() = StreamingRecognitionConfig.newBuilder()
        .setConfig(
            RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setAudioChannelCount(config.channelCount)
                .setSampleRateHertz(config.sampleRate.intValue)
                .setMaxAlternatives(0)
                .setEnableAutomaticPunctuation(config.enablePunctuation)
                .setLanguageCode(locale.language)
                .setProfanityFilter(config.profanityFilter)
                .setModel(config.recognitionModel.stringValue)
                .build()
        )
        .setInterimResults(config.sendPartialResults)
        .setSingleUtterance(config.isSingleUtterance)
        .build()

    private class CloudResponseObserver(
        private val channel: SendChannel<Result>
    ) : ResponseObserver<StreamingRecognizeResponse> {
        override fun onComplete() {
            channel.close()
        }

        override fun onResponse(response: StreamingRecognizeResponse) {
            val apiResult = response.resultsList.firstOrNull()

            val text = apiResult?.alternativesList?.firstOrNull()
                ?.transcript
                ?.takeIf { it.isNotBlank() }

            if (response.speechEventType == StreamingRecognizeResponse.SpeechEventType.END_OF_SINGLE_UTTERANCE) {
                L.w("END OF UTTERANCE")
            }

            sendResult(if (apiResult?.isFinal == true) Result.Final(text) else Result.Partial(text))
        }

        override fun onError(t: Throwable) =
            sendResult(Result.Exception(GoogleCloudSpeechToTextException(cause = t)))

        override fun onStart(controller: StreamController?) {}

        private fun sendResult(result: Result) {
            if (channel.isClosedForSend) {
                L.w("Channel $this is closed. Omitting $result.")
            } else {
                val isSuccess = channel.offer(result)
                if (!isSuccess) L.w("Failed to send $result to $this")
            }
        }
    }

    private fun createAuthorizedClient(credentials: Credentials): SpeechClient {
        val speechSettings = SpeechSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build()

        return SpeechClient.create(speechSettings)
    }

    data class Config(
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_16KHZ,
        val channelCount: Int = 1,
        val enablePunctuation: Boolean = false,
        val profanityFilter: Boolean = false,
        val recognitionModel: RecognitionModel = RecognitionModel.COMMAND_AND_SEARCH,
        val sendPartialResults: Boolean = true,
        val isSingleUtterance: Boolean = true
    )
}