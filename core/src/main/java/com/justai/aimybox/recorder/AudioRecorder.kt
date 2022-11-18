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
import java.io.File
import java.io.FileOutputStream
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
    private val outputChannelBufferSizeChunks: Int = Channel.UNLIMITED,

    private val speechRecord: File? = null

) {

    companion object {
        private const val MILLISECONDS_IN_SECOND = 1000
        private const val BITS_PER_SAMPLE: Short = 16
    }

    private val L = Logger("$className $name")

    // override val coroutineContext: CoroutineContext = Dispatchers.AudioRecord + Job()

    //private val scope = CoroutineScope(coroutineContext)

    private val frameSize = getSampleSize() * channelCount

    private val bufferSize = calculateBufferSize()

    private var isRecording = false

    private var internalBufferSize = 0

    private val BYTE_RATE = sampleRate * channelCount * 16 / 8


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
//        val fileStream = FileOutputStream(speechRecord)

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
//                            fileStream.write(buffer)

                        }
                    }
                    delay(periodMs / 2)
                }

            } catch (e: CancellationException) {
                // Ignore
            } catch (e: Throwable) {
                currentCoroutineContext().cancel()
                L.e("Uncaught AudioRecord exception", e)
            } finally {
                withContext(NonCancellable) {
                    recorder.release()
                    L.i("Recording finished")
                   // fileStream.close()
                    currentCoroutineContext().cancel()
                }
            }
        }.flowOn(Dispatchers.IO)
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


    private fun createRecorder(): AudioRecord {
        val channelConfig = when (channelCount) {
            1 -> CHANNEL_IN_MONO
            else -> CHANNEL_IN_STEREO
        }
        internalBufferSize =
            AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 4

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

    private fun wavFileHeader(): ByteArray {
        val headerSize = 44
        val header = ByteArray(headerSize)

        header[0] = 'R'.toByte() // RIFF/WAVE header
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()

        header[4] = (0 and 0xff).toByte() // Size of the overall file, 0 because unknown
        header[5] = (0 shr 8 and 0xff).toByte()
        header[6] = (0 shr 16 and 0xff).toByte()
        header[7] = (0 shr 24 and 0xff).toByte()

        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()

        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()

        header[16] = 16 // Length of format data
        header[17] = 0
        header[18] = 0
        header[19] = 0

        header[20] = 1 // Type of format (1 is PCM)
        header[21] = 0

        header[22] = channelCount.toByte()
        header[23] = 0

        header[24] = (sampleRate and 0xff).toByte() // Sampling rate
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()

        header[28] = (BYTE_RATE and 0xff).toByte() // Byte rate = (Sample Rate * BitsPerSample * Channels) / 8
        header[29] = (BYTE_RATE shr 8 and 0xff).toByte()
        header[30] = (BYTE_RATE shr 16 and 0xff).toByte()
        header[31] = (BYTE_RATE shr 24 and 0xff).toByte()

        header[32] = (channelCount * BITS_PER_SAMPLE / 8).toByte() //  16 Bits stereo
        header[33] = 0

        header[34] = BITS_PER_SAMPLE.toByte() // Bits per sample
        header[35] = 0

        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()

        header[40] = (0 and 0xff).toByte() // Size of the data section.
        header[41] = (0 shr 8 and 0xff).toByte()
        header[42] = (0 shr 16 and 0xff).toByte()
        header[43] = (0 shr 24 and 0xff).toByte()

        return header
    }

    private fun updateHeaderInformation(data: ArrayList<Byte>) {
        val fileSize = data.size
        val contentSize = fileSize - 44

        data[4] = (fileSize and 0xff).toByte() // Size of the overall file
        data[5] = (fileSize shr 8 and 0xff).toByte()
        data[6] = (fileSize shr 16 and 0xff).toByte()
        data[7] = (fileSize shr 24 and 0xff).toByte()

        data[40] = (contentSize and 0xff).toByte() // Size of the data section.
        data[41] = (contentSize shr 8 and 0xff).toByte()
        data[42] = (contentSize shr 16 and 0xff).toByte()
        data[43] = (contentSize shr 24 and 0xff).toByte()
}
}
