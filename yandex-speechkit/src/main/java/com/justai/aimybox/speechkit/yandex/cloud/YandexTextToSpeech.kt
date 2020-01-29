package com.justai.aimybox.speechkit.yandex.cloud

import android.content.Context
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.speechtotext.SampleRate
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import kotlinx.coroutines.cancel

class YandexTextToSpeech(
    context: Context,
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    var defaultLanguage: Language,
    var config: Config = Config()
) : BaseTextToSpeech(context) {

    private val api = YandexSynthesisApi(iAmTokenProvider, folderId)

    override suspend fun speak(speech: TextSpeech) {
        try {
            val language = resolveLanguage(speech.language)
            val audioData = api.request(speech.text, language, config)
            audioSynthesizer.play(AudioSpeech.Bytes(audioData))
        } catch (e: Throwable) {
            throw YandexCloudTextToSpeechException(cause = e)
        }
    }

    private fun resolveLanguage(language: String?): Language {
        return if (language == null) return defaultLanguage
        else Language(language)
    }

    override fun destroy() {
        super.destroy()
        coroutineContext.cancel()
    }

    data class Config(
        val apiUrl: String = "https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize",
        val voice: Voice = Voice.ALYSS,
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_48KHZ,
        val emotion: Emotion = Emotion.NEUTRAL,
        val speed: Speed = Speed.DEFAULT
    )
}

