package com.justai.aimybox.texttospeech

import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.model.Speech
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext

internal class TextToSpeechComponent(
    private var delegate: TextToSpeech,
    private val eventChannel: SendChannel<TextToSpeech.Event>,
    private val exceptionChannel: SendChannel<AimyboxException>
) : AimyboxComponent("TTS") {

    init {
        provideChannelsForDelegate()
    }

    private val L = Logger("Aimybox-TTS")

    suspend fun speak(speechList: List<Speech>) {
        cancel()
        eventChannel.send(TextToSpeech.Event.SpeechSequenceStarted(speechList))
        withContext(coroutineContext) {
            delegate.synthesize(speechList)
        }
        eventChannel.send(TextToSpeech.Event.SpeechSequenceCompleted(speechList))
    }

    fun setDelegate(textToSpeech: TextToSpeech) {
        if (delegate != textToSpeech) {
            cancel()
            delegate.destroy()
            delegate = textToSpeech
            provideChannelsForDelegate()
        }
    }

    private fun provideChannelsForDelegate() {
        delegate.eventChannel = eventChannel
        delegate.exceptionChannel = exceptionChannel
    }

    override fun cancel() {
        if (hasRunningJobs) {
            delegate.stop()
            super.cancel()
            L.w("Speech cancelled")
        }
    }
}

