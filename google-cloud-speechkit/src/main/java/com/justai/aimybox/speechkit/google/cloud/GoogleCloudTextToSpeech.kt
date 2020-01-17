package com.justai.aimybox.speechkit.google.cloud

import android.content.Context
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.Credentials
import com.google.cloud.texttospeech.v1.*
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.speechkit.google.cloud.model.Gender
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import java.util.*

class GoogleCloudTextToSpeech(
    context: Context,
    credentials: GoogleCloudCredentials,
    private val locale: Locale,
    private val config: Config = Config()
) : BaseTextToSpeech(context) {

    private val client = createAuthorizedClient(credentials.credentials)

    override suspend fun speak(speech: TextSpeech) {
        val input = SynthesisInput.newBuilder()
            .setText(speech.text).build()

        val voice = VoiceSelectionParams.newBuilder()
            .setLanguageCode(speech.language ?: locale.language)
            .setSsmlGender(config.gender.value)
            .setName(config.voice)
            .build()

        val audioConfig = AudioConfig.newBuilder()
            .setAudioEncoding(AudioEncoding.MP3)
            .setSpeakingRate(config.speakingRate)
            .setPitch(config.pitch)
            .build()

        val response = client.synthesizeSpeech(input, voice, audioConfig)
        audioSynthesizer.play(AudioSpeech.Bytes(response.audioContent.toByteArray()))
    }

    private fun createAuthorizedClient(credentials: Credentials): TextToSpeechClient {
        val settings = TextToSpeechSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build()

        return TextToSpeechClient.create(settings)
    }

    data class Config(
        val gender: Gender = Gender.NEUTRAL,
        val voice: String = "",
        val speakingRate: Double = 0.0,
        val pitch: Double = 0.0
    )
}