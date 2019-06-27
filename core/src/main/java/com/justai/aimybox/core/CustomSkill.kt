package com.justai.aimybox.core

import com.justai.aimybox.Aimybox
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response

/**
 * Interface for custom client-side skill.
 * To enable it in Aimybox, add an instance of the skill to [Config.skills].
 * */
interface CustomSkill {
    /**
     * Determines whether the current skill can handle the [response].
     * If no skill matches, the default handler will be called.
     * */
    fun canHandle(response: Response): Boolean = false

    /**
     * This method will be called just before any request to dialog api.
     * You can change [Request.data] to add any additional data to the request.
     * */
    suspend fun onRequest(request: Request) {}

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
    suspend fun onResponse(response: Response, aimybox: Aimybox) {}
}