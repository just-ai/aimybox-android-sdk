package com.justai.aimybox.texttospeech

import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.extensions.className
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.model.Speech
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TextToSpeechComponent(
    private var delegate: TextToSpeech,
    private val eventChannel: SendChannel<TextToSpeech.Event>,
    private val exceptionChannel: SendChannel<AimyboxException>
) : AimyboxComponent("TextToSpeech") {

    init {
        provideChannelsForDelegate()
    }

    private val L = Logger(className)

    suspend fun speak(speechList: List<Speech>) {
        L.assert(!hasRunningJobs) { "Synthesis is already running" }
        cancel()
        eventChannel.send(TextToSpeech.Event.SpeechSequenceStarted(speechList))
        withContext(coroutineContext) {
            delegate.synthesize(speechList)
        }
        eventChannel.send(TextToSpeech.Event.SpeechSequenceCompleted(speechList))
    }

    override suspend fun cancel() {
        if (hasRunningJobs) {
            delegate.stop()
            L.w("Speech cancelled")
        }
        super.cancel()
    }

    private fun provideChannelsForDelegate() {
        delegate.eventChannel = eventChannel
        delegate.exceptionChannel = exceptionChannel
    }

    suspend fun setDelegate(textToSpeech: TextToSpeech) {
        if (delegate != textToSpeech) {
            cancel()
            delegate.destroy()
            delegate = textToSpeech
            provideChannelsForDelegate()
        }
    }
}

