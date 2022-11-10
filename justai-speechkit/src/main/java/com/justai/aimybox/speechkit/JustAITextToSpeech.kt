package com.justai.aimybox.speechkit

import android.content.Context
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.speechkit.network.JASKServiceFactory
import com.justai.aimybox.speechkit.network.TTSService
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

/**
 * Example:
 *  var client = OkHttpClient()
 *  val config = JustAITextToSpeech.Config(token = "111111222233")
 *  val tts = JustAITextToSpeech(context, config, client)
 */

/**
 * @property context - Android application context
 *  @property config - configuration
 *  @property okHttpClient - OkHttpClient
 */
class JustAITextToSpeech(context: Context,
                         private val config: Config,
                         private val okHttpClient: OkHttpClient) : BaseTextToSpeech(context) {

    private val ttsService by lazy {
        JASKServiceFactory(config.baseUrl, okHttpClient).createService(TTSService::class.java)
    }

    override suspend fun speak(speech: TextSpeech) {
        try {
            val audioData = request(speech.text)
            onEvent(Event.SpeechDataReceived(audioData))
            audioSynthesizer.play(AudioSpeech.Bytes(audioData))
        } catch (e: Throwable) {
            throw (JustAITextToSpeechException(cause = e))
        }
    }

    private suspend fun request(text: String): ByteArray =
        withContext(IO) {
            val responseBody = ttsService.createTask(config.token, text)
            responseBody ?: throw Throwable("Response is empty.")
            responseBody.bytes()
        }

    /**
     *  @property token - a token to access to the voice
     *  @property baseUrl - the base TTS API url
     */
    data class Config(
        val token: String,
        val baseUrl: String = "https://aimyvoice.com/api/v1/"
    )
}