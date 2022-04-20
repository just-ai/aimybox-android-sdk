package com.justai.aimybox.api.aimybox

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

//To migrate BroadcastChannel usage to SharedFlow, start by replacing usages of the BroadcastChannel(capacity)
//constructor with MutableSharedFlow(0, extraBufferCapacity=capacity)
//(broadcast channel does not replay values to new subscribers). Replace send and trySend calls with emit and tryEmit, and convert subscribers' code to flow operators.
class EventBus<Event> {

    private val _events = MutableSharedFlow<Event>(0, extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events = _events.asSharedFlow()

    suspend fun invokeEvent(event: Event) = _events.emit(event)

    fun tryInvoke(event: Event) = _events.tryEmit(event)
}