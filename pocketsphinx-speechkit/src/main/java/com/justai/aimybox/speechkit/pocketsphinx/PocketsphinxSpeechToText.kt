package com.justai.aimybox.speechkit.pocketsphinx

import androidx.annotation.RequiresPermission
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.speechtotext.SpeechToText
import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.RecognitionListener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import java.io.File

class PocketsphinxSpeechToText(
    recognizerProvider: PocketsphinxRecognizerProvider,
    grammarFilePath: String,
    recognitionTimeout: Long = 10000L
): SpeechToText(recognitionTimeout) {

    companion object {
        private const val GRAMMAR_SEARCH = "grammar"
    }
    private val coroutineContext = Dispatchers.IO + CoroutineName("Aimybox-(PocketsphinxSTT)")

    private val recognizer = recognizerProvider.recognizer

    init {
        recognizer.addGrammarSearch(GRAMMAR_SEARCH, File(grammarFilePath))
    }

    override suspend fun cancelRecognition() {
        recognizer.cancel()
    }

    @RequiresPermission("android.permission.RECORD_AUDIO")
    override fun startRecognition(): Flow<Result> {
        return  callbackFlow {

            val listener = object : RecognitionListener {
                override fun onResult(hyp: Hypothesis?) {
                    channel.trySendBlocking(Result.Final(hyp?.hypstr))
                }

                override fun onPartialResult(hyp: Hypothesis?) {
                    val text = hyp?.hypstr
                    val result =
                        if (mustInterruptRecognition) Result.Final(text) else Result.Partial(text)
                    channel.trySendBlocking(result)
                }

                override fun onTimeout() {
                    finish()
                }

                override fun onBeginningOfSpeech() = onSpeechStart()

                override fun onEndOfSpeech() {
                    L.d("onEndOfSpeech")
                    onSpeechEnd()
                    recognizer.stop()
                }

                override fun onError(e: Exception?) {
                        channel.trySendBlocking(Result.Exception(SpeechToTextException(e?.message)))
                }

                fun finish() {
                    channel.close()
                    recognizer.removeListener(this)
                }
            }

            recognizer.addListener(listener)
            recognizer.startListening(GRAMMAR_SEARCH)
        }.catch { e ->
            onException(SpeechToTextException(cause = e))
        }.flowOn(coroutineContext)

    }

    override suspend fun stopRecognition() {
        recognizer.stop()
    }

    override fun destroy() {
        recognizer.shutdown()
    }
}