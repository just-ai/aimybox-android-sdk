package com.justai.aimybox.speechkit.yandex.cloud.stt

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechtotext.SpeechToText
import com.justai.aimybox.speechkit.yandex.cloud.AudioEncoding
import com.justai.aimybox.speechkit.yandex.cloud.L
import com.justai.aimybox.speechkit.yandex.cloud.Language
import com.justai.aimybox.speechkit.yandex.cloud.SampleRate
import com.justai.aimybox.speechkit.yandex.cloud.VoiceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import yandex.cloud.ai.stt.v2.SttServiceOuterClass.RecognitionSpec.AudioEncoding as InternalAudioEncoding

class YandexSpeechToText(
    yandexPassportOAuthKey: String,
    folderId: String,
    language: Language,
    config: Config = Config()
) : SpeechToText(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    private val audioRecorder = AudioRecorder(config.sampleRate.intValue)

    private val api =
        YandexRecognitionApi(yandexPassportOAuthKey, folderId, language, config)

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
            { exception -> sendResult(Result.Exception(SpeechToTextException(exception))) },
            onCompleted = { close() }
        )

        val audioData = audioRecorder.startAudioRecording()

        launch {
            audioData.consumeEach { data ->
                requestStream.onNext(YandexRecognitionApi.createRequest(data))
            }
        }

        invokeOnClose {
            requestStream.onCompleted()
            audioData.cancel()
        }
    }

    override fun stopRecognition() {
        audioRecorder.stopAudioRecording()
    }

    override fun cancelRecognition() {
        coroutineContext.cancelChildren()
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

