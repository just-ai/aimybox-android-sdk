package com.justai.aimybox.speechkit.yandex.cloud

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechtotext.SampleRate
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import yandex.cloud.api.ai.stt.v2.SttServiceOuterClass


@Suppress("unused")
class YandexSpeechToText(
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    language: Language,
    config: Config = Config(),
    maxAudioChunks: Int? = null,
    recognitionTimeout: Long = 10000L
) : SpeechToText(recognitionTimeout, maxAudioChunks) {

    private val coroutineContext = Dispatchers.IO + CoroutineName("Aimybox-(YandexSTT)")

    private val audioRecorder = AudioRecorder("Yandex", config.sampleRate.intValue)

    private val api = YandexRecognitionApi(iAmTokenProvider, folderId, language, config)

    fun setLanguage(language: Language) = api.setLanguage(language)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): Flow<Result> {
        initCounter()
        return  callbackFlow {
                val requestStream = api.openStream(
                    { response ->
                        val chunk = response.chunksList.first()
                        val text = chunk.alternativesList.first().text
                        val result = if (chunk.final) Result.Final(text) else Result.Partial(text)
                        trySendBlocking(result)
                    },
                    { exp ->
                        val exception = YandexCloudSpeechToTextException(cause = exp)
                        trySendBlocking(Result.Exception(exception))
                        onException(exception)
                    },
                    onCompleted = { channel.close() }
                )

                audioRecorder.startRecordingBytes().collect { data ->  //collectIN
                        requestStream.onNext(YandexRecognitionApi.createRequest(data))
                        onAudioBufferReceived(data)
                        if (mustInterruptRecognition) {
                            L.w( "Interrupting stream")
                            channel.close()
                        }
                    }

                awaitClose {
                    requestStream.onCompleted()
                }
            }.catch { e ->
                onException(YandexCloudSpeechToTextException(cause = e))
            }.flowOn(coroutineContext)
        }




    override suspend fun stopRecognition() = cancelRecognition()

    override suspend fun cancelRecognition() {
        coroutineContext.cancelChildrenAndJoin()
    }

    override fun destroy() {
        coroutineContext.cancel()
    }


    data class Config(
        val apiUrl: String = "stt.api.cloud.yandex.net",
        val apiPort: Int = 443,
        val voiceModel: VoiceModel = VoiceModel.GENERAL,
        val enableProfanityFilter: Boolean = true,
        val enablePartialResults: Boolean = true,
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_48KHZ,
        val rawResults: Boolean = false,
        val literatureText: Boolean = false,
        val enableLoggingData: Boolean = false,
        val normalizePartialData: Boolean = false,
        val pinningConfig: PinningConfig? = null
    )
}

