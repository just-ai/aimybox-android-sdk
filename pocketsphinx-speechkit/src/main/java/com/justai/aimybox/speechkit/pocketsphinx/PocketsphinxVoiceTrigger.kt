package com.justai.aimybox.speechkit.pocketsphinx

import com.justai.aimybox.voicetrigger.VoiceTrigger
import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.RecognitionListener
import java.lang.Exception

class PocketsphinxVoiceTrigger(
    recognizerProvider: PocketsphinxRecognizerProvider,
    phrase: String
): VoiceTrigger {

    companion object {
        private const val TRIGGER_SEARCH = "trigger"
    }

    private val recognizer = recognizerProvider.recognizer

    init {
        recognizer.addKeyphraseSearch(TRIGGER_SEARCH, phrase)
    }

    private lateinit var listener: TriggerListener

    override suspend fun startDetection(
        onTriggered: (phrase: String?) -> Unit,
        onException: (e: Throwable) -> Unit
    ) {
        listener = TriggerListener(onTriggered, onException)
        recognizer.addListener(listener)
        recognizer.startListening(TRIGGER_SEARCH)
    }

    override suspend fun stopDetection() {
        recognizer.cancel()
        recognizer.removeListener(listener)
    }

    private class TriggerListener(
        private val onTriggered: (phrase: String?) -> Unit,
        private val onException: (e: Throwable) -> Unit
    ): RecognitionListener {
        override fun onResult(hyp: Hypothesis?) {}

        override fun onPartialResult(hyp: Hypothesis?) {
            hyp?.let { onTriggered(hyp.hypstr) }
        }

        override fun onTimeout() {}

        override fun onBeginningOfSpeech() {}

        override fun onEndOfSpeech() {}

        override fun onError(e: Exception?) {
            e?.let(onException)
        }

    }
}