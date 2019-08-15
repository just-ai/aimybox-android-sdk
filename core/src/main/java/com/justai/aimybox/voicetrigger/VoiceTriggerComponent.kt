@file:Suppress("unused")

package com.justai.aimybox.voicetrigger

import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.VoiceTriggerException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

internal class VoiceTriggerComponent(
    private val events: SendChannel<VoiceTrigger.Event>,
    private val exceptions: SendChannel<AimyboxException>,
    private val onTriggered: () -> Unit
) : AimyboxComponent("VT") {

    private var delegate: VoiceTrigger? = null
    private var isStarted = false

    internal suspend fun start() {
        delegate?.let { delegate ->
            if (!isStarted) {
                isStarted = true
                events.send(VoiceTrigger.Event.Started)
                delegate.startDetection(
                    onTriggered = { phrase ->
                        onTriggered
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
            if (isStarted) {
                isStarted = false
                delegate.stopDetection()
                events.send(VoiceTrigger.Event.Stopped)
            }
        }
    }

    internal fun setDelegate(voiceTrigger: VoiceTrigger?) {
        if (delegate != voiceTrigger) {
            delegate?.destroy()
            delegate = voiceTrigger
            if (isStarted) {
                isStarted = false
                launch { start() }
            }
        }
    }

}