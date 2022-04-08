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

    private val api = YandexRecognitionApi(iAmTokenProvider, folderId, language, config)

    fun setLanguage(language: Language) = api.setLanguage(language)

    private var recognitionChannel: ReceiveChannel<Result>? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): ReceiveChannel<Result> {
        initCounter()
        return produce<Result> {
            try {
                val requestStream = api.openStream(
                    { response ->
                        val chunk = response.chunksList.first()
                        val text = chunk.alternativesList.first().text
                        val result = if (chunk.final) Result.Final(text) else Result.Partial(text)
                        sendResult(result)
                    },
                    { exception -> sendResult(Result.Exception(YandexCloudSpeechToTextException(cause = exception))) },
                    onCompleted = { close() }
                )

                val audioData = audioRecorder.startRecordingBytes()

                launch {
                    audioData.collect { data ->
                        requestStream.onNext(YandexRecognitionApi.createRequest(data))
                        onAudioBufferReceived(data)
                        if (mustInterruptRecognition) {
                            L.w( "Interrupting stream")
                            this@produce.cancel()
                        }
                    }
                }

                invokeOnClose {
                    requestStream.onCompleted()
                }
            } catch (e: Exception) {
                sendResult(Result.Exception(YandexCloudSpeechToTextException(cause = e)))
            }
        }
    }

    override suspend fun stopRecognition() = cancelRecognition()
//    {
//        audioRecorder.stopAudioRecording()
//    }

    override suspend fun cancelRecognition() {
        coroutineContext.cancelChildrenAndJoin()
    }

    override fun destroy() {
        api.cancel()
        coroutineContext.cancel()
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

