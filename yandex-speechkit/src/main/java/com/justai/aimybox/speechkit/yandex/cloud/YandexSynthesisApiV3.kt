package com.justai.aimybox.speechkit.yandex.cloud

import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.suspendCancellableCoroutine
import yandex.cloud.api.ai.tts.v3.SynthesizerGrpc
import yandex.cloud.api.ai.tts.v3.Tts
import yandex.cloud.api.ai.tts.v3.Tts.AudioFormatOptions
import yandex.cloud.api.ai.tts.v3.Tts.ContainerAudio
import java.io.InputStream
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


internal class YandexSynthesisApiV3(
    private val iAmTokenProvider: IAmTokenProvider,
    private val folderId: String,
    val config: YandexTextToSpeech.ConfigV3,
): AbstractYandexSynthesisApi {
    private val channel = PinnedChannelBuilder.build(config.apiUrl, config.apiPort, config.pinningConfig)

    private val stub: SynthesizerGrpc.SynthesizerStub = SynthesizerGrpc.newStub(channel)

    private val userId = UUID.randomUUID().toString()

    override suspend fun request(
        text: String,
        language: Language
    ): ByteArray {
        val containerAudio = ContainerAudio.newBuilder()
            .setContainerAudioType(ContainerAudio.ContainerAudioType.WAV)

        val outputAudioSpec = AudioFormatOptions.newBuilder()
            .setContainerAudio(containerAudio)

        val voiceHint = Tts.Hints.newBuilder()
            .setVoice(config.voice.stringValue)
        val speedHint = Tts.Hints.newBuilder()
            .setSpeed(config.speed.floatValue.toDouble())
        val volumeHint = Tts.Hints.newBuilder()
            .setVolume(config.volume.doubleValue)

        val request = Tts.UtteranceSynthesisRequest.newBuilder()
            .setText(text)
            .addHints(voiceHint)
            .addHints(speedHint)
            .addHints(volumeHint)
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