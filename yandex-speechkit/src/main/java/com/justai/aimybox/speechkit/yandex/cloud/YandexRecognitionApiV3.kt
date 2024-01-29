package com.justai.aimybox.speechkit.yandex.cloud

import com.google.protobuf.ByteString
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import yandex.cloud.api.ai.stt.v3.RecognizerGrpc
import yandex.cloud.api.ai.stt.v3.Stt

class YandexRecognitionApiV3(
    private val iAmTokenProvider: IAmTokenProvider,
    private val folderId: String,
    private var language: Language,
    private val config: YandexSpeechToText.Config
) {


    companion object {
        fun createRequest(data: ByteArray) = Stt.StreamingRequest.newBuilder().apply {
            this.chunk = Stt.AudioChunk.newBuilder()
                .apply {
                    this.data = ByteString.copyFrom(data)
                }
                .build()
        }.build()!!
    }



    private val channel = PinnedChannelBuilder
        .build(config.apiUrl, config.apiPort, config.pinningConfig)
    private val stub = RecognizerGrpc.newStub(channel)
    private var recognitionConfig: Stt.StreamingOptions? = null

    internal fun setLanguage(language: Language) {
        this.language = language
        recognitionConfig = null
    }

    internal suspend fun openStream(
        onResponse: (Stt.StreamingResponse) -> Unit,
        onError: (Throwable) -> Unit,
        onCompleted: () -> Unit
    ): StreamObserver<Stt.StreamingRequest>? {

        val token = iAmTokenProvider.getOAuthToken()
        val requestStream = attachHeaders(stub, token)?.recognizeStreaming(

            object : StreamObserver<Stt.StreamingResponse> {
                override fun onNext(value: Stt.StreamingResponse) = onResponse(value)
                override fun onError(t: Throwable) = onError(t)
                override fun onCompleted() = onCompleted()
            }
        )

        val sessionConfig = getOrCreateRecognitionConfig()

        requestStream?.onNext(Stt.StreamingRequest.newBuilder().apply {
            this.sessionOptions = sessionConfig
        }.build())

        L.w("stream opened")
        return requestStream
    }

    private fun getOrCreateRecognitionConfig(): Stt.StreamingOptions {
        val config = recognitionConfig ?: createRecognitionSpec(
            language,
            config.enableProfanityFilter,
            config.sampleRate.longValue,
            config.rawResults,
            config.literatureText
        )
        recognitionConfig = config
        return config
    }

    private fun createRecognitionSpec(
        language: Language,
        enableProfanityFilter: Boolean,
        sampleRate: Long,
        rawResults: Boolean,
        literatureText: Boolean
    ): Stt.StreamingOptions {

        val textNormalization = Stt.TextNormalizationOptions.newBuilder()
            .setLiteratureText(literatureText)
            .setProfanityFilter(enableProfanityFilter)
            .setTextNormalization(
                if (rawResults) Stt.TextNormalizationOptions.TextNormalization.TEXT_NORMALIZATION_ENABLED
                else Stt.TextNormalizationOptions.TextNormalization.TEXT_NORMALIZATION_DISABLED
            )
            .build()

        val audioFormatOptions = Stt.AudioFormatOptions.newBuilder()
            .apply {
                setRawAudio(Stt.RawAudio.newBuilder()
                    .apply {
                        sampleRateHertz = sampleRate
                        audioEncoding = Stt.RawAudio.AudioEncoding.LINEAR16_PCM
                    }
                )
            }
            .build()

        val languageRestrictionOptions = Stt.LanguageRestrictionOptions.newBuilder()
            .apply {
                restrictionType = Stt.LanguageRestrictionOptions.LanguageRestrictionType.WHITELIST
                addLanguageCode("ru-RU") //(language.stringValue)
            }
            .build()

        val recognitionModel = Stt.RecognitionModelOptions.newBuilder()
            .setTextNormalization(textNormalization)
            .setAudioFormat(audioFormatOptions)
            .setLanguageRestriction(languageRestrictionOptions)
            .setAudioProcessingType(Stt.RecognitionModelOptions.AudioProcessingType.REAL_TIME)
            .build()

        val eouClassifierOptions = Stt.EouClassifierOptions.newBuilder().apply {
            defaultClassifier = Stt.DefaultEouClassifier.newBuilder().apply {
                //type = Stt.DefaultEouClassifier.EouSensitivity.HIGH
            }.build()
        }.build()

        return Stt.StreamingOptions.newBuilder()
            .setRecognitionModel(recognitionModel)
            .setEouClassifier(eouClassifierOptions)
            .build()

    }


    private fun attachHeaders(
        stub: RecognizerGrpc.RecognizerStub,
        token: String
    ): RecognizerGrpc.RecognizerStub? {
        val extraHeaders = Metadata().apply {
            put("authorization", "Bearer $token")
            put("x-data-logging-enabled", config.enableLoggingData)
            put("x-folder-id", folderId)
        }
        return stub.withInterceptors(
            MetadataUtils.newAttachHeadersInterceptor(extraHeaders)
        )

    }

    private fun Metadata.put(key: String, value: Any) {
        val newKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
        val newValue = value.toString()
        put(newKey, newValue)
    }
}



