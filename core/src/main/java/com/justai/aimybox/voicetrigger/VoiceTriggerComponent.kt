package com.justai.aimybox.voicetrigger

import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.VoiceTriggerException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
internal class VoiceTriggerComponent(
    private val events: SendChannel<VoiceTrigger.Event>,
    private val exceptions: SendChannel<AimyboxException>,
    private val onTriggered: () -> Job
) : AimyboxComponent("VoiceTrigger") {

    private var delegate: VoiceTrigger? = null
    private var isStarted = AtomicBoolean(false)

    internal suspend fun start() {
        delegate?.let { delegate ->
            if (isStarted.compareAndSet(false, true)) {
                events.send(VoiceTrigger.Event.Started)
                delegate.startDetection(
                    onTriggered = { phrase ->
                        onTriggered()
                        events.offer(VoiceTrigger.Event.Triggered(phrase))
                    },
                    onException = { e ->
                        exceptions.offer(VoiceTriggerException(cause = e))
                    }
                )
            }
        }
    }

    internal suspend fun stop() {
        delegate?.let { delegate ->
            if (isStarted.compareAndSet(true, false)) {
                delegate.stopDetection()
                events.send(VoiceTrigger.Event.Stopped)
            }
        }
    }

    internal fun setDelegate(voiceTrigger: VoiceTrigger?) {
        if (delegate != voiceTrigger) {
            delegate?.destroy()
            delegate = voiceTrigger
            if (isStarted.get()) {
                launch { start() }
            }
        }
    }

}