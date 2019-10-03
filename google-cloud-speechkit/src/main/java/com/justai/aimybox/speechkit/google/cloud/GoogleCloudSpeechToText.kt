package com.justai.aimybox.speechkit.google.cloud

import com.google.api.gax.rpc.ClientStream
import com.google.api.gax.rpc.ResponseObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.speech.v1.*
import com.google.protobuf.ByteString
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechtotext.SampleRate
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import java.util.*

class GoogleCloudSpeechToText(
    var locale: Locale,
    var sampleRate: SampleRate,
    var channelCount: Int = 1,
    var enablePunctuation: Boolean = false,
    var profanityFilter: Boolean = false,
    var recognitionModel: RecognitionModel = RecognitionModel.DEFAULT
) : SpeechToText(), CoroutineScope {
    override val coroutineContext = Dispatchers.IO

    private val speechClient = SpeechClient.create()

    private val audioRecorder = AudioRecorder(
        name = "Google Cloud Recognition",
        sampleRate = sampleRate.intValue,
        channelCount = channelCount
    )


    override fun startRecognition() = produce<Result> {
        val stream = speechClient.streamingRecognizeCallable()
            .splitCall(CloudResponseObserver(channel))

        stream.sendConfig()

        val audioData = audioRecorder.startRecordingBytes()

        launch {
            audioData.consumeEach { data ->
                StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(data))
                    .build()
                    .let(stream::send)
            }
            stream.closeSend()
        }

        invokeOnClose {
            audioData.cancel()
            stream.closeSend()
        }
    }

    override suspend fun stopRecognition() {
        audioRecorder.stopAudioRecording()
    }

    override suspend fun cancelRecognition() {
        coroutineContext.cancelChildrenAndJoin()
    }

    private fun ClientStream<StreamingRecognizeRequest>.sendConfig() =
        StreamingRecognizeRequest.newBuilder()
            .setStreamingConfig(createConfig())
            .build()
            .let(::send)

    private fun createConfig() = StreamingRecognitionConfig.newBuilder()
        .setConfig(
            RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setAudioChannelCount(channelCount)
                .setSampleRateHertz(sampleRate.intValue)
                .setMaxAlternatives(0)
                .setEnableAutomaticPunctuation(enablePunctuation)
                .setLanguageCode(locale.language)
                .setProfanityFilter(profanityFilter)
                .setModel(recognitionModel.stringValue)
                .build()
        )
        .setInterimResults(true)
        .build()

    private class CloudResponseObserver(
        private val channel: SendChannel<Result>
    ) : ResponseObserver<StreamingRecognizeResponse> {
        override fun onComplete() {
            channel.close()
        }

        override fun onResponse(response: StreamingRecognizeResponse) {
            val apiResult = response.resultsList.first()

            val text = apiResult.alternativesList.first()
                .wordsList
                .joinToString(" ")
                .takeIf { it.isNotBlank() }

            sendResult(if (apiResult.isFinal) Result.Final(text) else Result.Partial(text))
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
}