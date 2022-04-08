package com.justai.aimybox.texttospeech

import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.model.Speech
import kotlinx.coroutines.withContext

internal class TextToSpeechComponent(
    private var delegate: TextToSpeech,
    private val eventBus: EventBus<TextToSpeech.Event>,
    private val exceptionBus: EventBus<AimyboxException>
) : AimyboxComponent("TTS") {

    init {
        provideChannelsForDelegate()
    }

    suspend fun speak(speechList: List<Speech>, onlyText: Boolean = true) {
     //   logger.assert(!hasRunningJobs) { "Synthesis is already running" }
        cancelRunningJob()
        eventBus.invokeEvent(TextToSpeech.Event.SpeechSequenceStarted(speechList))
    //    withContext(coroutineContext) {  //ToDO check this place!!!
            delegate.synthesize(speechList, onlyText)
    //    }
        eventBus.invokeEvent(TextToSpeech.Event.SpeechSequenceCompleted(speechList))
    }

    override suspend fun cancelRunningJob() {
        //if (hasRunningJobs) {
            delegate.stop()
            logger.w("Speech cancelled")
      //  }
        super.cancelRunningJob()
    }

    private fun provideChannelsForDelegate() {
        delegate.eventBus = eventBus
        delegate.exceptionBus = exceptionBus
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

