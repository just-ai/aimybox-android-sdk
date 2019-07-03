package com.justai.aimybox.speechkit.google.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class GooglePlatformSpeechToText(
    private val context: Context,
    var language: Locale = Locale.getDefault(),
    var preferOffline: Boolean = true
) : SpeechToText(), CoroutineScope {

    override val coroutineContext = Dispatchers.Main + Job()

    private var recognizer: SpeechRecognizer? = null

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): ReceiveChannel<Result> {
        val channel = Channel<Result>()
        launch {
            withContext(Dispatchers.Main) {
                if (recognizer == null) {
                    L.i("Initializing Google Platform STT")
                    recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    L.i("Google Platform STT is initialized")
                }
                recognizer?.setRecognitionListener(createRecognitionListener(channel))

                recognizer?.startListening(createRecognizerIntent(language.toString(), preferOffline))
            }
        }
        return channel
    }

    override fun stopRecognition() {
        launch(Dispatchers.Main) { recognizer?.cancel() }
    }

    override fun cancelRecognition() {
        launch(Dispatchers.Main) { recognizer?.cancel() }
    }

    override fun destroy() {
        launch(Dispatchers.Main) { recognizer?.destroy() }
        coroutineContext.cancel()
    }

    private fun createRecognitionListener(resultChannel: Channel<Result>) = object : RecognitionListener {
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            sendResult(Result.Partial(text))
        }

        override fun onBeginningOfSpeech() = onSpeechStart()

        override fun onEndOfSpeech() = onSpeechEnd()

        override fun onRmsChanged(rmsdB: Float) = onSoundVolumeRmsChanged(rmsdB)

        override fun onError(error: Int) {
            val exception = SpeechToTextException(IOException("SpeechRecognizer error[$error]: ${error.errorText}"))
            sendResult(Result.Exception(exception))
            resultChannel.close()
        }

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            sendResult(Result.Final(text))
            resultChannel.close()
        }

        private fun sendResult(result: Result) {
            if (resultChannel.isClosedForSend) {
                L.w("Channel $resultChannel is closed for send. Omitting $result")
            } else {
                resultChannel.offer(result).let { success ->
                    if (!success) L.w("Failed to send $result to $resultChannel")
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onReadyForSpeech(params: Bundle?) {}

        override fun onBufferReceived(buffer: ByteArray?) {}
    }

    private fun createRecognizerIntent(language: String, preferOffline: Boolean) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            }
        }

    private val Int.errorText
        get() = when (this) {
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> " Network operation timed out."
            SpeechRecognizer.ERROR_NETWORK -> "Network related error."
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. "
            SpeechRecognizer.ERROR_SERVER -> "Server sends error status."
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            else -> "Unknown error"
        }
}