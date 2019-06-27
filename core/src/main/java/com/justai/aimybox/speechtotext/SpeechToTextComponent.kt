package com.justai.aimybox.speechtotext

import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.RecognitionTimeoutException
import com.justai.aimybox.logging.Logger
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SpeechToTextComponent(
    private var delegate: SpeechToText,
    private val eventChannel: SendChannel<SpeechToText.Event>,
    private val exceptionChannel: SendChannel<AimyboxException>
) : AimyboxComponent("STT") {

    init {
        provideChannelsForDelegate()
    }

    private val L = Logger("Aimybox-STT")

    @SuppressLint("MissingPermission")
    @RequiresPermission("android.permission.RECORD_AUDIO")
    internal suspend fun recognizeSpeech(): String? {
        cancel()
        return withContext(coroutineContext) {

            val recognitionChannel = delegate.startRecognition()
            eventChannel.send(SpeechToText.Event.RecognitionStarted)

            var finalResult: String? = null

            val timeoutTask = startTimeout(delegate.recognitionTimeoutMs) {
                exceptionChannel.send(RecognitionTimeoutException())
                cancel()
            }

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

    fun stopRecognition() {
        delegate.stopRecognition()
    }

    internal fun setDelegate(speechToText: SpeechToText) {
        if (delegate != speechToText) {
            cancel()
            delegate.destroy()
            delegate = speechToText
            provideChannelsForDelegate()
        }
    }

    private fun provideChannelsForDelegate() {
        delegate.eventChannel = eventChannel
        delegate.exceptionChannel = exceptionChannel
    }

    private fun startTimeout(timeout: Long, onTimeout: suspend () -> Unit) = launch {
        delay(timeout)
        onTimeout()
    }

    override fun cancel() {
        if (hasRunningJobs) {
            super.cancel()
            delegate.cancelRecognition()
            eventChannel.sendBlocking(SpeechToText.Event.RecognitionCancelled)
            L.w("Recognition cancelled")
        }
    }
}