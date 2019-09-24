package com.justai.aimybox.voicetrigger

interface VoiceTrigger {
    /**
     * Start trigger phrase detection.
     *
     * @param onTriggered callback which should be called when trigger phrase detected.
     * @param onException callback which should be called when any exception is happened.
     * */
    suspend fun startDetection(onTriggered: (phrase: String?) -> Unit, onException: (e: Throwable) -> Unit)

    /**
     * Stop trigger phrase detection.
     * */
    suspend fun stopDetection()

    /**
     * Free all claimed resources and prepare the object to destroy.
     * This is only required if you consider to change the component in runtime.
     * */
    fun destroy() = Unit

    sealed class Event {
        object Started : Event()
        object Stopped : Event()
        data class Triggered(val phrase: String?) : Event()
    }
}