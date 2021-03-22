package com.justai.aimybox.speechkit.kaldi

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.kaldi.KaldiRecognizer
import org.kaldi.Model
import org.kaldi.RecognitionListener
import org.kaldi.SpeechService
import java.lang.Exception

class KaldiSpeechToText(
    assets: KaldiAssets
): SpeechToText(), CoroutineScope {

    override val coroutineContext = Dispatchers.IO + Job()

    private var recognizer: SpeechService? = null
    private val initialization = CompletableDeferred<Model>()

    companion object {
        init {
            System.loadLibrary("kaldi_jni")
        }
    }

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
            val kaldiRecognizer = KaldiRecognizer(model, 16000f)
            recognizer = SpeechService(kaldiRecognizer, 16000f).apply {
                addListener(RecognizerListener(channel))
                startListening()
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