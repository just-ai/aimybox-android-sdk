package com.justai.aimybox.recorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.extensions.className
import com.justai.aimybox.extensions.retry
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.map
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine scope audio recorder intended to use in [SpeechToText]
 * */
@Suppress("unused")
@ExperimentalCoroutinesApi
class AudioRecorder(
    /**
     * Name of the recorder
     * */
    name: String,
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
    private val channelCount: Int = 1,
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

    private val L = Logger("$className $name")

    override val coroutineContext: CoroutineContext = Dispatchers.AudioRecord + Job()

    private val minBufferSize = calculateMinBufferSize()
    private val bufferSize = calculateBufferSize(minBufferSize)

    /**
     * Launch new coroutine and start audio recording.
     * Will produce one frame of audio data each [periodMs] milliseconds.
     *
     * @return a channel of ByteArrays which contains recorded audio data.
     * */
    fun startRecordingBytes(): ReceiveChannel<ByteArray> {
        val channel = Channel<ByteArray>(outputChannelBufferSizeChunks)

        lateinit var recorder: AudioRecord

        launch {
            try {
                L.i("Start recording: SampleRate=$sampleRate, FrameSize: $periodMs ms, BufferSize: $bufferSize bytes")

                retry(5, delay = 300) { attempt ->
                    recorder = createRecorder()
                    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                        L.e("Failed to init AudioRecord, attempt $attempt")
                        recorder.release()
                        throw IOException("Failed to init AudioRecord after 5 retries")
                    }

                    recorder.startRecording()

                    val buffer = ByteArray(bufferSize)
                    loop@ while (isActive) {
                        val bytesRead = recorder.read(buffer, 0, buffer.size)
                        when {
                            bytesRead <= 0 -> {
                                recorder.release()
                                throw IOException("Read $bytesRead bytes from recorder")
                            }
                            channel.isClosedForSend -> {
                                L.w("Output channel is closed to send, $bytesRead bytes of recorded data omitted.")
                                break@loop
                            }
                            else -> channel.send(buffer.copyOf())
                        }
                    }

                    recorder.release()
                }
            } catch (e: CancellationException) {
                // Ignore
            } catch (e: Throwable) {
                channel.close(e)
                L.e("Uncaught AudioRecord exception", e)
            } finally {
                withContext(NonCancellable) {
                    recorder.release()
                    L.i("Recording finished")
                    channel.close()
                }
            }
        }

        return channel
    }

    /**
     * Launch new coroutine and start audio recording.
     * Will produce one frame of audio data each [periodMs] milliseconds.
     *
     * @return a channel of ShortArrays which contains recorded audio data.
     * */
    fun startRecordingShorts() = startRecordingBytes().convertBytesToShorts()

    /**
     * Stop the current recording.
     * This feature is synchronous, ensuring that all resources are released when it returns.
     * */
    suspend fun stopAudioRecording() = coroutineContext.cancelChildrenAndJoin()

    private fun createRecorder() = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channelCount,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
    )

    private fun calculateMinBufferSize() =
        AudioRecord.getMinBufferSize(sampleRate, channelCount, audioFormat).also {
            require(it != AudioRecord.ERROR && it != AudioRecord.ERROR_BAD_VALUE) {
                "Sample rate $sampleRate is not supported."
            }
        }

    private fun calculateBufferSize(minBufferSize: Int): Int {
        val sampleSize = when (audioFormat) {
            AudioFormat.ENCODING_PCM_FLOAT -> 2
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_8BIT -> 1
            else -> throw IllegalArgumentException("Unsupported format")
        }

        val frameSize = sampleSize * channelCount
        val dataRate = frameSize * sampleRate

        val bufferSize = dataRate * periodMs / MILLISECONDS_IN_SECOND

        require(bufferSize > minBufferSize) {
            "Buffer is too small. Current size: $bufferSize, min size: $minBufferSize"
        }

        return bufferSize
    }

    private fun ReceiveChannel<ByteArray>.convertBytesToShorts() = map { audioBytes ->
        check(audioBytes.size % 2 == 0)
        val audioData = ShortArray(audioBytes.size / 2)
        ByteBuffer.wrap(audioBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
            .get(audioData)
        audioData
    }
}
