package com.justai.aimybox.speechtotext

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.RecognitionTimeoutException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach

internal class SpeechToTextComponent(
    private var delegate: SpeechToText,
    private val eventBus: EventBus<SpeechToText.Event>,
    private val exceptionBus: EventBus<AimyboxException>
) : AimyboxComponent("STT") {

    init {
        provideChannelsForDelegate()
    }

    private var recognitionResult: Deferred<String?>? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    internal suspend fun recognizeSpeech(): String? {
        logger.assert(!hasRunningJobs) { "Recognition is already running" }
        cancelRunningJob()
        return withContext(coroutineContext) {

            logger.i("Begin recognition")
            val recognitionChannel = delegate.startRecognition()
            eventBus.invokeEvent(SpeechToText.Event.RecognitionStarted)

            val timeoutTask = startTimeout(delegate.recognitionTimeoutMs)

            recognitionResult?.cancel()
            recognitionResult = async {
                var finalResult: String? = null
                tryOrNull {
                    recognitionChannel.consumeEach { result ->
                        timeoutTask.cancel()
                        when (result) {
                            is SpeechToText.Result.Partial -> {
                                if (finalResult != result.text) {
                                    finalResult = result.text
                                    delegate.clearCounter()
                                    logger.d("Partial recognition result: ${result.text}")
                                    eventBus.invokeEvent(
                                        SpeechToText.Event.RecognitionPartialResult(
                                            result.text
                                        )
                                    )
                                }
                            }
                            is SpeechToText.Result.Final -> {
                                recognitionChannel.cancel()
                                logger.d("Recognition result: ${result.text}")
                                finalResult = result.text
                            }
                            is SpeechToText.Result.Exception -> {
                                recognitionChannel.cancel()
                                logger.e("Failed to get recognition result", result.exception)
                                exceptionBus.invokeEvent(result.exception)
                                finalResult = null
                            }
                        }
                    }
                }
                finalResult
            }

            val finalResult = tryOrNull {
                recognitionResult?.await()
            }
            timeoutTask.cancel()
            eventBus.invokeEvent(
                if (finalResult.isNullOrBlank()) {
                    SpeechToText.Event.EmptyRecognitionResult
                } else {
                    SpeechToText.Event.RecognitionResult(finalResult)
                }
            )

            finalResult
        }
    }

    private suspend fun <T> tryOrNull(action: suspend () -> T): T? = try {
        action()
    } catch (t: Throwable) {
        null
    }

    suspend fun stopRecognition() {
        delegate.stopRecognition()
    }

    override suspend fun cancelRunningJob() {
        if (hasRunningJobs) {
            recognitionResult?.cancel()
            delegate.cancelRecognition()
            eventBus.invokeEvent(SpeechToText.Event.RecognitionCancelled)
        }
        super.cancelRunningJob()
    }

    fun interruptRecognition() = recognitionResult?.cancel()

    private fun startTimeout(timeout: Long) = scope.launch {
        delay(timeout)
        exceptionBus.invokeEvent(RecognitionTimeoutException(timeout))
        cancelRunningJob()
    }

    private fun provideChannelsForDelegate() {
        delegate.eventChannel = eventBus
        delegate.exceptionChannel = exceptionBus
    }

    internal suspend fun setDelegate(speechToText: SpeechToText) {
        if (delegate != speechToText) {
            cancelRunningJob()
            delegate.destroy()
            delegate = speechToText
            provideChannelsForDelegate()
        }
    }
}