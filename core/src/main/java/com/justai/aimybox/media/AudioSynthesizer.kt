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


/**
 * Designed for usage inside [TextToSpeech] to play SSML audio and [AudioSpeech].
 *
 * @see TextToSpeech
 * */
class AudioSynthesizer(private val context: Context) : CoroutineScope {

    private val L = Logger(className)

    private var mediaPlayer = MediaPlayer()

    override val coroutineContext: CoroutineContext = Dispatchers.Default + Job()

    suspend fun play(source: AudioSpeech) {
        L.assert(contextJob.isActive) {
            "Can't play $source: AudioSynthesizer is released."
        }
        L.assert(!contextJob.children.any { it.isActive }) {
            "Can't play $source: AudioSynthesizer is busy."
        }
        launchPlayer(source).join()
    }

    suspend fun cancel() {
        mediaPlayer.stop()
        mediaPlayer.reset()
        contextJob.cancelChildrenAndJoin()
    }

    fun release() {
        mediaPlayer.release()
        contextJob.cancel()
    }

    private fun launchPlayer(source: AudioSpeech) = launch {
        val scope = this
        try {
            withContext(Dispatchers.IO) {
                source.load(context, mediaPlayer)
            }
            mediaPlayer.apply {
                setOnCompletionListener {
                    it.reset()
                }
                setOnPreparedListener {
                    it.start()
                }
                prepareAsync()
                setOnErrorListener { _, what, _ ->
                    L.e("MediaPlayer error code $what. Stopping AudioSynthesizer.")
                    scope.cancel()
                    true
                }
            }

        } catch (e: CancellationException) {
            L.w("AudioSynthesizer is cancelled.")
            mediaPlayer.reset()
        } catch (e: Throwable) {
            L.e(e)
        }
    }
}
