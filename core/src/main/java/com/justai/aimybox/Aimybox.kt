package com.justai.aimybox

import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.api.DialogApiComponent
import com.justai.aimybox.core.*
import com.justai.aimybox.extensions.className
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.Speech
import com.justai.aimybox.speechtotext.SpeechToText
import com.justai.aimybox.speechtotext.SpeechToTextComponent
import com.justai.aimybox.texttospeech.TextToSpeech
import com.justai.aimybox.texttospeech.TextToSpeechComponent
import com.justai.aimybox.voicetrigger.VoiceTrigger
import com.justai.aimybox.voicetrigger.VoiceTriggerComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.channels.sendBlocking

/**
 * The main library class, provides access to all library features.
 *
 * Every part of Aimybox workflow is delegated to [AimyboxComponent]s.
 * These components and some other parameters are defined in [Config].
 *
 * @param initialConfig initial configuration. Can be changed by [updateConfiguration] method.
 *
 * @see Config
 * */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class Aimybox(initialConfig: Config) : CoroutineScope {

    private val L = Logger(className)

    override val coroutineContext =
        Dispatchers.IO + SupervisorJob() + CoroutineName("Aimybox Root Scope")

    /**
     * Read only current configuration.
     * Use [updateConfiguration] method to change configuration at runtime.
     */
    var config: Config = initialConfig
        private set

    /* Event channels */

    /**
     * Broadcast channel for receiving exceptions.
     * If an exception occurs during the [Aimybox] workflow, it will be sent through this channel.
     *
     * @see AimyboxException
     * */
    val exceptions = Channel<AimyboxException>().broadcast()

    /**
     * Broadcast channel for receiving speech recognition events.
     *
     * @see SpeechToText.Event
     * */
    val speechToTextEvents = Channel<SpeechToText.Event>().broadcast()

    /**
     * Broadcast channel for receiving speech synthesis events.
     *
     * @see TextToSpeech.Event
     * */
    val textToSpeechEvents = Channel<TextToSpeech.Event>().broadcast()

    /**
     * Broadcast channel for receiving voice trigger events.
     *
     * @see VoiceTrigger.Event
     * */
    val voiceTriggerEvents = Channel<VoiceTrigger.Event>().broadcast()

    /**
     * Broadcast channel for receiving dialog API communication events.
     *
     * @see DialogApi.Event
     * */
    val dialogApiEvents = Channel<DialogApi.Event>().broadcast()

    /* Components */

    private val speechToText =
        SpeechToTextComponent(config.speechToText, speechToTextEvents, exceptions)
    private val textToSpeech =
        TextToSpeechComponent(config.textToSpeech, textToSpeechEvents, exceptions)
    private val dialogApi =
        DialogApiComponent(config.dialogApi, dialogApiEvents, exceptions)
    private val responseHandler =
        AimyboxResponseHandler(this, config.skills)

    @SuppressLint("MissingPermission")
    private val voiceTrigger =
        VoiceTriggerComponent(voiceTriggerEvents, exceptions, onTriggered = ::toggleRecognition)

    private val components = listOf(speechToText, textToSpeech, dialogApi, responseHandler)

    /* State */

    /**
     * Broadcast channel for receiving [Aimybox] state changes.
     *
     * @see State
     * */
    val stateChannel = ConflatedBroadcastChannel(State.STANDBY)

    /**
     * Current state of Aimybox.
     * */
    var state: State
        get() = stateChannel.value
        private set(value) = stateChannel.sendBlocking(value)

    /**
     * When muted, Aimybox can send requests and receive responses,
     * but synthesis and recognition is disabled.
     * */
    var isMuted = false
        private set

    init {
        launch { updateConfiguration(initialConfig) }
    }

    /* Common */

    /**
     * Loads new [Aimybox] configuration. If one of components changes, the old one will be destroyed.
     * */
    suspend fun updateConfiguration(config: Config) {
        speechToText.setDelegate(config.speechToText)
        textToSpeech.setDelegate(config.textToSpeech)
        voiceTrigger.setDelegate(config.voiceTrigger)
        dialogApi.setDelegate(config.dialogApi)
        responseHandler.setSkills(config.skills)
        this@Aimybox.config = config
        standby().join()
    }

    /**
     * Cancels any active component.
     * */
    suspend fun cancelCurrentTask() {
        components.forEach { it.cancel() }
    }

    /**
     * Stop recognition, synthesis, API call and launch voice trigger if present.
     * */
    fun standby() = launch {
        state = State.STANDBY

        cancelCurrentTask()
        voiceTrigger.start()
    }

    fun mute() = launch {
        state = State.STANDBY

        voiceTrigger.stop()
        speechToText.cancel()
        textToSpeech.cancel()
        isMuted = true
    }

    fun unmute() = launch {
        voiceTrigger.start()
        isMuted = false
    }

    /* TTS */

    /**
     * Start synthesis of the provided [speech].
     * The method invocation interrupts any previous synthesis.
     *
     * By default, after synthesis Aimybox will go to [State.STANDBY] state, but you can change the behavior using
     * [nextAction] parameter.
     *
     * @param nextAction defines which action runs after synthesis completion
     *
     * @return [Job] which completes once the synthesis is done, or null if [isMuted].
     *
     * @see Speech
     * @see NextAction
     * */
    @RequiresPermission("android.permission.RECORD_AUDIO")
    fun speak(speech: Speech, nextAction: NextAction = NextAction.STANDBY): Job? =
        speak(listOf(speech), nextAction)

    /**
     * Start synthesis of the provided [speeches].
     * The method invocation interrupts any previous synthesis.
     *
     * By default, after synthesis Aimybox will go to [State.STANDBY] state, but you can change the behavior using
     * [nextAction] parameter.
     *
     * @param nextAction defines which action runs after synthesis completion
     *
     * @return [Job] which completes when the synthesis is done, or null if [isMuted].
     *
     * @see Speech
     * @see NextAction
     * */
    @RequiresPermission("android.permission.RECORD_AUDIO")
    fun speak(speeches: List<Speech>, nextAction: NextAction = NextAction.STANDBY): Job? =
        if (!isMuted) launch {
            if (state == State.SPEAKING) return@launch
            state = State.SPEAKING

            stopSpeaking().join()
            voiceTrigger.stop()

            textToSpeech.speak(speeches)
        }.apply {
            invokeOnCompletion { cause ->
                if (cause is CancellationException) {
                    if (state == State.SPEAKING) standby()
                } else {
                    when (nextAction) {
                        NextAction.NOTHING -> Unit
                        NextAction.RECOGNITION -> startRecognition()
                        NextAction.STANDBY -> standby()
                    }
                }
            }
        } else null

    fun stopSpeaking() = launch { textToSpeech.cancel() }

    /* STT */

    /**
     * Start speech recognition.
     *
     * Once some speech has been recognized, then a dialog API request is launched.
     * in case nothing is recognized, [SpeechToText.Event.EmptyRecognitionResult] will be sent
     * to [speechToTextEvents], and Aimybox will go to [State.STANDBY] state.
     *
     * @return [Job] which completes when recognition is finished, or null if [isMuted].
     *
     * */
    @RequiresPermission("android.permission.RECORD_AUDIO")
    fun startRecognition(): Job? = if (!isMuted) launch {
        if (state == State.LISTENING) return@launch
        state = State.LISTENING

        stopSpeaking().join()
        stopReponseProcessing().join()

        voiceTrigger.stop()

        val speech = speechToText.recognizeSpeech()
        if (!speech.isNullOrBlank()) {
            send(Request(speech))
        } else {
            onEmptyRecognition()
        }
    }.apply {
        invokeOnCompletion { cause ->
            if (cause is CancellationException) onRecognitionCancelled()
        }
    } else null

    private fun onEmptyRecognition() {
        if (state == State.LISTENING) standby()
    }

    private fun onRecognitionCancelled() {
        L.w("Recognition cancelled")
        if (state == State.LISTENING) standby()
    }

    /**
     * Stops the current recognition, but not cancels it completely. If something was recognized,
     * then the request to a dialog API will be executed asynchronously after calling this method.
     * */
    fun stopRecognition(): Job = launch { speechToText.stopRecognition() }

    /**
     * Cancels the current recognition and discard partial recognition results.
     * */
    fun cancelRecognition(): Job = launch { speechToText.cancel() }

    /**
     * Toggle speech recognition.
     * This method is designed to use with software or hardware recognition button,
     * it plays earcon sound when recognition starts.
     *
     * @see [Config.Builder.setEarconRes]
     * */
    @RequiresPermission("android.permission.RECORD_AUDIO")
    fun toggleRecognition(): Job = launch {
        if (state == State.LISTENING) {
            cancelRecognition().join()
        } else {
            config.earcon?.start()
            startRecognition()
        }
    }

    /* API */

    /**
     * Send the [request] to a dialog API.
     *
     * @return [Job] which completes when the response is received.
     * */
    @RequiresPermission("android.permission.RECORD_AUDIO")
    fun send(request: Request) = launch {
        state = State.PROCESSING

        cancelRecognition().join()
        stopSpeaking().join()

        if (config.recognitionBehavior == Config.RecognitionBehavior.ALLOW_OVERRIDE) {
            voiceTrigger.start()
        }

        config.skills.forEach { it.onRequest(request) }

        val response = dialogApi.send(request)

        if (response != null) {
            state = State.PROCESSING
            cancelCurrentTask()
            process(response)
        } else {
            onEmptyResponse(request)
        }
    }

    @RequiresPermission("android.permission.RECORD_AUDIO")
    private fun process(response: Response) = responseHandler.handle(response)

    fun stopReponseProcessing() = launch { responseHandler.cancel() }

    private fun onEmptyResponse(request: Request) {
        if (state == State.PROCESSING) {
            L.w("Response is empty for $request")
            standby()
        }
    }

    /**
     * Determines every possible state of Aimybox.
     * */
    enum class State {
        /**
         * Aimybox is waiting for interaction. If voice trigger is defined, it is active in this state.
         * */
        STANDBY,
        /**
         * Aimybox is recognizing speech.
         * */
        LISTENING,
        /**
         * Aimybox is waiting for a dialog API to process the request.
         * */
        PROCESSING,
        /**
         * Aimybox is synthesizing speech.
         * */
        SPEAKING
    }

    /**
     * Defines what will happens once speech synthesis is completed.
     *
     * @see [Aimybox.speak]
     * */
    enum class NextAction {
        /**
         * Go to standby state.
         * */
        STANDBY,
        /**
         * Start speech recognition.
         * */
        RECOGNITION,
        /**
         * Do nothing after synthesis.
         *
         * **Caution: this constant is intended primarily for usage in a [CustomSkill].
         * It will not start the voice trigger after the synthesis, so your app may enter to a non-interactive state.**
         * */
        NOTHING;

        companion object {
            /**
             * If the speech is a question, it is obviously to start speech recognition after synthesis.
             * On the other hand, if the speech does not imply an answer, it is logical to go to standby state.
             * */
            fun byQuestion(isQuestion: Boolean?) = if (isQuestion == true) RECOGNITION else STANDBY
        }
    }
}
