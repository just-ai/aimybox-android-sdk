package com.justai.aimybox.speechtotext

import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.RecognitionTimeoutException
import com.justai.aimybox.extensions.className
import com.justai.aimybox.logging.Logger
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SpeechToTextComponent(
    private var delegate: SpeechToText,
    private val eventChannel: SendChannel<SpeechToText.Event>,
    private val exceptionChannel: SendChannel<AimyboxException>
) : AimyboxComponent("SpeechToText") {

    init {
        provideChannelsForDelegate()
    }

    private val L = Logger(className)

    @SuppressLint("MissingPermission")
    @RequiresPermission("android.permission.RECORD_AUDIO")
    internal suspend fun recognizeSpeech(): String? {
        L.assert(!hasRunningJobs) { "Recognition is already running" }
        cancel()
        return withContext(coroutineContext) {

            L.i("Begin recognition")
            val recognitionChannel = delegate.startRecognition()
            eventChannel.send(SpeechToText.Event.RecognitionStarted)

            val timeoutTask = startTimeout(delegate.recognitionTimeoutMs)

            var finalResult: String? = null
            launch {
                recognitionChannel.consumeEach { result ->
                    timeoutTask.cancel()
                    when (result) {
                        is SpeechToText.Result.Partial -> {
                            if (finalResult != result.text) {
                                finalResult = result.text
                                L.i("Partial recognition result: ${result.text}")
                                eventChannel.send(SpeechToText.Event.RecognitionPartialResult(result.text))
                            }
                        }
                        is SpeechToText.Result.Final -> {
                            recognitionChannel.cancel()
                            L.i("Recognition result: ${result.text}")
                            finalResult = result.text
                        }
                        is SpeechToText.Result.Exception -> {
                            recognitionChannel.cancel()
                            L.e("Failed to get recognition result", result.exception)
                            exceptionChannel.send(result.exception)
                            finalResult = null
                        }
                    }
                }
            }.join()

            timeoutTask.cancel()

            eventChannel.send(
                if (finalResult.isNullOrBlank()) {
                    SpeechToText.Event.EmptyRecognitionResult
                } else {
                    SpeechToText.Event.RecognitionResult(finalResult)
                }
            )

            finalResult
        }
    }

    suspend fun stopRecognition() {
        delegate.stopRecognition()
    }

    override suspend fun cancel() {
        if (hasRunningJobs) {
            delegate.cancelRecognition()
            eventChannel.send(SpeechToText.Event.RecognitionCancelled)
        }
        super.cancel()
    }

    private fun startTimeout(timeout: Long) = launch {
        delay(timeout)
        exceptionChannel.send(RecognitionTimeoutException())
        cancel()
    }

    private fun provideChannelsForDelegate() {
        delegate.eventChannel = eventChannel
        delegate.exceptionChannel = exceptionChannel
    }

    internal suspend fun setDelegate(speechToText: SpeechToText) {
        if (delegate != speechToText) {
            cancel()
            delegate.destroy()
            delegate = speechToText
            provideChannelsForDelegate()
        }
    }
}