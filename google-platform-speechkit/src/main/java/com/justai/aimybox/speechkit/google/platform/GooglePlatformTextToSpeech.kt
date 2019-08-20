package com.justai.aimybox.speechkit.google.platform

import android.content.Context
import android.os.Build
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.annotation.RequiresApi
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.speech.tts.TextToSpeech as GoogleTTS

class GooglePlatformTextToSpeech(
    context: Context,
    private val defaultLocale: Locale = context.resources.configuration.locale,
    private val initialVoice: Voice? = null,
    private val voicePitch: Float = 1.0F
) : BaseTextToSpeech(context) {

    companion object {
        private val SUPPORTED_LOCALES = listOf(
            Locale.ENGLISH, Locale("ru", "RU")
        )
    }

    init {
        L.i("Initializing Google Platform TTS")
    }

    private val initDeferred = CompletableDeferred<Unit>()
    private val synthesizer = GoogleTTS(context) { initDeferred.complete(Unit) }

    val voices
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        get() = synthesizer.voices

    var voice
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        get() = synthesizer.voice
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        set(value) {
            if (synthesizer.setVoice(value) != GoogleTTS.SUCCESS) L.e("Failed to set voice $voice")
        }

    var language
        get() = synthesizer.language
        set(value) {
            if (synthesizer.setLanguage(value) < 0) {
                L.e("Failed to set language $value")
                synthesizer.language = defaultLocale
            }
        }

    @Suppress("DEPRECATION")
    override suspend fun speak(speech: TextSpeech) {
        initDeferred.await()

        language = resolveLocale(speech.language)
        if (initialVoice != null) voice = initialVoice
        synthesizer.setPitch(voicePitch)

        suspendCancellableCoroutine<Unit> { continuation ->
            continuation.invokeOnCancellation { synthesizer.stop() }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                synthesizer.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) = continuation.resume(Unit)
                    override fun onError(utteranceId: String?) = continuation
                        .resumeWithException(GooglePlatformTextToSpeechException("GooglePlatformSpeechToTextException while synthesizing $speech"))
                })
                synthesizer.speak(speech.text, GoogleTTS.QUEUE_FLUSH, null, speech.text)
            } else {
                synthesizer.setOnUtteranceCompletedListener { continuation.resume(Unit) }
                synthesizer.speak(speech.text, GoogleTTS.QUEUE_FLUSH, null)
            }
        }
    }


    private fun resolveLocale(languageTag: String?): Locale {
        return languageTag?.let { languageTag ->
            SUPPORTED_LOCALES.find { it.toString().contains(languageTag) }
        } ?: language
    }

    override fun destroy() {
        super.destroy()
        synthesizer.shutdown()
    }
}
