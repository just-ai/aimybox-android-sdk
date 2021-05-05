@file:Suppress("unused")

package com.justai.aimybox.core

import android.content.Context
import android.media.MediaPlayer
import androidx.annotation.RawRes
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.Config.Companion.create
import com.justai.aimybox.speechtotext.SpeechToText
import com.justai.aimybox.texttospeech.TextToSpeech
import com.justai.aimybox.voicetrigger.VoiceTrigger

/**
 * Configuration object for [Aimybox] class.
 *
 * To create [Config] object, use [create] factory method.
 *
 * Created objects are immutable, to create updated version of existing object, use [update].
 *
 * @see Aimybox
 * @see Aimybox.updateConfiguration
 * */
data class Config internal constructor(
    val speechToText: SpeechToText,
    val textToSpeech: TextToSpeech,
    val dialogApi: DialogApi<*, *>,
    val voiceTrigger: VoiceTrigger?,
    val earcon: MediaPlayer?,
    val recognitionBehavior: RecognitionBehavior,
    val stopRecognitionBehavior: StopRecognitionBehavior
) {

    companion object {
        /**
         * Create new [Config] object.
         *
         * @param block intended to configure optional parameters,
         * such as [VoiceTrigger], [CustomSkill]s, [RecognitionBehavior], [StopRecognitionBehavior]
         *
         * @see Config.update
         * @see SpeechToText
         * @see TextToSpeech
         * @see DialogApi
         * */
        fun create(
            speechToText: SpeechToText,
            textToSpeech: TextToSpeech,
            dialogApi: DialogApi<*, *>,
            block: Builder.() -> Unit = {}
        ) = Builder(speechToText, textToSpeech, dialogApi).apply(block).build()
    }

    class Builder internal constructor(
        var speechToText: SpeechToText,
        var textToSpeech: TextToSpeech,
        var dialogApi: DialogApi<*, *>,
        var voiceTrigger: VoiceTrigger? = null,
        /**
         * @see RecognitionBehavior
         * */
        var recognitionBehavior: RecognitionBehavior = RecognitionBehavior.SYNCHRONOUS,
        /**
         * @see StopRecognitionBehavior
         */
        var stopRecognitionBehavior: StopRecognitionBehavior = StopRecognitionBehavior.CANCEL_REQUEST
    ) {

        private var earcon: MediaPlayer? = null

        internal constructor(prototype: Config) : this(
            prototype.speechToText,
            prototype.textToSpeech,
            prototype.dialogApi,
            prototype.voiceTrigger,
            prototype.recognitionBehavior
        ) {
            earcon = prototype.earcon
        }

        internal fun build() = Config(
            speechToText,
            textToSpeech,
            dialogApi,
            voiceTrigger,
            earcon,
            recognitionBehavior,
            stopRecognitionBehavior
        )

        fun setEarconRes(context: Context, @RawRes earconRes: Int? = null) {
            earcon?.release()
            earcon = if (earconRes != null) MediaPlayer.create(context, earconRes) else null
        }
    }

    /**
     * Create an updated version of existing [Config] object.
     *
     * @param block override current configuration parameters inside the block.
     *
     * @see Config.create
     * @see Aimybox.updateConfiguration
     * @see SpeechToText
     * @see TextToSpeech
     * @see DialogApi
     * */
    fun update(block: Builder.() -> Unit) = Builder(this).apply(block).build()

    /**
     * Determines the system behavior in the case when a user attempts to start speech recognition
     * during the speaking or processing of current request.
     * */
    enum class RecognitionBehavior {
        /**
         * Voice trigger is active only if Aimybox is in state [Aimybox.State.STANDBY]
         * */
        SYNCHRONOUS,

        /**
         * Voice trigger is active if Aimybox is in any state excluding [Aimybox.State.SPEAKING].
         * If the new recognition is done before the old response from dialog api is received, the response will be discarded and a new request begins.
         * */
        ALLOW_OVERRIDE
    }


    /**
     * Determines the system behavior in the case when a user attempts to stop speech recognition.
     * */
    enum class StopRecognitionBehavior {
        /**
         * Cancel recognition entirely and abandon all results.
         */
        CANCEL_REQUEST,

        /**
         * Stops recognition and send recognized text.
         */
        PROCESS_REQUEST
    }
}
