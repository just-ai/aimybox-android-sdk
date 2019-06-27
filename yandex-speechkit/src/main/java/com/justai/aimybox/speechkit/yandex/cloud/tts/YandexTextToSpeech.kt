package com.justai.aimybox.speechkit.yandex.cloud.tts

import android.content.Context
import android.media.MediaPlayer
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.speechkit.yandex.cloud.Emotion
import com.justai.aimybox.speechkit.yandex.cloud.L
import com.justai.aimybox.speechkit.yandex.cloud.Language
import com.justai.aimybox.speechkit.yandex.cloud.SampleRate
import com.justai.aimybox.speechkit.yandex.cloud.Speed
import com.justai.aimybox.speechkit.yandex.cloud.Voice
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.*
import kotlin.coroutines.resume

class YandexTextToSpeech(
    context: Context,
    yandexPassportOAuthKey: String,
    folderId: String,
    var defaultLanguage: Language,
    var config: Config = Config()
) : BaseTextToSpeech(context) {

    private val api = YandexSynthesisApi(yandexPassportOAuthKey, folderId)

    private val mediaPlayer = MediaPlayer()
    private var isSpeaking = false

    override fun isSpeaking() = isSpeaking

    override suspend fun speak(speech: TextSpeech) {
        isSpeaking = true
        val language = resolveLanguage(speech.language)
        val audioData = api.request(speech.text, language, config)

        val file = File.createTempFile("speech_", UUID.randomUUID().toString())

        try {
            file.writeBytes(audioData)

            coroutineScope {
                launch {
                    mediaPlayer.apply {
                        setDataSource(file.path)
                        prepare()
                        playSuspendable()
                    }
                }.invokeOnCompletion {
                    mediaPlayer.stop()
                    mediaPlayer.reset()
                    isSpeaking = false
                }
            }
        } finally {
            file.delete()
        }
    }

    private fun resolveLanguage(language: String?): Language {
        if (language == null) return defaultLanguage
        val resolvedLanguage = Language.values().find { it.stringValue.contains(language) }
        return if (resolvedLanguage != null) {
            resolvedLanguage
        } else {
            L.w("Failed to resolve language \"$language\". Using default \"$defaultLanguage\" language instead.")
            defaultLanguage
        }
    }

    private suspend fun MediaPlayer.playSuspendable() {
        suspendCancellableCoroutine<Unit> { continuation ->
            setOnCompletionListener {
                continuation.resume(Unit)
            }
            setOnErrorListener { _, what, _ ->
                L.e("MediaPlayer error code $what. Stopping player.")
                continuation.resume(Unit)
                true
            }
            start()
        }
    }

    override fun destroy() {
        super.destroy()
        coroutineContext.cancel()
        mediaPlayer.release()
    }

    data class Config(
        val apiUrl: String = "https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize",
        val voice: Voice = Voice.ALYSS,
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_48KHZ,
        val emotion: Emotion = Emotion.NEUTRAL,
        val speed: Speed = Speed.DEFAULT
    )
}

