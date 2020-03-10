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
import kotlin.coroutines.CoroutineContext

@ExperimentalCoroutinesApi
class KaldiWebsocketSpeechToText(
    private val uri: String,
    sampleRate: Int = 8000
): SpeechToText(), CoroutineScope {

    override val coroutineContext: CoroutineContext = Dispatchers.IO + Job()

    private lateinit var ws: WebSocket
    private val audioRecorder = AudioRecorder("Kaldi", sampleRate)

    override suspend fun stopRecognition() {
        audioRecorder.stopAudioRecording()
    }

    override suspend fun cancelRecognition() {
        ws.disconnect()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecognition() = produce<Result> {
        val audioData = audioRecorder.startRecordingBytes()

        launch {
            ws = WebSocketFactory().createSocket(uri).addListener(
                SocketListener(channel)
            ).connectAsynchronously()

            audioData.consumeEach { data ->
                ws.sendBinary(data)
                onAudioBufferReceived(data)
            }

            ws.disconnect()
        }

        invokeOnClose {
            audioData.cancel()
            ws.disconnect()
        }
    }

    override fun destroy() {
        coroutineContext.cancel()
    }

    class SocketListener(
        private val channel: SendChannel<Result>
    ): WebSocketAdapter() {

        override fun onTextMessage(websocket: WebSocket?, text: String?) {
            text?.parsePartial().takeIf { it?.isNotEmpty()!! }?.let {
                channel.offer(Result.Partial(it))
            }
            text?.parseResult().takeIf { it?.isNotEmpty()!! }?.let {
                channel.offer(Result.Final(it))
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