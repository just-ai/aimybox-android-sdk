package com.justai.aimybox.speechkit.kaldi

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.recorder.AudioRecorder
import com.justai.aimybox.speechtotext.SpeechToText
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class KaldiWebsocketSpeechToText(
    private val uri: String,
    sampleRate: Int = 8000,
    maxAudioChunks: Int? = null,
    recognitionTimeout: Long = 10000L
): SpeechToText(recognitionTimeout, maxAudioChunks) {

    private val coroutineContext: CoroutineContext = Dispatchers.IO + CoroutineName("Aimybox-(KaldiWebsocket)")

    private lateinit var ws: WebSocket
    private val audioRecorder = AudioRecorder("Kaldi", sampleRate)

    override suspend fun stopRecognition() {
        audioRecorder.stopAudioRecording()
    }

    override suspend fun cancelRecognition() {
        ws.disconnect()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition(): Flow<Result> {
        initCounter()
        return  callbackFlow {
            val audioData = audioRecorder.startRecordingBytes()

                ws = WebSocketFactory().createSocket(uri).addListener(
                    SocketListener(channel)
                ).connectAsynchronously()

                audioData.collect { data ->
                    ws.sendBinary(data)
                    onAudioBufferReceived(data)
                    if (mustInterruptRecognition) {
                        L.d("Interrupting stream")
                        channel.close()
                    }
                }

            awaitClose {
                ws.disconnect()
            }
        }
    }

    override fun destroy() {
        coroutineContext.cancel()
    }

    inner class SocketListener(
        private val channel: SendChannel<Result>
    ): WebSocketAdapter() {

        override fun onTextMessage(websocket: WebSocket?, text: String?) {
            text?.parsePartial().takeIf { it?.isNotEmpty()!! }?.let {
                val result = if (mustInterruptRecognition) Result.Final(it) else Result.Partial(it)
                channel.trySendBlocking(result)
            }
            text?.parseResult().takeIf { it?.isNotEmpty()!! }?.let {
                channel.trySendBlocking(Result.Final(it))
            }
        }

        override fun onDisconnected(
            websocket: WebSocket?,
            serverCloseFrame: WebSocketFrame?,
            clientCloseFrame: WebSocketFrame?,
            closedByServer: Boolean
        ) {
            L.d("Disconnected ${closedByServer.takeIf { it }?.let { "by server" }  }}")
            channel.close()
        }
    }
}