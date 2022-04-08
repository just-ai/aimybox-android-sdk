package com.justai.aimybox.api

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.core.*
import com.justai.aimybox.extensions.className
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.model.reply.AudioReply
import com.justai.aimybox.model.reply.TextReply
import com.justai.aimybox.model.reply.asAudioSpeech
import com.justai.aimybox.model.reply.asTextSpeech
import kotlinx.coroutines.*

/**
 * Interface for dialog api implementation.
 *
 * @see AimyboxDialogApi
 * */

abstract class DialogApi<TRequest : Request, TResponse : Response> :
    AimyboxComponent("API") {

    companion object {
        const val DEFAULT_REQUEST_TIMEOUT = 10_000L
    }

    /**
     * Provide your custom skills to this set.
     * */
    //protected abstract val customSkills: LinkedHashSet<CustomSkill<TRequest, TResponse>>
    protected abstract val customSkills: LinkedHashSet<CustomSkill<TRequest, TResponse>>

    /**
     * Override this value to change request timeout. By default it returns [DEFAULT_REQUEST_TIMEOUT].
     * */
    protected open val requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT

    /**
     * Create the dialog api request.
     * */
    abstract fun createRequest(query: String): TRequest

    /**
     * Send the [request] to dialog api.
     *
     * @return [Response]
     * */
    abstract suspend fun send(request: TRequest): TResponse

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    internal suspend fun send(query: String, aimybox: Aimybox, isSilentRequest: Boolean = false) {
        cancelRunningJob()
        withContext(coroutineContext) {
            val baseRequest = createRequest(query)
            val request =
                customSkills.filter { it.canHandleRequest(baseRequest) }
                    .fold(baseRequest) { request, skill ->
                        skill.onRequest(request, aimybox)
                    }

            val response = try {
                withTimeout(requestTimeoutMs) {
                    send(request).also {
                        aimybox.dialogApiEvents.invokeEvent(Event.RequestSent(request))
                    }
                }.also {
                    aimybox.dialogApiEvents.invokeEvent(Event.ResponseReceived(it))
                }
            } catch (e: TimeoutCancellationException) {
                val timeoutException = ApiRequestTimeoutException(request, requestTimeoutMs)
                logger.e(timeoutException)
                aimybox.exceptions.invokeEvent(timeoutException)
                aimybox.standby()
            } catch (e: CancellationException) {
                logger.w("Request was cancelled")
                logger.d(request)
                aimybox.dialogApiEvents.invokeEvent(Event.RequestCancelled(request))
            } catch (e: Throwable) {
                logger.e("Error during request", e)
                logger.d(request)
                aimybox.exceptions.invokeEvent(ApiException(cause = e))
                aimybox.standby()
            } as? TResponse

            if (response != null) handle(response, aimybox, isSilentRequest)
            else logger.d("Response is empty for $request")
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun handle(response: TResponse, aimybox: Aimybox, isSilentRequest: Boolean = false) {
        scope.launch {
            val skill = customSkills.find { it.canHandle(response) }

            if (skill != null) {
                logger.i("Found skill \"${skill.className}\" for action \"${response.action}\". ")
                skill.onResponse(
                    response,
                    aimybox,
                    defaultHandler = { response -> handleDefault(response, aimybox) })
            } else {
                if (!response.action.isNullOrBlank()) {
                    logger.w("No suitable skill found for action \"${response.action}\". Handling by default...")
                }
                if (!isSilentRequest) handleDefault(response, aimybox)
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    protected open suspend fun handleDefault(response: Response, aimybox: Aimybox) {
        try {
            val nextAction = Aimybox.NextAction.byQuestion(response.question)

            val speeches = response.replies
                .filter { it is TextReply || it is AudioReply }
                .map {
                    when (it) {
                        is TextReply -> it.asTextSpeech()
                        is AudioReply -> it.asAudioSpeech()
                        else -> throw IllegalArgumentException("Reply type is not supported by default handler")
                    }
                }

            speeches.takeIf { it.isNotEmpty() }?.let { it ->

                try {
                    val filteredSpeeches = it.filter { speech ->
                        !(speech is TextSpeech && speech.text.isEmpty()) }
                    if (filteredSpeeches.isNotEmpty()) {
                        aimybox.speak(filteredSpeeches, nextAction)?.join()
                    }
                } catch (e: CancellationException) {
                    logger.w("Speech cancelled", e)
                }
            } ?: aimybox.standby()

        } catch (e: Throwable) {
            logger.e("Failed to parse replies from $response", e)
        }
    }

    fun getCustomSkill(skillName: String) =
        customSkills.find { skill ->
            skill::class.java.name == skillName

        }

    /**
     * Events, which you can receive using [Aimybox.dialogApiEvents] channel.
     * */
    sealed class Event {

        /**
         * Dispatched just after the [request] has been sent.
         *
         * Changing [Request.data]  will not take any effect,
         * to change it before request is sent, write a [CustomSkill] and add it to [Config].
         * */
        data class RequestSent(val request: Request) : Event()

        /**
         * Dispatched when the [response] is received.
         * */
        data class ResponseReceived(val response: Response) : Event()

        /**
         * Dispatched when the [request] has been cancelled.
         * */
        data class RequestCancelled(val request: Request) : Event()
    }

}
