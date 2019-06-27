package com.justai.aimybox.speechkit.google.cloud

import androidx.annotation.RequiresPermission
import com.google.api.gax.rpc.ApiStreamObserver
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEachIndexed
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class GoogleCloudSpeechToText(
    private val languageCode: String,
    credential: InputStream
) : SpeechToText(), CoroutineScope {

    companion object {
        private const val SPEECH_EVENT_UNSPECIFIED = "SPEECH_EVENT_UNSPECIFIED"
    }

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    init {
        L.i("Initializing Google Cloud STT")
    }

    private var speechClient = SpeechClient.create(
        SpeechSettings.newBuilder()
            .setCredentialsProvider { GoogleCredentials.fromStream(credential) }
            .build()
    )

    private val mAudioRecorder = AudioRecorder()

    init {
        L.i("Google Cloud STT initialized")
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun startRecognition() = produce<Result> {
        val requestStream = speechClient.streamingRecognizeCallable()
            .bidiStreamingCall(createStreamObserver(channel))

        launch {
            mAudioRecorder.startAudioRecording().consumeEachIndexed { (index, value) ->
                val builder = StreamingRecognizeRequest.newBuilder()
                    .setAudioContent(ByteString.copyFrom(value))
                    .apply { if (index == 0) addStreamingConfig() }

                requestStream.onNext(builder.build())
            }
        }.invokeOnCompletion { requestStream.onCompleted() }
    }

    override fun stopRecognition() {
        stopAudioRecording()
    }

    override fun cancelRecognition() {
        stopAudioRecording()
        coroutineContext.cancelChildren()
    }

    override fun destroy() {
        speechClient.shutdownNow()
        speechClient.awaitTermination(3, TimeUnit.SECONDS)
    }

    private fun createStreamObserver(channel: SendChannel<Result>): ApiStreamObserver<StreamingRecognizeResponse> {
        return object : ApiStreamObserver<StreamingRecognizeResponse> {
            override fun onNext(response: StreamingRecognizeResponse) {
                if (response.speechEventType.name == SPEECH_EVENT_UNSPECIFIED) {
                    val result = response.getResults(0)
                    val text = result.getAlternatives(0).transcript

                    if (result.isFinal) {
                        stopAudioRecording()
                        channel.sendResult(Result.Final(text))
                        channel.close()
                    } else {
                        channel.sendResult(Result.Partial(text))
                    }
                }
            }

            override fun onError(e: Throwable) {
                stopAudioRecording()
                channel.sendResult(Result.Exception(SpeechToTextException(e)))
                channel.close()
            }

            override fun onCompleted() {
                channel.close()
            }
        }
    }

    private fun stopAudioRecording() {
        mAudioRecorder.stopAudioRecording()
    }

    private fun StreamingRecognizeRequest.Builder.addStreamingConfig() {
        val recognitionConfig = RecognitionConfig.newBuilder()
            .setLanguageCode(languageCode)
            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
            .setSampleRateHertz(16000)
            .build()

        streamingConfig = StreamingRecognitionConfig.newBuilder()
            .setConfig(recognitionConfig)
            .setInterimResults(true)
            .setSingleUtterance(true)
            .build()
    }

    private fun SendChannel<Result>.sendResult(result: Result) {
        if (isClosedForSend) {
            L.w("Channel $this is closed. Omitting $result.")
        } else {
            offer(result).let { success ->
                if (!success) L.w("Failed to send $result to $this")
            }
        }
    }

}