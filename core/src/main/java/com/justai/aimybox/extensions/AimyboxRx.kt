package com.justai.aimybox.extensions

import com.justai.aimybox.Aimybox
import kotlinx.coroutines.rx2.asObservable

fun Aimybox.stateObservable() = stateChannel.asObservable()

fun Aimybox.exceptionsObservable() = exceptions.events.asObservable()

fun Aimybox.dialogApiEventsObservable() = dialogApiEvents.events.asObservable()

fun Aimybox.speechToTextEventsObservable() = speechToTextEvents.events.asObservable()

fun Aimybox.textToSpeechEventsObservable() = textToSpeechEvents.events.asObservable()

fun Aimybox.voiceTriggerEventsObservable() = voiceTriggerEvents.events.asObservable()

