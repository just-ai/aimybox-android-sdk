package com.justai.aimybox.api

import android.Manifest
import androidx.annotation.RequiresPermission
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.core.*
import com.justai.aimybox.extensions.className
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.TextReply
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
    protected abstract suspend fun send(request: TRequest): TResponse

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    internal suspend fun send(query: String, aimybox: Aimybox) {
        cancelRunningJob()
        withContext(coroutineContext) {
            val request = customSkills.fold(createRequest(query)) { request, skill ->
                skill.onRequest(request)
            }

            val response = try {
                withTimeout(requestTimeoutMs) {
                    send(request).also {
                        aimybox.dialogApiEvents.send(Event.RequestSent(request))
                    }
                }.also {
                    aimybox.dialogApiEvents.send(Event.ResponseReceived(it))
                }
            } catch (e: TimeoutCancellationException) {
                val timeoutException = ApiRequestTimeoutException(request, requestTimeoutMs)
                L.e(timeoutException)
                aimybox.exceptions.send(timeoutException)
            } catch (e: CancellationException) {
                L.w("Request was cancelled")
                L.d(request)
                aimybox.dialogApiEvents.send(Event.RequestCancelled(request))
            } catch (e: Throwable) {
                L.e("Error during request", e)
                L.d(request)
                aimybox.exceptions.send(ApiException(cause = e))
            } as? TResponse

            if (response != null) handle(response, aimybox)
            else L.d("Response is empty for $request")
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun handle(response: TResponse, aimybox: Aimybox) {
        launch {
            val skill = customSkills.find { it.canHandle(response) }

            if (skill != null) {
                L.i("Found skill \"${skill.className}\" for action \"${response.action}\". ")
                skill.onResponse(
                    response,
                    aimybox,
                    defaultHandler = { response -> handleDefault(response, aimybox) })
            } else {
                if (!response.action.isNullOrBlank()) {
                    L.w("No suitable skill found for action \"${response.action}\". Handling by default...")
                }
                handleDefault(response, aimybox)
            }
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    protected open suspend fun handleDefault(response: Response, aimybox: Aimybox): Unit = try {
        val lastTextReply = response.replies.lastOrNull { it is TextReply }

        response.replies.filterIsInstance<TextReply>().forEach { reply ->
            val nextAction = if (reply == lastTextReply) {
                Aimybox.NextAction.byQuestion(response.question)
            } else {
                Aimybox.NextAction.STANDBY
            }
            try {
                aimybox.speak(reply.asTextSpeech(), nextAction)?.join()
            } catch (e: CancellationException) {
                L.w("Speech cancelled", e)
            }
        }
    } catch (e: Throwable) {
        L.e("Failed to parse replies from $response", e)
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
