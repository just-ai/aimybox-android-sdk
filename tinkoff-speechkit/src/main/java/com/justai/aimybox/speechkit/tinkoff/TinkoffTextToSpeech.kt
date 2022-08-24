package com.justai.aimybox.speechkit.tinkoff

import android.content.Context

import com.justai.aimybox.extensions.put
import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.speechtotext.SampleRate
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import tinkoff.cloud.tts.v1.TextToSpeechGrpc
import tinkoff.cloud.tts.v1.Tts
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TinkoffTextToSpeech(
    context: Context,
    private val tokenProvider: TokenProvider,
    private val config:  TinkoffTextToSpeech.Config
) : BaseTextToSpeech(context){


    private val channel = ManagedChannelBuilder.forTarget("api.tinkoff.ai:443").build()
    private var clientTTS: TextToSpeechGrpc.TextToSpeechStub? = null

    override suspend fun speak(speech: TextSpeech) {

        try {
            val interceptor = attachHeadersToMetadata()
            clientTTS = TextToSpeechGrpc.newStub(channel).withInterceptors(interceptor)
            val audioData = request(speech.text)
            audioSynthesizer.play(AudioSpeech.Bytes(audioData))
        } catch (e: Throwable){
            throw  TinkoffCloudTextToSpeechException(cause = e)
        }
    }

    private suspend fun request(text: String): ByteArray{

        val audioConfig = Tts.AudioConfig.newBuilder()
            .setAudioEncoding(Tts.AudioEncoding.LINEAR16)
            .setSampleRateHertz(config.sampleRate.intValue)

        val requestBuilder = Tts.SynthesizeSpeechRequest.newBuilder()
            .setAudioConfig(audioConfig)

        if (config.voiceName.isNotBlank()) {
            val voiceSelctionParam = Tts.VoiceSelectionParams.newBuilder()
                .setName(config.voiceName)
            requestBuilder.setVoice(voiceSelctionParam)
        }

        val inputText = Tts.SynthesisInput.newBuilder()
            .setSsml(text)

        requestBuilder.setInput(inputText)

        return suspendCancellableCoroutine { continuation ->
            val streamObserver = StreamObserverImpl(continuation)
            clientTTS?.streamingSynthesize(requestBuilder.build(), streamObserver)
        }.use(InputStream::readBytes)
   }

    private
    suspend fun attachHeadersToMetadata(): ClientInterceptor {
        val token = tokenProvider.generateToken()
        val metadata = Metadata()
        metadata.put("authorization", "Bearer $token")

        return MetadataUtils.newAttachHeadersInterceptor(metadata)
    }


    class Config {
        val apiUrl: String = "api.tinkoff.ai:443"
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_48KHZ
        val voiceName: String = ""

    }

    class StreamObserverImpl(val continuation: CancellableContinuation<InputStream>): StreamObserver<Tts.StreamingSynthesizeSpeechResponse> {

        override fun onNext(value: Tts.StreamingSynthesizeSpeechResponse?) {
            when {
                value == null -> { }
                value.audioChunk.isEmpty -> {}
                else -> {
                    val byteData = value.audioChunk.newInput()
                    continuation.resume(byteData)
                }
            }
        }

        override fun onError(t: Throwable?) {
            if (t != null) {
                continuation.resumeWithException(t)
            }
        }

        override fun onCompleted() {
        }

    }
}

