package com.justai.aimybox.recorder

import android.media.AudioFormat
import android.media.AudioFormat.CHANNEL_IN_MONO
import android.media.AudioFormat.CHANNEL_IN_STEREO
import android.media.AudioRecord
import android.media.MediaRecorder
import com.justai.aimybox.extensions.className
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import kotlin.math.pow

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
    private val periodMs: Long = 400,
    /**
     * Output channel capacity in frames. One frame contains [periodMs] milliseconds of audio data.
     * */
    private val outputChannelBufferSizeChunks: Int = Channel.UNLIMITED

) {

    companion object {
        private const val MILLISECONDS_IN_SECOND = 1000
    }

    private val L = Logger("$className $name")

    // override val coroutineContext: CoroutineContext = Dispatchers.AudioRecord + Job()

    //private val scope = CoroutineScope(coroutineContext)

    private val frameSize = getSampleSize() * channelCount

    private val bufferSize = calculateBufferSize()

    private var isRecording = false

    private var internalBufferSize = 0

    /**
     * Launch new coroutine and start audio recording.
     * Will produce one frame of audio data each [periodMs] milliseconds.
     *
     * @return a flow of ByteArrays which contains recorded audio data.
     * */
    fun startRecordingBytes(): Flow<ByteArray> {
        L.i("Start recording: SampleRate=$sampleRate, FrameSize: $periodMs ms, BufferSize: $bufferSize bytes")
        // val countAttempts = 5
        // retry(countAttempts, delay = 300) { attempt ->

        val recorder = createRecorder()
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            L.e("Failed to init AudioRecord")
            recorder.release()
            throw IOException("Failed to init AudioRecord")
        }

        recorder.startRecording()
        isRecording = true
        val buffer = ByteArray(bufferSize)
        return flow<ByteArray> {
            try {
                var bytesCount = 0
                loop@ while (isRecording) {
                    val bytesRead = recorder.read(buffer, 0, bufferSize)
                    bytesCount += bytesRead
                    L.w("Reads byte: $bytesRead")
                    when {
                        bytesRead == 0 -> {
                            delay(periodMs)
                        }
                        bytesRead < 0 -> {
                            recorder.release()
                            throw IOException("Read $bytesRead bytes from recorder")
                        }
                        else -> {
                            emit(buffer.copyOf())
                        }
                    }
                    delay(periodMs)
                }

                //recorder.release()
                //  }
            } catch (e: CancellationException) {
                // Ignore
            } catch (e: Throwable) {
                currentCoroutineContext().cancel()
                L.e("Uncaught AudioRecord exception", e)
            } finally {
                withContext(NonCancellable) {
                    recorder.release()
                    L.i("Recording finished")
                    currentCoroutineContext().cancel()
                }
            }
        }.flowOn(Dispatchers.AudioRecord)
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
    suspend fun stopAudioRecording() {
        isRecording = false
    } //coroutineContext.cancelChildrenAndJoin()

    //fun interruptAudioRecording()  coroutineContext.cancelChildren()

    /**
     * Calculates a RMS level from recorded chunk
     *
     * @return an RMS level in Db
     */
    fun calculateRmsDb(data: ByteArray): Int {
        val avg = (data.sum() / data.size).toDouble()
        val sumMeanSquare: Double = data.fold(0.0) { acc, curr ->
            acc + (curr - avg).pow(2.0)
        }.toDouble()

        val averageMeanSquare = sumMeanSquare / data.size
        return (averageMeanSquare.pow(0.5) + 0.5).toInt()
    }


    private fun createRecorder():  AudioRecord {
        val channelConfig = when (channelCount) {
            1 -> CHANNEL_IN_MONO
            else -> CHANNEL_IN_STEREO
        }
        internalBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4

        val recoder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelCount,
            AudioFormat.ENCODING_PCM_16BIT,
            internalBufferSize
        )

        return recoder
    }


    private fun calculateBufferSize(): Int {

        val dataRate = frameSize * sampleRate

        return dataRate * periodMs.toInt() / MILLISECONDS_IN_SECOND

    }

    private fun getSampleSize(): Int {
        val sampleSize = when (audioFormat) {
            AudioFormat.ENCODING_PCM_FLOAT -> 2
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_8BIT -> 1
            else -> throw IllegalArgumentException("Unsupported format")
        }
        return sampleSize
    }


    private fun Flow<ByteArray>.convertBytesToShorts() = map { audioBytes ->
    check(audioBytes.size % 2 == 0)
    val audioData = ShortArray(audioBytes.size / 2)
    ByteBuffer.wrap(audioBytes)
        .order(ByteOrder.LITTLE_ENDIAN)
        .asShortBuffer()
        .get(audioData)
    audioData
}
}
