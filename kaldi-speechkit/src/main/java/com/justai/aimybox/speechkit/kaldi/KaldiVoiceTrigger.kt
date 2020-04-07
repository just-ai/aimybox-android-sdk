package com.justai.aimybox.speechkit.kaldi

import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.voicetrigger.VoiceTrigger
import kotlinx.coroutines.*
import org.kaldi.Model
import org.kaldi.RecognitionListener
import org.kaldi.SpeechRecognizer
import java.lang.Exception

class KaldiVoiceTrigger(
    assets: KaldiAssets,
    private val phrases: Collection<String>
): VoiceTrigger, CoroutineScope {

    private val job = Job()
    override val coroutineContext = Dispatchers.IO + job

    private val initialization = CompletableDeferred<Model>()

    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

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

    override suspend fun startDetection(
        onTriggered: (phrase: String?) -> Unit,
        onException: (e: Throwable) -> Unit
    ) {
        if (!isListening) {
            recognizer = SpeechRecognizer(initialization.await()).apply {
                addListener(RecognizerListener(phrases, onTriggered, onException))
                startListening()
                isListening = true
            }

            L.i("Kaldi voice trigger was started")
        }
    }

    override suspend fun stopDetection() {
        recognizer?.run {
            cancel()
            shutdown()
            job.cancelChildrenAndJoin()
            isListening = false
            L.i("Kaldi voice trigger was stopped")
        }
    }

    internal class RecognizerListener(
        private val phrases: Collection<String>,
        private val onTriggered: (phrase: String?) -> Unit,
        private val onException: (e: Throwable) -> Unit
    ): RecognitionListener {
        private var triggered = false

        override fun onResult(result: String?) {}

        override fun onPartialResult(result: String?) {
            if (!triggered) {
                result?.parsePartial()?.findAnyOf(phrases)?.let {
                    triggered = true
                    L.i("Detected phrase ${it.second}")
                    onTriggered(it.second)
                }
            }
        }

        override fun onTimeout() {}

        override fun onError(e: Exception?) {
            L.e(e)
            e?.let { onException(e) }
        }

    }
}