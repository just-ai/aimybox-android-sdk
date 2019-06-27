package com.justai.aimybox.speechkit.houndify

import android.content.Context
import androidx.annotation.RequiresPermission
import com.hound.android.fd.UserIdFactory
import com.hound.android.sdk.VoiceSearch
import com.hound.android.sdk.VoiceSearchInfo
import com.hound.android.sdk.audio.SimpleAudioByteStreamSource
import com.hound.android.sdk.util.HoundRequestInfoFactory
import com.hound.core.model.sdk.HoundResponse
import com.hound.core.model.sdk.PartialTranscript
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import java.io.IOException
import java.util.*

private val L = Logger("Houndify")

class HoundifySpeechToText(
    context: Context,
    clientId: String,
    clientKey: String
) : SpeechToText() {

    private val factory = createVoiceFactory(context, clientId, clientKey)
    private var voiceSearch: VoiceSearch? = null
    private var audioStream: SimpleAudioByteStreamSource? = null

    /*
    * SpeechToText
    * */

    @RequiresPermission("android.permission.RECORD_AUDIO")
    override fun startRecognition(): ReceiveChannel<Result> {
        val channel = Channel<Result>()
        voiceSearch = factory.build(channel)
        voiceSearch?.start()
        return channel
    }

    override fun stopRecognition() {
        L.d("stopRecognition")
        voiceSearch?.stopRecording()
    }

    override fun cancelRecognition() {
        L.d("cancelRecognition")
        voiceSearch?.abort()
    }


    private fun createVoiceFactory(context: Context, clientId: String, clientKey: String) = object {
        fun build(resultChannel: SendChannel<Result>) = VoiceSearch.Builder().apply {
            audioStream = SimpleAudioByteStreamSource()

            setRequestInfo(buildRequestInfo(context))
            setClientId(clientId)
            setClientKey(clientKey)
            setListener(object : VoiceSearch.HoundResponseListener {
                private var lastResult: String? = null

                override fun onResponse(response: HoundResponse?, info: VoiceSearchInfo?) {
                    sendResult(Result.Final(lastResult))
                    resultChannel.close()
                }

                override fun onAbort(info: VoiceSearchInfo) {
                    sendResult(Result.Exception(SpeechToTextException(IOException("Houndify is aborted"))))
                    resultChannel.close()
                }

                override fun onError(e: Exception, info: VoiceSearchInfo) {
                    sendResult(Result.Exception(SpeechToTextException(e)))
                    resultChannel.close()
                }

                override fun onTranscriptionUpdate(transcript: PartialTranscript) {
                    lastResult = transcript.partialTranscript
                    sendResult(Result.Partial(lastResult))
                }

                override fun onRecordingStopped() {}

                private fun sendResult(result: Result) {
                    if (resultChannel.isClosedForSend) {
                        L.w("Channel $resultChannel is closed for send. Omitting $result")
                    } else {
                        resultChannel.offer(result).let { success ->
                            if (!success) L.w("Failed to send $result to $resultChannel")
                        }
                    }
                }
            })
            setAudioSource(audioStream)
        }.build()
    }

    private fun buildRequestInfo(context: Context) = HoundRequestInfoFactory.getDefault(context).apply {
        userId = UserIdFactory.get(context)
        requestId = UUID.randomUUID().toString()
    }

    override fun destroy() {
        voiceSearch?.abort()
    }
}
