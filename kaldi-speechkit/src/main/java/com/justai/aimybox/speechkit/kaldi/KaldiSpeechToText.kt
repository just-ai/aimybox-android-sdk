package com.justai.aimybox.speechkit.kaldi

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kaldi.Model
import org.kaldi.RecognitionListener
import org.kaldi.SpeechRecognizer
import java.lang.Exception

class KaldiSpeechToText(
    assets: KaldiAssets
): SpeechToText(), CoroutineScope {

    override val coroutineContext = Dispatchers.IO + Job()

    private var recognizer: SpeechRecognizer? = null
    private lateinit var model: Model

    companion object {
        init {
            System.loadLibrary("kaldi_jni")
        }
    }

    init {
        launch {
            model = Model(assets.directory)
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
        recognizer = SpeechRecognizer(model)
        recognizer?.addListener(RecognizerListener(channel))
        recognizer?.startListening()
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

private fun String.parseResult(): String {
    return JSONObject(this).optString("text")
}

private fun String.parsePartial(): String {
    return JSONObject(this).optString("partial")
}