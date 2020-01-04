package com.justai.aimybox.model

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.justai.aimybox.logging.Logger
import java.io.File
import java.util.*

/**
 * Represents everything what a device should play as audios.
 * */
sealed class Speech

/**
 * Contains [text] to synthesize. If no [language] provided, synthesizer should use the default language.
 * */
data class TextSpeech(val text: String, val language: String? = null) : Speech()

/**
 * Contains pre-recorded speech, which synthesizer simply should play.
 * */
sealed class AudioSpeech : Speech() {
    protected val L = Logger("AudioSpeech")

    /**
     * Set data source to provided [mediaPlayer].
     * */
    abstract fun load(context: Context, mediaPlayer: MediaPlayer)

    /**
     * Audio speech located by [uri].
     * */
    data class Uri(val uri: String) : AudioSpeech() {
        override fun load(context: Context, mediaPlayer: MediaPlayer) =
            mediaPlayer.setDataSource(uri)
    }

    /**
     * Local raw audio resource.
     * */
    data class Raw(@RawRes val resourceId: Int) : AudioSpeech() {
        override fun load(context: Context, mediaPlayer: MediaPlayer) {
            context.resources.openRawResourceFd(resourceId).use { asset ->
                mediaPlayer.setDataSource(asset.fileDescriptor, 0, asset.declaredLength)
            }
        }
    }

    @Suppress("ArrayInDataClass")
    data class Bytes(val audioData: ByteArray) : AudioSpeech() {
        override fun load(context: Context, mediaPlayer: MediaPlayer) {
            val file = try {
                File.createTempFile("speech_", UUID.randomUUID().toString())
            } catch (e: Throwable) {
                L.e("Failed to create temp file", e)
                return
            }
            try {
                file.writeBytes(audioData)
                mediaPlayer.apply {
                    setDataSource(file.path)
                }
            } finally {
                file.delete()
            }
        }
    }
}