package com.justai.aimybox.speechkit.yandex.cloud

import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import yandex.cloud.api.ai.stt.v2.SttServiceGrpc
import yandex.cloud.api.ai.stt.v2.SttServiceOuterClass
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext

internal class YandexRecognitionApi(
    private val iAmTokenProvider: IAmTokenProvider,
    private val folderId: String,
    private var language: Language,
    private val config: YandexSpeechToText.Config
) : CoroutineScope {

    companion object {
        fun createRequest(data: ByteArray) = SttServiceOuterClass.StreamingRecognitionRequest.newBuilder().apply {
            audioContent = ByteString.copyFrom(data)
        }.build()!!
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    private val channel = PinnedChannelBuilder
        .build(config.apiUrl, config.apiPort, config.pinningConfig)

    private val stub = SttServiceGrpc.newStub(channel)

    private var recognitionConfig: SttServiceOuterClass.RecognitionConfig? = null

    internal fun setLanguage(language: Language) {
        this.language = language
        recognitionConfig = null
    }

    internal suspend fun openStream(
        onResponse: (SttServiceOuterClass.StreamingRecognitionResponse) -> Unit,
        onError: (Throwable) -> Unit,
        onCompleted: () -> Unit
    ): StreamObserver<SttServiceOuterClass.StreamingRecognitionRequest> {

        val token = iAmTokenProvider.getOAuthToken()

        val requestStream = attachHeaders(stub, token).streamingRecognize(
            object : StreamObserver<SttServiceOuterClass.StreamingRecognitionResponse> {
                override fun onNext(value: SttServiceOuterClass.StreamingRecognitionResponse) = onResponse(value)
                override fun onError(t: Throwable) = onError(t)
                override fun onCompleted() = onCompleted()
            }
        )

        val sessionConfig = getOrCreateRecognitionConfig()
        requestStream.onNext(SttServiceOuterClass.StreamingRecognitionRequest.newBuilder().apply {
            config = sessionConfig
        }.build())

        return requestStream
    }

    private fun getOrCreateRecognitionConfig(): SttServiceOuterClass.RecognitionConfig {
        val config = recognitionConfig ?: createRecognitionConfig(
            createRecognitionSpec(
                language,
                config.voiceModel,
                config.enableProfanityFilter,
                config.enablePartialResults,
                config.sampleRate.longValue,
                config.rawResults,
                config.literatureText
            ),
            folderId
        )
        recognitionConfig = config
        return config
    }

    private fun createRecognitionSpec(
        language: Language,
        voiceModel: VoiceModel,
        enableProfanityFilter: Boolean,
        enablePartialResults: Boolean,
        sampleRate: Long,
        rawResults: Boolean,
        literatureText: Boolean
    ): SttServiceOuterClass.RecognitionSpec = SttServiceOuterClass.RecognitionSpec.newBuilder().apply {
        languageCode = language.stringValue
        model = voiceModel.stringValue
        profanityFilter = enableProfanityFilter
        partialResults = enablePartialResults
        sampleRateHertz = sampleRate
        audioEncoding = SttServiceOuterClass.RecognitionSpec.AudioEncoding.LINEAR16_PCM
        this.rawResults = rawResults
        this.literatureText = literatureText
    }.build()

    private fun createRecognitionConfig(
        spec: SttServiceOuterClass.RecognitionSpec,
        folderId: String
    ): SttServiceOuterClass.RecognitionConfig = SttServiceOuterClass.RecognitionConfig.newBuilder().apply {
        specification = spec
        folderIdBytes = ByteString.copyFrom(folderId, Charset.forName("UTF-8"))
    }.build()

    private fun attachHeaders(stub: SttServiceGrpc.SttServiceStub, token: String): SttServiceGrpc.SttServiceStub {
        val metadata = Metadata().apply {
            put("authorization", "Bearer $token")
            put("x-data-logging-enabled", config.enableLoggingData)
            put("x-normalize-partials", config.normalizePartialData)
        }
        return MetadataUtils.attachHeaders(stub, metadata)
    }

    private fun Metadata.put(key: String, value: Any) {
        val newKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
        val newValue = value.toString()
        put(newKey, newValue)
    }
}