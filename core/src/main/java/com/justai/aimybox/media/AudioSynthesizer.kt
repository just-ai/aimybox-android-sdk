package com.justai.aimybox.media

import android.content.Context
import android.media.MediaPlayer
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.texttospeech.TextToSpeech
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * Designed for usage inside [TextToSpeech] to play SSML audio and [AudioSpeech].
 *
 * @see TextToSpeech
 * */
class AudioSynthesizer(private val context: Context) : CoroutineScope {

    private val L = Logger("Aimybox-AudioSynthesizer")

    private val job = Job()

    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    private var mediaPlayer = MediaPlayer()

    suspend fun play(source: AudioSpeech) {
        when {
            job.isCancelled -> L.a("Can't play $source: AudioSynthesizer is released.")
            job.children.any() -> L.e("Can't play $source: AudioSynthesizer is busy.")
            else -> launchPlayer(source).join()
        }
    }

    fun cancel() {
        job.cancelChildren()
    }

    fun release() {
        mediaPlayer.release()
        job.cancel()
    }

    private fun launchPlayer(source: AudioSpeech) = launch {
        try {
            source.load(context, mediaPlayer)
            mediaPlayer.prepare()
            suspendCancellableCoroutine { continuation ->
                mediaPlayer.setOnCompletionListener { continuation.resume(Unit) }
                mediaPlayer.setOnErrorListener { _, what, _ ->
                    L.e("MediaPlayer error code $what. Stopping AudioSynthesizer.")
                    cancel()
                    true
                }
                mediaPlayer.start()
            }
        } catch (e: CancellationException) {
            L.e("AudioSynthesizer is cancelled.")
        } catch (e: Throwable) {
            mediaPlayer.release()
            mediaPlayer = MediaPlayer()
            L.e(e)
        } finally {
            mediaPlayer.reset()
        }
    }
}