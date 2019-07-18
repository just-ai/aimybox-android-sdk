package com.justai.aimybox.extensions

import android.media.MediaPlayer
import com.justai.aimybox.core.L
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun MediaPlayer.playSuspendable() {
    suspendCancellableCoroutine<Unit> { continuation ->
        setOnCompletionListener {
            continuation.resume(Unit)
        }
        setOnErrorListener { _, what, _ ->
            L.e("MediaPlayer error code $what. Stopping player.")
            continuation.resume(Unit)
            true
        }
        start()
    }
}