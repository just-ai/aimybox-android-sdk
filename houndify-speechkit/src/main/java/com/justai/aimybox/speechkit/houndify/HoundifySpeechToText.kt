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
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.speechtotext.SpeechToText
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import java.util.*

private val L = Logger("Houndify Speechkit")

@Suppress("unused")
class HoundifySpeechToText(
    context: Context,
    clientId: String,
    clientKey: String,
    recognitionTimeout: Long = 10000L
) : SpeechToText(recognitionTimeout) {

    private val coroutineContext = Dispatchers.IO + CoroutineName("Aimybox-(HoundifySTT)")

    private val factory = createVoiceFactory(context, clientId, clientKey)
    private var voiceSearch: VoiceSearch? = null
    private var audioStream: SimpleAudioByteStreamSource? = null

    /*
    * SpeechToText
    * */

    @RequiresPermission("android.permission.RECORD_AUDIO")
    override fun startRecognition(): Flow<Result> {
        return  callbackFlow {
            voiceSearch = factory.build(channel)
            voiceSearch?.start()
        }.catch { e ->
            onException(HoundifySpeechToTextException(cause = e))
        }.flowOn(coroutineContext)
    }

    override suspend fun stopRecognition() {
        voiceSearch?.stopRecording()
    }

    override suspend fun cancelRecognition() {
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
                    resultChannel.trySendBlocking(Result.Final(lastResult))
                    resultChannel.close()
                }

                override fun onAbort(info: VoiceSearchInfo) {
                    resultChannel.trySendBlocking(Result.Exception(HoundifySpeechToTextException("Houndify is aborted")))
                    resultChannel.close()
                }

                override fun onError(e: Exception, info: VoiceSearchInfo) {
                    resultChannel.trySendBlocking(Result.Exception(HoundifySpeechToTextException(cause = e)))
                    resultChannel.close()
                }

                override fun onTranscriptionUpdate(transcript: PartialTranscript) {
                    lastResult = transcript.partialTranscript
                    resultChannel.trySendBlocking(Result.Partial(lastResult))
                }

                override fun onRecordingStopped() {}
            })
            setAudioSource(audioStream)
        }.build()
    }

    private fun buildRequestInfo(context: Context) =
        HoundRequestInfoFactory.getDefault(context).apply {
            userId = UserIdFactory.get(context)
            requestId = UUID.randomUUID().toString()
        }

    override fun destroy() {
        voiceSearch?.abort()
    }
}
