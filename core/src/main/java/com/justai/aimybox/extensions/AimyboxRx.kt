package com.justai.aimybox.extensions

import com.justai.aimybox.Aimybox
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.rx2.asObservable

fun Aimybox.exceptionsEventsObservable() = exceptions.toObservable()

fun Aimybox.dialogApiEventsObservable() = dialogApiEvents.toObservable()

fun Aimybox.speechToTextObservable() = speechToTextEvents.toObservable()

@FlowPreview
@ExperimentalCoroutinesApi
private fun <T : Any> BroadcastChannel<T>.toObservable(): Observable<T> =
    openSubscription().consumeAsFlow().asObservable()