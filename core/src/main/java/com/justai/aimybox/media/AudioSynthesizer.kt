package com.justai.aimybox.media

import android.content.Context
import android.media.MediaPlayer
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.extensions.className
import com.justai.aimybox.extensions.contextJob
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.texttospeech.TextToSpeech
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    suspend fun cancel() {
        mediaPlayer?.stop()
        mediaPlayer?.reset()
    }

    fun release() {
        mediaPlayer?.release()
    }

    private suspend fun launchPlayer(source: AudioSpeech) {

       // mediaPlayer = MediaPlayer()

        try {
            withContext(Dispatchers.IO) {
                mediaPlayer?.let {
                    source.load(context, it)
                }

            }

            mediaPlayer?.setOnCompletionListener { player ->
                player.reset()
            }
            mediaPlayer?.setOnPreparedListener { player ->
                player.start()
            }
            mediaPlayer?.setOnErrorListener { player, what, _ ->
                L.e("MediaPlayer error code $what. Stopping AudioSynthesizer.")
                player.reset()
                true
            }

            mediaPlayer?.prepareAsync()

        } catch (e: CancellationException) {
            L.w("AudioSynthesizer is cancelled.")
        } catch (e: Throwable) {
            L.e(e)
        } finally {
            mediaPlayer?.reset()
        }
    }
}
