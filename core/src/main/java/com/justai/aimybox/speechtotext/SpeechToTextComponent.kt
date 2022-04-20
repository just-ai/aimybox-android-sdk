package com.justai.aimybox.speechtotext

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.RecognitionTimeoutException
import com.justai.aimybox.core.SpeechToTextException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

internal class SpeechToTextComponent(
    private var delegate: SpeechToText,
    private val eventBus: EventBus<SpeechToText.Event>,
    private val exceptionBus: EventBus<AimyboxException>
) : AimyboxComponent("STT") {

    init {
        provideChannelsForDelegate()
    }

    private var recognitionResult: Job? = null
    private var timeoutTask: Job? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    internal suspend fun recognizeSpeech(): String? {
        //    logger.assert(!hasRunningJobs) { "Recognition is already running" }
        cancelRunningJob()
        return withContext(componentContext) {

            logger.i("Begin recognition")
            val recognitionFlow = delegate.startRecognition()
            eventBus.invokeEvent(SpeechToText.Event.RecognitionStarted)

            timeoutTask = launch {
                startTimeout(delegate.recognitionTimeoutMs)
            }

            var textResult: String? = null

            recognitionResult?.cancel()
            recognitionResult = launch {

                recognitionFlow
                    .catch { e ->
                        exceptionBus.invokeEvent(SpeechToTextException(cause = e))
                        currentCoroutineContext().cancel()
                    }
                    .onEach { result ->
                        if (timeoutTask?.isActive == true){
                            timeoutTask?.cancel()
                        }
                        when (result) {
                            is SpeechToText.Result.Partial -> {
                                if (textResult != result.text) {
                                    textResult = result.text
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
                                logger.d("Recognition result: ${result.text}")
                                textResult = result.text
                                cancel()
                            }
                            is SpeechToText.Result.Exception -> {
                                logger.e("Failed to get recognition result", result.exception)
                                exceptionBus.invokeEvent(result.exception)
                                cancel()
                            }
                        }
                    }
                    .collect()
            }

            try {
                recognitionResult?.join()
            } catch (e: CancellationException) {
                e.printStackTrace()
                eventBus.invokeEvent(
                    if (textResult.isNullOrBlank()) {
                        SpeechToText.Event.EmptyRecognitionResult
                    } else {
                        SpeechToText.Event.RecognitionResult(textResult)
                    }
                )
            }
            textResult
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
        if (hasRunningChildrenJobs) {
            timeoutTask?.cancel()
            recognitionResult?.cancel()
            delegate.cancelRecognition()
            eventBus.invokeEvent(SpeechToText.Event.RecognitionCancelled)
        }
       // super.cancelRunningJob()

    }


    fun interruptRecognition() = recognitionResult?.cancel()

    private suspend fun startTimeout(timeout: Long) {
        delay(timeout)
        exceptionBus.invokeEvent(RecognitionTimeoutException(timeout))
        cancelRunningJob()
    }

    private fun provideChannelsForDelegate() {
        delegate.eventBus = eventBus
        delegate.exceptionBus = exceptionBus
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