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
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext
import kotlin.math.pow

/**
 * Coroutine scope audio recorder intended to use in [SpeechToText]
 * */
//@Suppress("unused")
//@ExperimentalCoroutinesApi
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
)   {

    companion object {
        private const val MILLISECONDS_IN_SECOND = 1000
    }

    private var isReading = false

    private val L = Logger("$className $name")

    //val coroutineContext: CoroutineContext = Dispatchers.AudioRecord + Job()

    private val bufferSize = calculateBufferSize()

    /**
     * Launch new coroutine and start audio recording.
     * Will produce one frame of audio data each [periodMs] milliseconds.
     *
     * @return a flow of ByteArrays which contains recorded audio data.
     * */
    fun startRecordingBytes(): Flow<ByteArray> {

        isReading = true
        val recorder = createRecorder()

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            L.e("Failed to init AudioRecord")
            recorder.release()
            throw IOException("Failed to init AudioRecord")
        }

        return flow {

            try {
                L.i("Start recording: SampleRate=$sampleRate, FrameSize: $periodMs ms, BufferSize: $bufferSize bytes")
                val countAttempts = 5
//                retry(countAttempts, delay = 300) { attempt ->
//
//                    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
//                        L.e("Failed to init AudioRecord, attempt $attempt")
//                        recorder.release()
//                        throw IOException("Failed to init AudioRecord after $countAttempts retries")
//                    }
//
//                    recorder.startRecording()
//
//                    loop@ while (currentCoroutineContext().isActive) {
//                        val buffer = ByteArray(bufferSize)
//                        val bytesRead = recorder.read(buffer, 0, buffer.size)
//                        when {
//                            bytesRead <= 0 -> {
//                                recorder.release()
//                                throw IOException("Read $bytesRead bytes from recorder")
//                            }
//                            else -> emit(buffer)
//
//                        }
//                    }
//
//                    recorder.release()
//                }

               // retry(countAttempts, delay = 300) { attempt ->

                    recorder.startRecording()
                    val buffer = ByteArray(bufferSize)
                    loop@ while (currentCoroutineContext().isActive && isReading) {

                       // val buffer = ByteArray(bufferSize)
                        val bytesRead = recorder.read(buffer, 0, buffer.size)
                        when {
                            bytesRead <= 0 -> {
                                recorder.release()
                                throw IOException("Read $bytesRead bytes from recorder")
                            }
                            else -> emit(buffer.copyOf(bytesRead))

                        }


                    }

                    recorder.release()

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
    //suspend fun stopAudioRecording() { //= coroutineContext.cancelChildrenAndJoin()
//    }
    fun stopAudioRecording(){
        isReading = false
    }
   // fun interruptAudioRecording() = coroutineContext.cancelChildren()

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

    private fun createRecorder() = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channelCount,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
    )

    private fun calculateBufferSize(): Int {
        val sampleSize = when (audioFormat) {
            AudioFormat.ENCODING_PCM_FLOAT -> 2
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_8BIT -> 1
            else -> throw IllegalArgumentException("Unsupported format")
        }

        val frameSize = sampleSize * channelCount
        val dataRate = frameSize * sampleRate

        return dataRate * periodMs / MILLISECONDS_IN_SECOND
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
