package com.justai.aimybox.extensions

import com.justai.aimybox.Aimybox
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.rx2.asObservable

fun Aimybox.exceptionsObservable() = exceptions.toObservable()

fun Aimybox.dialogApiEventsObservable() = dialogApiEvents.toObservable()

fun Aimybox.speechToTextEventsObservable() = speechToTextEvents.toObservable()

fun Aimybox.textToSpeechEventsObservable() = textToSpeechEvents.toObservable()

fun Aimybox.voiceTriggerEventsObservable() = voiceTriggerEvents.toObservable()

@FlowPreview
@ExperimentalCoroutinesApi
private fun <T : Any> BroadcastChannel<T>.toObservable() = asFlow().asObservable()