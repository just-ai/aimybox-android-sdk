package com.justai.aimybox.speechkit.yandex.cloud

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechtotext.SampleRate
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import yandex.cloud.api.ai.stt.v3.Stt
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class YandexSpeechToText(
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    language: Language,
    config: Config = Config(),
    maxAudioChunks: Int? = null,
    recognitionTimeout: Long = 10000L
) : SpeechToText(recognitionTimeout, maxAudioChunks) {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    private val audioRecorder = AudioRecorder("Yandex", config.sampleRate.intValue)

    private val api = YandexRecognitionApiV3(iAmTokenProvider, folderId, language, config)

    fun setLanguage(language: Language) = api.setLanguage(language)

    private var recognitionChannel: ReceiveChannel<Result>? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): ReceiveChannel<Result> {
        initCounter()
        return produce<Result> (context = coroutineContext){
            try {
                val requestStream = api.openStream(
                    { response ->
                        when (response.eventCase) {
                            Stt.StreamingResponse.EventCase.PARTIAL -> {
                                val alternativesList = response.partial.alternativesList
                                if (alternativesList.isNotEmpty()) {
                                    sendResult(Result.Partial(alternativesList.first().text))
                                }
                            }
                            Stt.StreamingResponse.EventCase.FINAL -> {
                                val alternativesList = response.final.alternativesList
                                if (alternativesList.isNotEmpty()) {
                                    sendResult(Result.Final(alternativesList.first().text))
                                }
                            }
                            else -> {
                            }
                        }
                    },
                    { exception ->
                        sendResult(
                            Result.Exception(
                                YandexCloudSpeechToTextException(
                                    cause = exception
                                )
                            )
                        )
                    },
                    onCompleted = { close() }
                )

                val audioData = audioRecorder.startRecordingBytes()

                launch {
                    audioData.collect { data ->
                        requestStream?.onNext(YandexRecognitionApiV3.createRequest(data))
                        onAudioBufferReceived(data)
                        if (mustInterruptRecognition) {
                            L.w("Interrupting stream")
                            this@produce.cancel()
                        }
                    }
                }

                invokeOnClose {
                    requestStream?.onCompleted()
                }
            } catch (e: Exception) {
                sendResult(Result.Exception(YandexCloudSpeechToTextException(cause = e)))
            }
        }
    }

    override suspend fun stopRecognition() {
        audioRecorder.stopAudioRecording()
    }

    override suspend fun cancelRecognition() {
        coroutineContext.cancelChildrenAndJoin()
    }

    override fun destroy() {
        coroutineContext.cancel()
    }

    private fun SendChannel<Result>.sendResult(result: Result) {
        if (isClosedForSend) {
            L.w("Channel $this is closed. Omitting $result.")
        } else {
            trySend(result).isSuccess.let { success ->
                if (!success) L.w("Failed to send $result to $this")
            }
        }
    }

    data class Config(
        val apiUrl: String = "stt.api.cloud.yandex.net",
        val apiPort: Int = 443,
        val voiceModel: VoiceModel = VoiceModel.GENERAL,
        val enableProfanityFilter: Boolean = true,
        val enablePartialResults: Boolean = true,
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_48KHZ,
        val rawResults: Boolean = false,
        val literatureText: Boolean = false,
        val enableLoggingData: Boolean = false,
        val normalizePartialData: Boolean = false,
        val pinningConfig: PinningConfig? = null
    )
}

