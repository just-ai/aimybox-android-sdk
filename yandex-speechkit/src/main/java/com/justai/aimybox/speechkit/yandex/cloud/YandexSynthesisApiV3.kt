package com.justai.aimybox.speechkit.yandex.cloud

import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import speechkit.tts.v3.SynthesizerGrpc
import speechkit.tts.v3.Tts
import speechkit.tts.v3.Tts.AudioFormatOptions
import speechkit.tts.v3.Tts.ContainerAudio
import java.io.InputStream
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


internal class YandexSynthesisApiV3(
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    config: YandexTextToSpeechV3.Config,
): AbstractYandexSynthesisApi<YandexTextToSpeechV3.Config>(iAmTokenProvider, folderId, config) {
    private val channel = ManagedChannelBuilder
        .forAddress(config.apiUrl, config.apiPort)
        .build()

    private val stub: SynthesizerGrpc.SynthesizerStub = SynthesizerGrpc.newStub(channel)

    private val userId = UUID.randomUUID().toString()

    override suspend fun request(
        text: String,
        language: Language,
        config: YandexTextToSpeechV3.Config
    ): ByteArray {
        val containerAudio = ContainerAudio.newBuilder()
            .setContainerAudioType(ContainerAudio.ContainerAudioType.WAV)

        val outputAudioSpec = AudioFormatOptions.newBuilder()
            .setContainerAudio(containerAudio)

        val hint = Tts.Hints.newBuilder()
            .setVoice(config.voice.stringValue)


        val request = Tts.UtteranceSynthesisRequest.newBuilder()
            .setModel(VoiceModel.GENERAL.stringValue)
            .setText(text)
            .addHints(hint)
            .setOutputAudioSpec(outputAudioSpec)
            .build()

        val stubWithHeaders = stub.attachHeaders()

        return suspendCancellableCoroutine<InputStream> { continuation ->
            val responseObserver = object : StreamObserver<Tts.UtteranceSynthesisResponse> {
                override fun onNext(value: Tts.UtteranceSynthesisResponse?) {
                    when {
                        value == null -> {}
                        !value.hasAudioChunk() -> {}
                        value.audioChunk.data.isEmpty -> {}
                        else -> {
                            val byteData = value.audioChunk.data.newInput()
                            continuation.resume(byteData)
                        }
                    }
                }

                override fun onError(t: Throwable?) {
                    L.e("Exception occurred during API request. Request: $request")
                    if (t != null) {
                        continuation.resumeWithException(t)
                    }
                }

                override fun onCompleted() {}
            }
            stubWithHeaders.utteranceSynthesis(request, responseObserver)
        }.use(InputStream::readBytes)
    }

    private fun Metadata.put(key: String, value: String) {
        val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
        put(metaKey, value)
    }

    private suspend fun SynthesizerGrpc.SynthesizerStub.attachHeaders(): SynthesizerGrpc.SynthesizerStub {
        val token = iAmTokenProvider.getOAuthToken()
        val metadata = Metadata().apply {
            put("authorization", "Bearer $token")
            put("x-data-logging-enabled", config.enableLoggingData.toString())
            put("x-client-request-id", userId)
            put("x-folder-id", folderId)
        }
        return MetadataUtils.attachHeaders(this, metadata)
    }
}