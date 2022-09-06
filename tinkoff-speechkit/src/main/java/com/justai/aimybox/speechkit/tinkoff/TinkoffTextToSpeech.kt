package com.justai.aimybox.speechkit.tinkoff

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
import android.media.AudioTrack
import android.media.AudioTrack.MODE_STREAM
import com.justai.aimybox.extensions.put
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.speechtotext.SampleRate
import com.justai.aimybox.texttospeech.BaseTextToSpeech
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import tinkoff.cloud.tts.v1.TextToSpeechGrpc
import tinkoff.cloud.tts.v1.Tts

class TinkoffTextToSpeech(
    context: Context,
    private val tokenProvider: TokenProvider,
    private val config: Config
) : BaseTextToSpeech(context) {


    private val channel = ManagedChannelBuilder.forTarget(config.apiUrl).build()
    private var clientTTS: TextToSpeechGrpc.TextToSpeechBlockingStub? = null

    override suspend fun speak(speech: TextSpeech) {

        try {
            val interceptor = attachHeadersToMetadata()
            clientTTS = TextToSpeechGrpc.newBlockingStub(channel).withInterceptors(interceptor)
            val audioStream = AudioStreamRecorder(config.sampleRate.intValue)
            request(speech.text)
                .onCompletion {
                    audioStream.close()
                }
                .collect { data ->
                    audioStream.writeData(data)
                }
        } catch (e: Throwable) {
            throw  TinkoffCloudTextToSpeechException(cause = e)
        }
    }

    private suspend fun request(text: String): Flow<ByteArray> {

        val audioConfig = Tts.AudioConfig.newBuilder()
            .setSampleRateHertz(config.sampleRate.intValue)
            .setAudioEncoding(Tts.AudioEncoding.LINEAR16)

        val requestBuilder = Tts.SynthesizeSpeechRequest.newBuilder()
            .setAudioConfig(audioConfig)

        if (config.voiceName.isNotBlank()) {
            val voiceSelctionParam = Tts.VoiceSelectionParams.newBuilder()
                .setName(config.voiceName)
            requestBuilder.setVoice(voiceSelctionParam)
        }

        val inputTextBuilder = Tts.SynthesisInput.newBuilder()
            .apply {
                clearText()
                if (config.inputInSSML) {
                    ssml = text
                } else {
                    setText(text)
                }
            }

        requestBuilder.setInput(inputTextBuilder)

        val request = requestBuilder.build()
        val responses = clientTTS?.streamingSynthesize(request)

        return flow {
            if (responses == null) {
                emit(ByteArray(0))
                return@flow
            }
            for (response in responses) {
                emit(response.audioChunk.toByteArray())
            }
        }
    }

    private
    fun attachHeadersToMetadata(): ClientInterceptor {
        val token = tokenProvider.generateToken()
        val metadata = Metadata()
        metadata.put("authorization", "Bearer $token")

        return MetadataUtils.newAttachHeadersInterceptor(metadata)
    }


    class Config {
        val apiUrl = "api.tinkoff.ai:443"
        val sampleRate = SampleRate.SAMPLE_RATE_48KHZ
        val voiceName = ""
        val inputInSSML = false

    }

}

//TODO Make this recorder master after some refactoring
class AudioStreamRecorder(val sampleRate: Int) {

    private val bufferSize =
        AudioTrack.getMinBufferSize(sampleRate, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT)

    private val audioTrack = AudioTrack(
        AudioAttributes.Builder().apply {
            setUsage(AudioAttributes.USAGE_MEDIA)
            setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        }.build(),
        AudioFormat.Builder().apply {
            setChannelMask(ENCODING_PCM_16BIT)
        }.build(),
        bufferSize,
        MODE_STREAM,
        AUDIO_SESSION_ID_GENERATE
    ).apply {
        play()
    }

    private var bytesWereWritten = 0

    fun writeData(audioData: ByteArray): Int {
        bytesWereWritten = audioTrack.write(audioData, 0, audioData.size)
        return bytesWereWritten
    }

    fun close() {
        audioTrack.stop()
        audioTrack.release()
    }

}


