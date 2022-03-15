package com.justai.aimybox

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.getSystemService
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.*
import com.justai.aimybox.core.Config.*
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.model.Speech
import com.justai.aimybox.speechtotext.SpeechToText
import com.justai.aimybox.speechtotext.SpeechToTextComponent
import com.justai.aimybox.texttospeech.TextToSpeech
import com.justai.aimybox.texttospeech.TextToSpeechComponent
import com.justai.aimybox.voicetrigger.VoiceTrigger
import com.justai.aimybox.voicetrigger.VoiceTriggerComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

/**
 * The main library class, provides access to all library features.
 *
 * Every part of Aimybox workflow is delegated to [AimyboxComponent]s.
 * These components and some other parameters are defined in [Config].
 *
 * @param initialConfig initial configuration. Can be changed by [updateConfiguration] method.
 * @param applicationContext need to pause/resume playing audio stream while assistant is running.
 * If this parameter is `null`, audio would not be paused.
 *
 * @see Config
 * */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class Aimybox(
    initialConfig: Config,
    applicationContext: Context?
) : CoroutineScope {

    @Deprecated(
        "Audio focus cannot be requested. Use constructor with application context instead.",
        ReplaceWith("Aimybox(initialConfig, context)")
    )
    constructor(initialConfig: Config) : this(initialConfig, null)

    private val L = Logger()

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
    private val dialogApi get() = config.dialogApi

    @SuppressLint("MissingPermission")
    private val voiceTrigger =
        VoiceTriggerComponent(voiceTriggerEvents, exceptions, onTriggered = ::toggleRecognition)

    private val components = listOf(speechToText, textToSpeech, dialogApi)


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
        @Synchronized
        get() = stateChannel.value
        @Synchronized
        private set(value) {
            when (value) {
                State.STANDBY -> abandonRequestAudioFocus()
                State.LISTENING, State.SPEAKING -> requestAudioFocus()
                State.PROCESSING -> Unit
            }
            stateChannel.trySendBlocking(value)
        }

    /**
     * When muted, Aimybox can send requests and receive responses,
     * but synthesis and recognition is disabled.
     * */
    var isMuted = false
        private set


    var isVoiceTriggerActivated: Boolean = true
        set(value) {
            field = value
            launch {
                if (field) {
                    voiceTrigger.start()
                } else {
                    voiceTrigger.stop()
                }
            }
        }

    init {
        launch { updateConfiguration(initialConfig) }

        // Fix for Kaldi Voice Trigger behaviour.

        exceptions.observe {
            val mustVoiceTriggerBeStarted = state == State.STANDBY ||
                    (config.recognitionBehavior == RecognitionBehavior.ALLOW_OVERRIDE
                            && state != State.LISTENING
                            )
            if (it is VoiceTriggerException && mustVoiceTriggerBeStarted) {
                voiceTrigger.stop()
                delay(200)
                if (isVoiceTriggerActivated) {
                    voiceTrigger.start()
                }
            }
        }
    }

    /* Common */

    /**
     * Loads new [Aimybox] configuration. If one of components changes, the old one will be destroyed.
     * */
    suspend fun updateConfiguration(config: Config) {
        speechToText.setDelegate(config.speechToText)
        textToSpeech.setDelegate(config.textToSpeech)
        voiceTrigger.setDelegate(config.voiceTrigger)
        this.config = config
        standby().join()
    }

    /**
     * Cancels any active component.
     * */
    suspend fun cancelCurrentTask() {
        components.forEach { it.cancelRunningJob() }
    }

    /**
     * Stop recognition, synthesis, API call and launch voice trigger if present.
     * */
    fun standby() = launch {
        state = State.STANDBY

        cancelCurrentTask()
        if (isVoiceTriggerActivated) {
            voiceTrigger.start()
        }
    }

    fun mute() = launch {
        state = State.STANDBY

        voiceTrigger.stop()
        speechToText.cancelRunningJob()
        textToSpeech.cancelRunningJob()
        isMuted = true
    }

    fun unmute() = launch {
        if (isVoiceTriggerActivated) {
            voiceTrigger.start()
        }
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
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun speak(
        speech: Speech,
        nextAction: NextAction = NextAction.STANDBY,
        onlyText: Boolean = true
    ): Job? =
        speak(listOf(speech), nextAction, onlyText)

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
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun speak(
        speeches: List<Speech>,
        nextAction: NextAction = NextAction.STANDBY,
        onlyText: Boolean = true
    ): Job? =
        if (!isMuted) launch {
            if (state == State.SPEAKING) return@launch
            state = State.SPEAKING

            stopSpeaking().join()

            if (config.recognitionBehavior == RecognitionBehavior.SYNCHRONOUS) {
                voiceTrigger.stop()
            }

            textToSpeech.speak(speeches, onlyText)

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

    fun stopSpeaking() = launch {
        textToSpeech.cancelRunningJob()
    }


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

    private val mAudioManager =
        applicationContext?.let { getSystemService(it, AudioManager::class.java) }

    private val audioFocusChangeListener =
        OnAudioFocusChangeListener {
            when (it) {
                AUDIOFOCUS_GAIN -> {
                    L.d("AudioFocus Gain")
                    hasAudioFocus = true
                }
                AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT, AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    L.d("AudioFocus Loss")
                    hasAudioFocus = false
                }
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildAudioFocusRequest() =
        AudioFocusRequest.Builder(AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()

    private var hasAudioFocus: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = buildAudioFocusRequest()
        }
    }


    private fun requestAudioFocus() {
        if (hasAudioFocus) {
            return
        }
        val requestResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = buildAudioFocusRequest()
            }
            audioFocusRequest?.let {
                mAudioManager?.requestAudioFocus(it)
            } ?: AUDIOFOCUS_REQUEST_FAILED
        } else {
            mAudioManager?.requestAudioFocus(
                audioFocusChangeListener,
                STREAM_MUSIC,
                AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }

        if (requestResult == AUDIOFOCUS_REQUEST_GRANTED) {
            hasAudioFocus = true
        }

    }

    private fun abandonRequestAudioFocus() {
        if (!hasAudioFocus) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { focusRequest ->
                mAudioManager?.abandonAudioFocusRequest(focusRequest)
            }
        } else {
            mAudioManager?.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecognition(): Job? = if (!isMuted) launch {
        if (state == State.LISTENING) return@launch
        state = State.LISTENING

        stopSpeaking().join()
        cancelPendingRequest().join()

        voiceTrigger.stop()

        val speech = speechToText.recognizeSpeech()

        if (!speech.isNullOrBlank()) {
            sendRequest(speech)
        } else {
            onEmptyRecognition()
        }
    }.apply {
        invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                onRecognitionCancelled()
            }
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

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun stopRecognitionAndChangeState(): Job = launch {
        speechToText.stopRecognition()
        state = State.STANDBY
    }


    /**
     * Cancels the current recognition and discard partial recognition results.
     * */
    fun cancelRecognition(): Job = launch { speechToText.cancelRunningJob() }

    /**
     * Toggle speech recognition.
     * This method is designed to use with software or hardware recognition button,
     * it plays earcon sound when recognition starts.
     *
     * @see [Config.Builder.setEarconRes]
     * */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun toggleRecognition(): Job = launch {
        val state = state
        when {
            state != State.LISTENING -> {
                config.earcon?.start()
                startRecognition()
            }
            config.stopRecognitionBehavior == StopRecognitionBehavior.PROCESS_REQUEST -> {
                interruptRecognition()
            }
            else -> {
                cancelRecognition().join()
            }
        }
    }

    fun interruptRecognition(): Job = launch {
        speechToText.interruptRecognition()
    }


    /* API */

    /**
     * Send the [request] to a dialog API.
     *
     * @return [Job] which completes when the response is received.
     * */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun sendRequest(query: String) = launch {
        state = State.PROCESSING
        cancelRecognition().join()
        stopSpeaking().join()

        if (config.recognitionBehavior == RecognitionBehavior.ALLOW_OVERRIDE) {
            if (isVoiceTriggerActivated) {
                voiceTrigger.start()
            }
        }

        val response = dialogApi.send(query, this@Aimybox)
        if (response == null) {
            onEmptyResponse()
        }
    }

    /**
     * Send the silent [request] to a dialog API.
     * Response to this request will not be processed by TTS.
     *
     * @return [Job] which completes when the response is received.
     * */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun sendSilentRequest(query: String) = launch {
        state = State.PROCESSING

        cancelRecognition().join()
        stopSpeaking().join()

        if (config.recognitionBehavior == RecognitionBehavior.ALLOW_OVERRIDE) {
            if (isVoiceTriggerActivated) {
                voiceTrigger.start()
            }
        }

        dialogApi.send(query, this@Aimybox, isSilentRequest = true)
    }

    fun cancelPendingRequest() = launch { dialogApi.cancelRunningJob() }

    private fun onEmptyResponse() {
        if (state == State.PROCESSING) standby()
    }

    private fun <T> BroadcastChannel<T>.observe(action: suspend (T) -> Unit) {
        val channel = openSubscription()
        launch {
            channel.consumeEach { action(it) }
        }.invokeOnCompletion { channel.cancel() }
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
