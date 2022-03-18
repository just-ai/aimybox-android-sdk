package com.justai.aimybox.core

import com.justai.aimybox.Aimybox
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response

/**
 * Interface for custom client-side skill.
 * To enable it in Aimybox, add an instance of the skill to [Config.skills].
 * */
interface CustomSkill<TRequest : Request, in TResponse : Response> {
    /**
     * This method will be called just before any request to dialog api.
     * You can modify the request or return it without changes.
     * */
    suspend fun onRequest(request: TRequest, aimybox: Aimybox): TRequest = request

    /**
     *
     */
    var isRequestHandled: Boolean

    /**
     * Determines whether the current skill can handle the [response].
     * If no skill matches, the default handler will be called.
     * */
    fun canHandle(response: TResponse): Boolean = false

    /**
     * Called if [canHandle] returned true for the [response].
     *
     * **Note: default response handler will not be called, so you should manually implement [Aimybox] behavior.
     * In the most cases it is enough to call [Aimybox.standby] when you finish processing.**
     *
     * If you're going to use speech synthesis, consider to look at [Aimybox.NextAction] behavior modifier.
     *
     * @see Aimybox.NextAction
     * @see Aimybox.speak
     * */
    suspend fun onResponse(
        response: TResponse,
        aimybox: Aimybox,
        defaultHandler: suspend (Response) -> Unit
    ) = defaultHandler(response)
}