package com.justai.aimybox.recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

private val L = Logger("AudioRecorder")

/**
 * Coroutine scope audio recorder intended to use in [SpeechToText]
 * */
@Suppress("unused")
@ExperimentalCoroutinesApi
class AudioRecorder(
    /**
     * Recording sample rate in Hertz
     * */
    private val sampleRate: Int = 16000,
    /**
     * Recording format.
     *
     * @see AudioFormat
     * */
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    /**
     * Recording channel configuration.
     *
     * @see AudioFormat
     * */
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    /**
     * Data chunk duration in milliseconds.
     * */
    private val periodMs: Int = 400,
    /**
     * Output channel capacity in frames. One frame contains [periodMs] milliseconds of audio data.
     * */
    private val outputChannelBufferSizeChunks: Int = Channel.UNLIMITED
) : CoroutineScope {

    companion object {
        private const val MILLISECONDS_IN_SECOND = 1000
    }

    override val coroutineContext: CoroutineContext = Dispatchers.AudioRecord + Job()

    /**
     * Launch new coroutine and start audio recording.
     * Will produce one frame of audio data each [periodMs] milliseconds.
     *
     * @return a channel of ByteArrays which contains recorded audio data.
     * */
    fun startAudioRecording() = produce(capacity = outputChannelBufferSizeChunks) {
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        require(minBufferSize != AudioRecord.ERROR && minBufferSize != AudioRecord.ERROR_BAD_VALUE) {
            "Sample rate $sampleRate is not supported."
        }

        val bufferSize = calculateBufferSize()
        check(bufferSize > minBufferSize) {
            "Buffer is too small. Current size: $bufferSize, min size: $minBufferSize"
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        try {
            L.i("Start recording: SampleRate=$sampleRate, FrameSize: $periodMs ms, BufferSize: $bufferSize b")
            audioRecord.startRecording()

            launch {
                while (isActive) {
                    val buffer = ByteArray(bufferSize)
                    val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                    if (!isClosedForSend) {
                        send(buffer)
                    } else {
                        L.i(
                            """Output channel is closed to send, 
                                |$bytesRead bytes of recorded data omitted.""".trimMargin()
                        )
                    }
                }
            }

            invokeOnClose {
                L.i("Releasing recorder")
                audioRecord.release()
            }
        } catch (e: CancellationException) {
            // Ignore
        } catch (e: Throwable) {
            close(e)
            L.e("Uncaught AudioRecord exception", e)
        } finally {
            L.i("Recording finished")
        }
    }

    /**
     * Stop the current recording.
     * This feature is synchronous, ensuring that all resources are released when it returns.
     * */
    fun stopAudioRecording() = runBlocking {
        coroutineContext.cancelChildren()
        coroutineContext[Job]?.children?.toList()?.joinAll()
    }

    private fun calculateBufferSize(): Int {
        val sampleSize = when (audioFormat) {
            AudioFormat.ENCODING_PCM_FLOAT -> 2
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_8BIT -> 1
            else -> throw IllegalArgumentException("Unsupported format")
        }

        val channelsCount = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 1
            else -> throw IllegalArgumentException("Unsupported channel config")
        }

        val frameSize = sampleSize * channelsCount
        val dataRate = frameSize * sampleRate

        return dataRate * periodMs / MILLISECONDS_IN_SECOND
    }
}
