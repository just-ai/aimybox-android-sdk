package com.justai.aimybox.speechkit.kaldi

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.lang.Exception

class KaldiSpeechToText(
    assets: KaldiAssets,
    recognitionTimeout: Long = 10000L
): SpeechToText(recognitionTimeout), CoroutineScope {

    override val coroutineContext = Dispatchers.IO + Job()

    private var recognizer: SpeechService? = null
    private val initialization = CompletableDeferred<Model>()

    init {
        launch {
            initialization.complete(Model(assets.directory))
        }
    }

    override suspend fun stopRecognition() {
        recognizer?.stop()
    }

    override suspend fun cancelRecognition() {
        recognizer?.cancel()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): ReceiveChannel<Result> {
        val channel = Channel<Result>()
        launch {
            val model = initialization.await()
            val kaldiRecognizer = Recognizer(model, 16000f)
            recognizer = SpeechService(kaldiRecognizer, 16000f).apply {
                startListening(RecognizerListener(channel))
            }
        }
        return channel
    }

    internal class RecognizerListener(
        private val channel: Channel<Result>
    ): RecognitionListener, CoroutineScope {

        override val coroutineContext = Dispatchers.IO

        override fun onResult(result: String?) {
            launch {
                channel.send(Result.Final(result?.parseResult()))
                finish()
            }
        }

        override fun onFinalResult(result: String?) {
            launch {
                channel.send(Result.Final(result?.parseResult()))
                finish()
            }
        }

        override fun onPartialResult(result: String?) {
            launch {
                channel.send(Result.Partial(result?.parsePartial()))
            }
        }

        override fun onTimeout() {}

        override fun onError(e: Exception?) {
            launch {
                channel.send(Result.Exception(SpeechToTextException(e?.message)))
                finish()
            }
        }

        private fun finish() {
            channel.cancel()
        }
    }
}