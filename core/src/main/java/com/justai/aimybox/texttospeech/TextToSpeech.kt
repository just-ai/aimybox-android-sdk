package com.justai.aimybox.texttospeech

import com.justai.aimybox.Aimybox
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.TextToSpeechException
import com.justai.aimybox.media.AudioSynthesizer
import com.justai.aimybox.model.Speech
import kotlinx.coroutines.channels.SendChannel

/**
 * Base class for speech synthesizers.
 *
 * If you aren't going to use custom SSML parsing, consider use [BaseTextToSpeech].
 *
 * @see AudioSynthesizer
 * @see BaseTextToSpeech
 * */
abstract class TextToSpeech {

    internal lateinit var eventChannel: SendChannel<Event>
    internal lateinit var exceptionChannel: SendChannel<AimyboxException>

    /**
     * Start synthesis of provided [speechSequence] and suspend until it is finished.
     * */
    abstract suspend fun synthesize(speechSequence: List<Speech>)

    /**
     * Stop current synthesis.
     * */
    abstract suspend fun stop()

    /**
     * Free all claimed resources and prepare the object to destroy.
     * This is only required if you consider to change the component in runtime.
     * */
    open fun destroy() = Unit

    /**
     * Call this function to send the [event] to the [Aimybox.textToSpeechEvents] channel.
     * @see Event
     * */
    suspend fun onEvent(event: Event) {
        eventChannel.send(event)
    }

    /**
     * Call this function to send the [exception] to the [Aimybox.exceptions] channel.
     * */
    suspend fun onException(exception: TextToSpeechException) {
        exceptionChannel.send(exception)
    }

    sealed class Event {
        data class SpeechSequenceStarted(val speeches: List<Speech>) : Event()
        data class SpeechStarted(val speech: Speech) : Event()
        data class SpeechEnded(val speech: Speech) : Event()
        data class SpeechSequenceCompleted(val speeches: List<Speech>) : Event()
        data class SpeechSkipped(val speech: Speech) : Event()
    }
}


