package com.justai.aimybox.speechkit.yandex.cloud

import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import yandex.cloud.api.ai.stt.v3.RecognizerGrpc
import yandex.cloud.api.ai.stt.v3.Stt

class YandexRecognitionV3(
    private val iAmTokenProvider: IAmTokenProvider,

    private val config: YandexSpeechToText.Config
) {


    private val channel = PinnedChannelBuilder
        .build(config.apiUrl, config.apiPort, config.pinningConfig)

    private val stub = RecognizerGrpc.newStub(channel)

    private var recognitionConfig: Stt.StreamingOptions? = null

    internal suspend fun openStream(
        onResponse: (Stt.StreamingResponse) -> Unit,
        onError: (Throwable) -> Unit,
        onCompleted: () -> Unit
    ): StreamObserver<Stt.StreamingRequest>? {

        val token = iAmTokenProvider.getOAuthToken()
        val requestStream = attachHeaders(stub, token)?.recognizeStreaming(

            object: StreamObserver<Stt.StreamingResponse>{
               override fun onNext(value: Stt.StreamingResponse) = onResponse(value)
               override fun onError(t: Throwable) = onError(t)
               override fun onCompleted() = onCompleted()
            }
        )

        val sessionConfig = getOrCreateRecognitionConfig()

        requestStream?.onNext(Stt.StreamingRequest.newBuilder().apply {
          this.sessionOptions = sessionConfig
        }.build())


        return requestStream
    }

    private fun getOrCreateRecognitionConfig(): Stt.StreamingOptions {
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
        sampleRate: Long,
        rawResults: Boolean,
        literatureText: Boolean
    ): Stt.StreamingOptions{

        val textNormalization = Stt.TextNormalizationOptions.newBuilder()
            .setLiteratureText(literatureText)
            .setProfanityFilter(enableProfanityFilter)

        val audioFormatOptions = Stt.AudioFormatOptions.newBuilder().apply {
            setRawAudio(Stt.RawAudio.newBuilder().apply {
                sampleRateHertz = sampleRate
            })
        }

        val recognitionModel = Stt.RecognitionModelOptions.newBuilder()
           .setTextNormalization(textNormalization)
           .setAudioFormat(audioFormatOptions)

        val options = Stt.StreamingOptions.newBuilder()
            .setRecognitionModel(recognitionModel)
            .





//        languageCode = language.stringValue
//        model = voiceModel.stringValue
//        profanityFilter = enableProfanityFilter
//        partialResults = enablePartialResults
//        sampleRateHertz = sampleRate
//        audioEncoding = SttServiceOuterClass.RecognitionSpec.AudioEncoding.LINEAR16_PCM
//        this.rawResults = rawResults
//        this.literatureText = literatureText
    }


    private fun attachHeaders(stub: RecognizerGrpc.RecognizerStub, token: String): RecognizerGrpc.RecognizerStub? {
        val extraHeaders = Metadata().apply {
            put("authorization", "Bearer $token")
            put("x-data-logging-enabled", config.enableLoggingData)
            put("x-normalize-partials", config.normalizePartialData)
        }
        return stub.withInterceptors(
            MetadataUtils.newAttachHeadersInterceptor(extraHeaders))

    }

    private fun Metadata.put(key: String, value: Any) {
        val newKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
        val newValue = value.toString()
        put(newKey, newValue)
    }
}



