package com.justai.aimybox.texttospeech

import android.content.Context
import androidx.annotation.CallSuper
import com.justai.aimybox.core.TextToSpeechException
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.extensions.contextJob
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.media.AudioSynthesizer
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.Speech
import com.justai.aimybox.model.TextSpeech
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext

/**
 * Improved version of [TextToSpeech] with already implemented simple SSML parsing and event delivering.
 * */
abstract class BaseTextToSpeech(context: Context) : TextToSpeech() {

    val coroutineContext: CoroutineContext = Dispatchers.IO // + CoroutineName("TTS")

    val scope = CoroutineScope(coroutineContext) + CoroutineName("BaseTextToSpeech")

    private val L = Logger("TTS")

    protected val audioSynthesizer = AudioSynthesizer(context)

    private val parser = SSMLSpeechParser()

    private var wasCancelled = false

    /**
     * Start synthesis of [speech] and suspend until it finished.
     * If coroutineScope of the function is cancelled, it should immediately stop speech synthesis.
     * */
    abstract suspend fun speak(speech: TextSpeech)

    final override suspend fun synthesize(speechSequence: List<Speech>, onlyText : Boolean) = withContext(coroutineContext) {
        wasCancelled = false
        speechSequence.asFlow()
            .extractSSML(onlyText)
            .handleErrors()
            .collect { speech ->
                if (isActive) {
                    try {
                        L.i("Synthesizing $speech")
                        onEvent(Event.SpeechStarted(speech))
                        when (speech) {
                            is TextSpeech -> speak(speech)
                            is AudioSpeech -> audioSynthesizer.play(speech)
                        }
                        if (wasCancelled) {
                            contextJob.cancel()
                            return@collect
                        } else {
                            L.i("Completed synthesis of $speech")
                            onEvent(Event.SpeechEnded(speech))
                        }

                    } catch (e: Throwable) {
                        L.e("Failed to synthesize $speech", e)
                        onException(TextToSpeechException(cause = e))
                    }
                } else {
                    L.w("Speech sequence is cancelled. Skipping $speech.")
                    onEvent(Event.SpeechSkipped(speech))
                }
            }
        L.i("Speech sequence synthesis completed")
    }

    @CallSuper
    override suspend fun stop() {
        wasCancelled = true
        scope.contextJob.cancelChildrenAndJoin()
        audioSynthesizer.cancel()
    }

    @CallSuper
    override fun destroy() {
        audioSynthesizer.release()
    }


    private fun Flow<Speech>.extractSSML(useOnlyText: Boolean = true) = map { speech ->
        when (speech) {
            is AudioSpeech -> flowOf(speech)
            is TextSpeech -> {
                if (useOnlyText){
                    flowOf(speech)
                } else {
                    parser.extractSSML(speech.text)

                }
            }
        }
    }.flattenConcat()

    private fun <T> Flow<T>.handleErrors(): Flow<T> = catch {
            e -> onException(TextToSpeechException(cause = e))
    }
}