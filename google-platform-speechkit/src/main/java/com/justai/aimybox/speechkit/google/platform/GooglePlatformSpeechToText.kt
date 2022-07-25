package com.justai.aimybox.speechkit.google.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresPermission
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import java.util.*

@Suppress("unused")
class GooglePlatformSpeechToText(
    private val context: Context,
    var language: Locale = Locale.getDefault(),
    var preferOffline: Boolean = false,
    recognitionTimeout: Long = 10000L
) : SpeechToText(recognitionTimeout) {

    private val coroutineContext = Dispatchers.Main + CoroutineName("Aimybox-(GoogleSTT)")

    private var recognizer: SpeechRecognizer? = null

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): Flow<Result> {
        return  callbackFlow {
            if (recognizer == null) {
                L.i("Initializing Google Platform STT")
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                L.i("Google Platform STT is initialized")
            }
            recognizer?.setRecognitionListener(createRecognitionListener(channel))
            recognizer?.startListening(createRecognizerIntent(language.toString(), preferOffline))
        }.catch { e ->
            onException(GooglePlatformSpeechToTextException(code = 100 , cause = e))
        }.flowOn(coroutineContext)

    }

    override suspend fun stopRecognition() {
        withContext(Dispatchers.Main) {
            recognizer?.stopListening()
        }
    }

    override suspend fun cancelRecognition() {
        withContext(Dispatchers.Main) {
            recognizer?.cancel()
        }
    }

    override fun destroy() {
        recognizer?.destroy()
        coroutineContext.cancel()
    }

    private fun createRecognitionListener(resultChannel: SendChannel<Result>) =
        object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                resultChannel.trySendBlocking(Result.Partial(text))
            }

            override fun onBeginningOfSpeech() = onSpeechStart()

            override fun onEndOfSpeech() = onSpeechEnd()

            override fun onRmsChanged(rmsdB: Float) = onSoundVolumeRmsChanged(rmsdB)

            override fun onError(code: Int) {
                val exception = GooglePlatformSpeechToTextException(code, code.errorText)
                resultChannel.trySendBlocking(Result.Exception(exception))
                finish()
            }

            override fun onResults(results: Bundle?) {
                val text =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                resultChannel.trySendBlocking((Result.Final(text)))
                finish()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBufferReceived(buffer: ByteArray?) {
                buffer?.let { onAudioBufferReceived(buffer) }
            }

            private fun finish() {
                resultChannel.close()
                recognizer?.cancel()
            }
        }

    private fun createRecognizerIntent(language: String, preferOffline: Boolean) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
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