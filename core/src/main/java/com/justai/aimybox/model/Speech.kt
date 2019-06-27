package com.justai.aimybox.model

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes

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
    /**
     * Set data source to provided [mediaPlayer].
     * */
    abstract fun load(context: Context, mediaPlayer: MediaPlayer)

    /**
     * Audio speech located by [uri].
     * */
    data class Uri(val uri: String) : AudioSpeech() {
        override fun load(context: Context, mediaPlayer: MediaPlayer) = mediaPlayer.setDataSource(uri)
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
}