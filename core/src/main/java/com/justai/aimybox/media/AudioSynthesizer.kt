package com.justai.aimybox.media

import android.content.Context
import android.media.MediaPlayer
import com.justai.aimybox.extensions.className
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.texttospeech.TextToSpeech
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Designed for usage inside [TextToSpeech] to play SSML audio and [AudioSpeech].
 *
 * @see TextToSpeech
 * */
class AudioSynthesizer(private val context: Context) {

    private val L = Logger(className)

    // private var mediaPlayer: MediaPlayer? = null

    private var mediaPlayer = MediaPlayer()


    // override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    suspend fun play(source: AudioSpeech) {
//        L.assert(contextJob.isActive) {
//            "Can't play $source: AudioSynthesizer is released."
//        }
//        L.assert(!contextJob.children.any { it.isActive }) {
//            "Can't play $source: AudioSynthesizer is busy."
//        }
        launchPlayer(source)
    }

    fun cancel() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.reset()
    }

    fun release() {
        mediaPlayer.release()
    }

    private suspend fun launchPlayer(source: AudioSpeech) {

        // mediaPlayer = MediaPlayer()

        try {
            withContext(Dispatchers.IO) {
                mediaPlayer.let {
                    source.load(context, it)
                }

            }
            suspendCancellableCoroutine<Unit> { continuation ->

                mediaPlayer.setOnCompletionListener { player ->
                    player.reset()
                    continuation.resume(Unit)
                }

                mediaPlayer.setOnErrorListener { player, what, _ ->
                    L.e("MediaPlayer error code $what. Stopping AudioSynthesizer.")
                    player.reset()
                    continuation.resume(Unit)
                    true
                }

                mediaPlayer.setOnPreparedListener { player ->
                        player.start()
                }

                mediaPlayer.prepareAsync()
            }

        } catch (e: CancellationException) {
            L.w("AudioSynthesizer is cancelled.")
        } catch (e: Throwable) {
            L.e(e)
        }
    }
}

