package com.justai.aimybox.api.aimybox

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EventBus<Event> {

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    suspend fun invokeEvent(event: Event) = _events.emit(event)

    fun tryInvoke(event: Event) = _events.tryEmit(event)
}