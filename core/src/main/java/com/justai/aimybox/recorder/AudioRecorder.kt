package com.justai.aimybox.recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

private val L = Logger("AudioRecorder")

/**
 * Coroutine scope audio recorder intended to use in [SpeechToText]
 * */
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
     * Data sample duration in milliseconds.
     * */
    private val sampleSizeMs: Int = 400,
    /**
     * Output channel capacity in chunks. One chunk contains [sampleSizeMs] milliseconds of audio data.
     * */
    private val outputChannelBufferSizeChunks: Int = Channel.UNLIMITED
) : CoroutineScope {

    private val recordingDispatcher = Dispatchers.AudioRecord

    override val coroutineContext: CoroutineContext = recordingDispatcher + Job()

    /**
     * Launch new coroutine and start audio recording.
     * Will produce chunks of audio data each [sampleSizeMs] milliseconds.
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
            L.d("Start recording")
            audioRecord.startRecording()

            launch {
                val buffer = ByteArray(bufferSize)
                while (isActive) {
                    buffer.fill(0)
                    val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                    if (!isClosedForSend) {
                        send(buffer)
                    } else {
                        L.w(
                            """Output channel is closed to send, $bytesRead bytes omitted.
                                Check channel capacity to prevent data loss.""".trimIndent()
                        )
                    }
                }
            }

            invokeOnClose {
                L.i("Stopping recorder")
                audioRecord.tryStop()
                L.i("Releasing recorder")
                audioRecord.release()
            }
        } catch (e: CancellationException) {
            // Ignore
        } catch (e: Throwable) {
            close(e)
            L.e("Uncaught AudioRecord exception", e)
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

    private fun AudioRecord.tryStop() = try {
        stop()
    } catch (e: IllegalStateException) {
        // Ignore
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
        return sampleSize * sampleRate * channelsCount * sampleSizeMs / 1000
    }
}
