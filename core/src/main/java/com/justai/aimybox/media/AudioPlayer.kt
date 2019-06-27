package com.justai.aimybox.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.annotation.RawRes
import com.justai.aimybox.core.L
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

private const val PLAYER_STATE_POLLING_DELAY_MS = 250L

/**
 * Asynchronous audio player implemented using Kotlin coroutines.
 * */
class AudioPlayer(context: Context) : CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    private val sources = Channel<Source>()

    /**
     * Send [Source] to the channel to start play it.
     * */
    val inputSources = sources as SendChannel<Source>
    /**
     * State changes of the [AudioPlayer] will be sent to the channel.
     * */
    val state = ConflatedBroadcastChannel(State.STOPPED)

    private var playerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    init {
        launch {
            sources.consumeEach { source ->
                mediaPlayer = source.createMediaPlayer(context)
                playerJob = mediaPlayer!!.launchPlayer()
                source.onComplete()
                state.send(State.STOPPED)
            }
        }
    }

    fun togglePlay() {
        if (mediaPlayer?.isPlaying == true) pause() else resume()
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun seek(time: Int) {
        mediaPlayer?.seekTo(time)
    }

    fun stop() {
        state.offer(State.STOPPED)
        playerJob?.cancel()
    }

    private fun MediaPlayer.getState(): State {
        val playbackState = if (isPlaying) {
            State.PlaybackState.PLAYING
        } else {
            State.PlaybackState.PAUSED
        }
        return State(playbackState, duration, currentPosition)
    }

    private fun MediaPlayer.launchPlayer() = launch {
        val observeStateJob = launch {
            while (isActive) {
                delay(PLAYER_STATE_POLLING_DELAY_MS)
                mediaPlayer?.getState()?.let { state.send(it) }
            }
        }
        try {
            playSuspendable()
        } finally {
            observeStateJob.cancel()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private suspend fun MediaPlayer.playSuspendable() {
        suspendCancellableCoroutine<Unit> { continuation ->
            setOnCompletionListener {
                continuation.resume(Unit)
            }
            setOnErrorListener { _, what, _ ->
                L.e("AudioPlayer error code $what. Stopping player.")
                continuation.resume(Unit)
                true
            }
            start()
        }
    }

    fun finalize() {
        coroutineContext.cancelChildren()
    }

    sealed class Source {

        private var completion = CompletableDeferred<Unit>()

        data class Raw(@RawRes val resId: Int) : Source()
        data class Url(val url: String) : Source()

        /**
         * Await for this track to finish.
         * */
        suspend fun await() {
            completion.await()
        }

        internal fun onComplete() = completion.complete(Unit)

        internal fun createMediaPlayer(context: Context): MediaPlayer = when (this) {
            is Raw -> MediaPlayer.create(context, resId)
            is Url -> MediaPlayer.create(context, Uri.parse(url))
        }
    }

    data class State(val playbackState: PlaybackState, val duration: Int, val position: Int) {
        companion object {
            val STOPPED = State(PlaybackState.IDLE, -1, -1)
        }

        enum class PlaybackState { IDLE, PLAYING, PAUSED }
    }
}