package com.justai.aimybox.speechkit.snowboy

import ai.kitt.snowboy.SnowboyInternalApiProxy
import android.content.Context
import com.getkeepsafe.relinker.ReLinker
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.voicetrigger.VoiceTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
class SnowboyVoiceTrigger(
    context: Context,
    assets: SnowboyAssets,
    sensitivity: Float = 0.6F,
    audioGain: Float = 1.0F
) : VoiceTrigger, CoroutineScope {

    init {
        ReLinker.loadLibrary(context, "snowboy-detect-android")
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + job

    private val detector =
        SnowboyInternalApiProxy(assets.resourcesFilePath, assets.modelFilePath).apply {
            setSensitivity(sensitivity)
            setAudioGain(audioGain)
            applyFrontend()
        }

    private val recorder = AudioRecorder("Snowboy")

    private fun startRecording(onTriggered: (phrase: String?) -> Unit) {
        val audioDataChannel = recorder.startRecordingShorts()

        launch {
            audioDataChannel.consumeEach { audioData ->
                detector.runDetection(audioData).let { resultCode ->
                    when (resultCode) {
                        -2 -> L.i("No speech detected")
                        -1 -> L.i("Unknown detection error")
                        0 -> L.i("Speech detected, but no hotwords found")
                        else -> if (resultCode > 0) {
                            L.i("Hotword $resultCode detected")
                            onTriggered(resultCode.toString())
                        } else {
                            L.e("Unexpected result code: $resultCode")
                        }
                    }
                }
            }
        }
    }

    override suspend fun startDetection(
        onTriggered: (phrase: String?) -> Unit,
        onException: (e: Throwable) -> Unit
    ) {
        startRecording(onTriggered)
    }

    override suspend fun stopDetection() {
        recorder.stopAudioRecording()
        job.cancelChildrenAndJoin()
    }

    override fun destroy() {
        launch { stopDetection() }
        detector.delete()
    }

}