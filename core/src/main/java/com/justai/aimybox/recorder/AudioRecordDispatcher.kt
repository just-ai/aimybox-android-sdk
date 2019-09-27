package com.justai.aimybox.recorder

import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

private val dispatcher = Executors.newSingleThreadExecutor { runnable ->
    object : Thread(runnable, "AudioRecord thread") {
        init {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        }
    }
}.asCoroutineDispatcher()


/**
 * Dispatcher which is intended to record audio on Android devices.
 * */
internal val Dispatchers.AudioRecord
    get() = dispatcher