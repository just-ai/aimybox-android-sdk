package com.justai.aimybox.api

import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.core.Config
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply

/**
 * Interface for dialog api implementation.
 *
 * @see AimyboxDialogApi
 * */
interface DialogApi {

    companion object {
        const val DEFAULT_REQUEST_TIMEOUT = 10_000L
    }

    /**
     * Override this function to change request timeout. By default it returns [DEFAULT_REQUEST_TIMEOUT].
     * */
    fun getRequestTimeoutMs(): Long = DEFAULT_REQUEST_TIMEOUT

    /**
     * Send the [request] to dialog api.
     *
     * @return [Response]
     * */
    suspend fun send(request: Request): Response?

    fun destroy()

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
        data class ResponseReceived(val response: Response?) : Event()

        /**
         * Dispatched when the [request] has been cancelled.
         * */
        data class RequestCancelled(val request: Request): Event()

        /**
         * Dispatched when the [reply] is going to be processed.
         * */
        data class NextReply(val reply: Reply) : Event()
    }

}
