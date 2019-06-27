package com.justai.aimybox.texttospeech

import android.content.Context
import androidx.annotation.CallSuper
import com.justai.aimybox.core.TextToSpeechException
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.media.AudioSynthesizer
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.Speech
import com.justai.aimybox.model.TextSpeech
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Improved version of [TextToSpeech] with already implemented simple SSML parsing and event delivering.
 * */
abstract class BaseTextToSpeech(context: Context) : TextToSpeech(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    private val L = Logger("Aimybox-TTS")

    private val audioSynthesizer = AudioSynthesizer(context)

    private val parser = SSMLSpeechParser()

    /**
     * Start synthesis of [speech] and suspend until it finished.
     * If coroutineScope of the function is cancelled, it should immediately stop speech synthesis.
     * */
    abstract suspend fun speak(speech: TextSpeech)

    final override suspend fun synthesize(speechSequence: List<Speech>) = withContext(coroutineContext) {
        onEvent(Event.SpeechSequenceStarted(speechSequence))
        speechSequence.asFlow()
            .map { extractSSML(it) }
            .flattenConcat()
            .collect { speech ->
                if (isActive) {
                    try {
                        L.i("Synthesizing $speech")
                        onEvent(Event.SpeechStarted(speech))
                        when (speech) {
                            is TextSpeech -> speak(speech)
                            is AudioSpeech -> audioSynthesizer.play(speech)
                        }
                        L.i("Completed synthesis of $speech")
                        onEvent(Event.SpeechEnded(speech))
                    } catch (e: Throwable) {
                        L.e("Failed to synthesize $speech", e)
                        onException(TextToSpeechException(e))
                    }
                } else {
                    L.w("Speech sequence is cancelled. Skipping $speech.")
                    onEvent(Event.SpeechSkipped(speech))
                }
            }
        L.i("Speech sequence synthesis completed")
        onEvent(Event.SpeechSequenceCompleted(speechSequence))
    }

    @CallSuper
    override fun stop() {
        coroutineContext.cancelChildren()
        audioSynthesizer.cancel()
    }

    @CallSuper
    override fun destroy() {
        audioSynthesizer.release()
    }

    private fun extractSSML(speech: Speech): Flow<Speech> {
        return when(speech) {
            is AudioSpeech -> flowOf(speech)
            is TextSpeech -> parser.extractSSML(speech.text)
        }
    }
}