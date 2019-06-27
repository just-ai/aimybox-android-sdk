package com.justai.aimybox.speechkit.yandex.cloud.stt

import com.google.protobuf.ByteString
import com.justai.aimybox.speechkit.yandex.cloud.AudioEncoding
import com.justai.aimybox.speechkit.yandex.cloud.IAmTokenGenerator
import com.justai.aimybox.speechkit.yandex.cloud.Language
import com.justai.aimybox.speechkit.yandex.cloud.VoiceModel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import yandex.cloud.ai.stt.v2.SttServiceGrpc
import yandex.cloud.ai.stt.v2.SttServiceOuterClass
import java.nio.charset.Charset
import kotlin.coroutines.CoroutineContext
import io.grpc.Channel as GRPCChannel

internal class YandexRecognitionApi(
    private val yandexPassportOAuthKey: String,
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

    private val channel = ManagedChannelBuilder
        .forAddress(config.apiUrl, config.apiPort)
        .build()

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

        val token = IAmTokenGenerator.getOAuthToken(yandexPassportOAuthKey)

        val requestStream = attachOAuthHeader(stub, token).streamingRecognize(
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
                config.encoding
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
        encoding: AudioEncoding
    ): SttServiceOuterClass.RecognitionSpec = SttServiceOuterClass.RecognitionSpec.newBuilder().apply {
        languageCode = language.stringValue
        model = voiceModel.stringValue
        profanityFilter = enableProfanityFilter
        partialResults = enablePartialResults
        sampleRateHertz = sampleRate
        audioEncoding = encoding.encodingValue
    }.build()

    private fun createRecognitionConfig(
        spec: SttServiceOuterClass.RecognitionSpec,
        folderId: String
    ): SttServiceOuterClass.RecognitionConfig = SttServiceOuterClass.RecognitionConfig.newBuilder().apply {
        specification = spec
        folderIdBytes = ByteString.copyFrom(folderId, Charset.forName("UTF-8"))
    }.build()

    private fun attachOAuthHeader(stub: SttServiceGrpc.SttServiceStub, token: String): SttServiceGrpc.SttServiceStub {
        val metadata = Metadata().apply {
            val key = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
            val value = "Bearer $token"
            put(key, value)
        }
        return MetadataUtils.attachHeaders(stub, metadata)
    }

}