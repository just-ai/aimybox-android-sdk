package com.justai.aimybox.speechkit.yandex.cloud

import android.content.Context
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import kotlinx.coroutines.cancel

abstract class AbstractYandexTextToSpeech<T : AbstractYandexTextToSpeech.BaseConfig>(
    context: Context,
    protected val iAmTokenProvider: IAmTokenProvider,
    protected val folderId: String,
    var defaultLanguage: Language,
    val config: T
) : BaseTextToSpeech(context) {

    abstract val api: AbstractYandexSynthesisApi

    override suspend fun speak(speech: TextSpeech) {
        try {
            val language = resolveLanguage(speech.language)
            val audioData = api.request(speech.text, language)
            onEvent(Event.SpeechDataReceived(audioData))
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

    abstract class BaseConfig(
        open val apiUrl: String,
        open val voice: Voice,
        open val enableLoggingData: Boolean,
    )
}

