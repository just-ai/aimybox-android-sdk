package com.justai.aimybox.speechkit.pocketsphinx

import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.channels.ReceiveChannel

import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.RecognitionListener
import edu.cmu.pocketsphinx.SpeechRecognizerSetup
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.lang.Exception

class PocketsphinxSpeechToText(
    assets: PocketsphinxAssets,
    private val timeout: Long = 5000
): SpeechToText(), CoroutineScope {

    companion object {
        private const val GRAMMAR_SEARCH = "grammar"
    }

    override val coroutineContext = Dispatchers.IO + Job()
    private lateinit var channel: Channel<Result>
    private lateinit var timeoutTask: Job

    private val recognizer = SpeechRecognizerSetup.defaultSetup()
        .setAcousticModel(File(assets.acousticModelFilePath))
        .setDictionary(File(assets.dictionaryFilePath))
        .recognizer

    private val listener = object : RecognitionListener {
        override fun onResult(hyp: Hypothesis?) {
            launch {
                channel.send(Result.Final(hyp?.hypstr))
                finish()
            }
        }

        override fun onPartialResult(hyp: Hypothesis?) {
            launch {
                channel.send(Result.Partial(hyp?.hypstr))
            }
        }

        override fun onTimeout() {}

        override fun onBeginningOfSpeech() = onSpeechStart()

        override fun onEndOfSpeech() {
            L.d("onEndOfSpeech")
            onSpeechEnd()
            timeoutTask.cancel()
            recognizer.stop()
        }

        override fun onError(e: Exception?) {
            launch {
                channel.send(Result.Exception(SpeechToTextException(e?.message)))
                finish()
            }
        }

        fun finish() {
            channel.close()
        }
    }

    init {
        recognizer.addGrammarSearch(GRAMMAR_SEARCH, File(assets.grammarFilePath))
        recognizer.addListener(listener)
    }

    override suspend fun cancelRecognition() {
        timeoutTask.cancel()
        recognizer.cancel()
    }

    override fun startRecognition(): ReceiveChannel<Result> {
        channel = Channel()
        recognizer.startListening(GRAMMAR_SEARCH)
        timeoutTask = launch {
            delay(timeout)
            recognizer.stop()
        }
        return channel
    }

    override suspend fun stopRecognition() {
        timeoutTask.cancel()
        recognizer.stop()
    }

    override fun destroy() {
        timeoutTask.cancel()
        recognizer.shutdown()
    }
}