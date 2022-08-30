package com.justai.aimybox.speechkit.tinkoff

import android.Manifest
import androidx.annotation.RequiresPermission
import com.google.protobuf.ByteString
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.extensions.put
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechtotext.SampleRate
import com.justai.aimybox.speechtotext.SpeechToText
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import tinkoff.cloud.stt.v1.SpeechToTextGrpc
import tinkoff.cloud.stt.v1.Stt

class TinkoffSpeechToText (
    private val tokenProvider: TokenProvider,
    val config: TinkoffSpeechToText.Config,
    maxAudioChunks: Int? = null,
    recognitionTimeout: Long = 10000L
    ) : SpeechToText(recognitionTimeout, maxAudioChunks) {

    private val channel = ManagedChannelBuilder.forTarget("api.tinkoff.ai:443").build()
    private var clientSTT: SpeechToTextGrpc.SpeechToTextStub? = null

    private val coroutineContext = Dispatchers.IO + CoroutineName("Aimybox-(TinkoffSTT)")

    private val audioRecorder = AudioRecorder("Tinkoff", config.sampleRate.intValue)

    override suspend fun stopRecognition() = cancelRecognition()

    override suspend fun cancelRecognition() {
        coroutineContext.cancelChildrenAndJoin()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): Flow<Result> {

        val interceptor = attachHeadersToMetadata()
        clientSTT = SpeechToTextGrpc.newStub(channel).withInterceptors(interceptor)

        return callbackFlow {

            val sessionConfig = getOrCreateRecognitionConfig()
            val requestBuilder = Stt.StreamingRecognizeRequest.newBuilder()

            val callBacks = SteamObserverCallbacksImpl(this)
            val observer = StreamObserverImpl(callBacks)
            val requestStream = clientSTT?.streamingRecognize(observer)
            requestStream?.onNext(requestBuilder.apply {
                streamingConfig = sessionConfig
            }.build())

            audioRecorder.startRecordingBytes().collect { data ->  //collectIN
                requestStream?.onNext(createRequest(data))
                onAudioBufferReceived(data)
                if (mustInterruptRecognition) {
                    L.w( "Interrupting stream")
                    channel.close()
                }
            }

            awaitClose {
                requestStream?.onCompleted()
            }
        }.catch { e ->
            onException(TinkoffCloudSpeechToTextException(cause = e))
        }.flowOn(coroutineContext)
    }

    private fun createRequest(data: ByteArray): Stt.StreamingRecognizeRequest =
        Stt.StreamingRecognizeRequest.newBuilder()
            .apply{
                audioContent = ByteString.copyFrom(data)
            }
            .build()

    private fun getOrCreateRecognitionConfig(): Stt.StreamingRecognitionConfig {

        val vadConfig = Stt.VoiceActivityDetectionConfig.newBuilder()
            .apply {
                silenceDurationThreshold = config.silenceDurationThreshold
            }
            .build()

        val recognitionConfig = Stt.RecognitionConfig.newBuilder()
            .apply {
                enableDenormalization = config.enableDenormalization
                profanityFilter = config.enableProfanityFilter
                enableAutomaticPunctuation = config.enableAutomaticPunctuation
                encoding = Stt.AudioEncoding.LINEAR16
                sampleRateHertz = config.sampleRate.intValue
                numChannels = 1
                setVadConfig(vadConfig)
            }
            .build()

        return Stt.StreamingRecognitionConfig.newBuilder()
            .setConfig(recognitionConfig)
            .setSingleUtterance(config.singleUtterance)
            .build()
    }

    private fun attachHeadersToMetadata(): ClientInterceptor {
        val token = tokenProvider.generateToken()
        val metadata = Metadata()
        metadata.put("authorization", "Bearer $token")

        return MetadataUtils.newAttachHeadersInterceptor(metadata)
    }

    //TODO Check way for
    interface SteamObserverCallbacks {
        var onResponse: (Stt.StreamingRecognizeResponse) -> Unit
        var onError: (Throwable) -> Unit
        var onCompleted: () -> Unit
    }

    inner class SteamObserverCallbacksImpl(val scope: ProducerScope<Result>) : SteamObserverCallbacks {

        override var onResponse: (Stt.StreamingRecognizeResponse) -> Unit =
            { response ->

                val chunk = response.resultsList.first().recognitionResult
                val text = chunk.alternativesList.first().transcript
                val result =
                    if (response.resultsOrBuilderList[0].isFinal) {
                        Result.Final(text)
                    } else {
                        Result.Partial(text)
                    }
                    scope.trySendBlocking(result)
            }

        override var onError: (Throwable) -> Unit =
            { exp ->
                val exception = TinkoffCloudSpeechToTextException(cause = exp)
                scope.trySendBlocking(Result.Exception(exception))
                onException(exception)
        }

        override var onCompleted: () -> Unit = {
            scope.channel.close()
        }
}

    class StreamObserverImpl(private val callBacks: SteamObserverCallbacks)
        : StreamObserver<Stt.StreamingRecognizeResponse>{
        override fun onNext(value: Stt.StreamingRecognizeResponse) =  callBacks.onResponse(value)
        override fun onError(t: Throwable) =  callBacks.onError(t)
        override fun onCompleted() = callBacks.onCompleted()
    }

    class Config {
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_48KHZ
        val enableProfanityFilter: Boolean = true
        val enableAutomaticPunctuation: Boolean = true
        val enableDenormalization: Boolean = true
        val silenceDurationThreshold = 1.0F
        val singleUtterance = false

    }
}