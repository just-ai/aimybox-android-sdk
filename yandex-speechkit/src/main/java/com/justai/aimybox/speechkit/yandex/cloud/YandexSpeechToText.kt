package com.justai.aimybox.speechkit.yandex.cloud

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class YandexSpeechToText(
    yandexPassportOAuthKey: String,
    folderId: String,
    language: Language,
    config: Config = Config()
) : SpeechToText(), CoroutineScope {

    override val recognitionTimeoutMs = 10000L

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    private val audioRecorder = AudioRecorder("Yandex", config.sampleRate.intValue)

    private val api = YandexRecognitionApi(yandexPassportOAuthKey, folderId, language, config)

    fun setLanguage(language: Language) = api.setLanguage(language)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition() = produce<Result> {
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
            audioData.consumeEach { data ->
                requestStream.onNext(YandexRecognitionApi.createRequest(data))
            }
        }

        invokeOnClose {
            audioData.cancel()
            requestStream.onCompleted()
        }
    }

    override suspend fun stopRecognition() {
        audioRecorder.stopAudioRecording()
    }

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
        val encoding: AudioEncoding = AudioEncoding.PCM //TODO change to opus when implemented
    )
}

