package com.justai.aimybox.voicetrigger

import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.VoiceTriggerException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("unused")
internal class VoiceTriggerComponent(
    private val eventsBus: EventBus<VoiceTrigger.Event>,
    private val exceptionsBus: EventBus<AimyboxException>,
    private val onTriggered: () -> Job  //TODO Replacement candidate
) : AimyboxComponent("VT") {

    private var delegate: VoiceTrigger? = null
    private var isStarted = AtomicBoolean(false)


    internal suspend fun start() {
        delegate?.let { delegate ->
            if (isStarted.compareAndSet(false, true)) {
                eventsBus.invokeEvent(VoiceTrigger.Event.Started)
                delegate.startDetection(
                    onTriggered = { phrase ->
                        onTriggered()
                        eventsBus.tryInvoke(VoiceTrigger.Event.Triggered(phrase))
                    },
                    onException = { e ->
                        exceptionsBus.tryInvoke(VoiceTriggerException(cause = e))
                    }
                )
            }
        }
    }

    internal suspend fun stop() {
        delegate?.let { delegate ->
            if (isStarted.get()) {
                delegate.stopDetection()
                isStarted.set(false)
                eventsBus.invokeEvent(VoiceTrigger.Event.Stopped)
            }
        }
    }

    internal fun setDelegate(voiceTrigger: VoiceTrigger?) {
        if (delegate != voiceTrigger) {
            delegate?.destroy()
            delegate = voiceTrigger
            if (isStarted.get()) {
                scope.launch { start() }
            }
        }
    }

    override suspend fun cancelRunningJob() {
        delegate?.stopDetection()
        super.cancelRunningJob()
    }

}