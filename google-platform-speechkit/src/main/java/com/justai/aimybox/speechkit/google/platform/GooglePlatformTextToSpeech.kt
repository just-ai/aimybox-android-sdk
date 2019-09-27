package com.justai.aimybox.speechkit.google.platform

import android.content.Context
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.speech.tts.TextToSpeech as GoogleTTS

@Suppress("unused", "MemberVisibilityCanBePrivate")
class GooglePlatformTextToSpeech(
    context: Context,
    var defaultLocale: Locale = Locale.getDefault()
) : BaseTextToSpeech(context) {

    companion object {
        const val DEFAULT_VOICE_PITCH = 1.0F
    }

    init {
        L.i("Initializing Google Platform TTS")
    }

    private val initialization = CompletableDeferred<Unit>()
    private val synthesizer = GoogleTTS(context) {
        initialization.complete(Unit)
        L.i("Google Platform TTS initialized")
    }

    private var voicePitch = DEFAULT_VOICE_PITCH

    @Suppress("DEPRECATION")
    override suspend fun speak(speech: TextSpeech) {
        initialization.await()

        synthesizer.setLanguageFrom(speech, defaultLocale)

        synthesizer.setPitch(voicePitch)

        suspendCancellableCoroutine<Unit> { continuation ->
            continuation.invokeOnCancellation { synthesizer.stop() }

            synthesizer.setOnUtteranceProgressListener(object : UtteranceProgressListener() {

                override fun onStop(utteranceId: String?, interrupted: Boolean) =
                    continuation.resume(Unit)

                override fun onError(utteranceId: String?, errorCode: Int) = onError(utteranceId)
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = continuation.resume(Unit)
                override fun onError(utteranceId: String?) =
                    continuation.resumeWithException(
                        GooglePlatformTextToSpeechException(
                            "GooglePlatformSpeechToTextException while synthesizing $speech"
                        )
                    )
            })
            synthesizer.speak(speech.text, GoogleTTS.QUEUE_FLUSH, null, speech.text)
        }
    }

    override suspend fun stop() {
        super.stop()
        synthesizer.stop()
    }

    suspend fun getVoice(): Voice {
        initialization.await()
        return synthesizer.voice
    }

    suspend fun setVoice(voice: Voice) {
        initialization.await()
        if (synthesizer.setVoice(voice) != GoogleTTS.SUCCESS) L.e("Failed to set voice $voice")
    }

    fun setVoicePitch(pitch: Float) {
        voicePitch = pitch
    }

    suspend fun getAvailableVoices(): Set<Voice> {
        initialization.await()
        return synthesizer.voices
    }

    suspend fun getAvailableLanguages(): Set<Locale> {
        initialization.await()
        return synthesizer.availableLanguages
    }

    private fun GoogleTTS.setLanguageFrom(speech: TextSpeech, default: Locale) {
        val locale = speech.language
            ?.takeIf(String::isNotBlank)
            ?.let(::Locale)
            ?: default

        if (synthesizer.setLanguage(locale) < 0) {
            L.e("Failed to set locale $locale")
            synthesizer.language = default
        }
    }

    override fun destroy() {
        super.destroy()
        synthesizer.shutdown()
    }
}
