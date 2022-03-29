package com.justai.aimybox.texttospeech

import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
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

    suspend fun speak(speechList: List<Speech>, onlyText: Boolean = true) {
        logger.assert(!hasRunningJobs) { "Synthesis is already running" }
        cancelRunningJob()
        eventChannel.send(TextToSpeech.Event.SpeechSequenceStarted(speechList))
        withContext(coroutineContext) {
            delegate.synthesize(speechList, onlyText)
        }
        eventChannel.send(TextToSpeech.Event.SpeechSequenceCompleted(speechList))
    }

    override suspend fun cancelRunningJob() {
        if (hasRunningJobs) {
            delegate.stop()
            logger.w("Speech cancelled")
        }
        super.cancelRunningJob()
    }

    private fun provideChannelsForDelegate() {
        delegate.eventChannel = eventChannel
        delegate.exceptionChannel = exceptionChannel
    }

    suspend fun setDelegate(textToSpeech: TextToSpeech) {
        if (delegate != textToSpeech) {
            cancelRunningJob()
            delegate.destroy()
            delegate = textToSpeech
            provideChannelsForDelegate()
        }
    }
}

