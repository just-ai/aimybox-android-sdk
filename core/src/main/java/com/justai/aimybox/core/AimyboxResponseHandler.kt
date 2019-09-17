package com.justai.aimybox.core

import androidx.annotation.RequiresPermission
import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.extensions.className
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.TextReply
import com.justai.aimybox.model.reply.asTextSpeech
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

internal class AimyboxResponseHandler(
    private val aimybox: Aimybox,
    private val events: SendChannel<DialogApi.Event>,
    private var skills: Collection<CustomSkill>
) : AimyboxComponent("Response Handling") {

    internal fun setSkills(newSkills: Collection<CustomSkill>) {
        if (newSkills != skills) {
            cancel()
            skills = newSkills
        }
    }

    @RequiresPermission("android.permission.RECORD_AUDIO")
    internal fun handle(response: Response) = launch {
        val skill = skills.find { it.canHandle(response) }
        if (skill != null) {
            L.i("Found skill \"${skill.className}\" for action \"${response.action}\". ")
            skill.onResponse(response, aimybox, ::handleDefault)
        } else {
            if (!response.action.isNullOrBlank()) {
                L.i("No suitable skills for action \"${response.action}\". Handling by default...")
            }
            handleDefault(response)
        }
    }

    @RequiresPermission("android.permission.RECORD_AUDIO")
    private suspend fun handleDefault(response: Response): Unit = try {
        L.d("Begin handling")
        val lastTextReply = response.replies.lastOrNull { it is TextReply }

        response.replies.forEach { reply ->
            L.d("Reply $reply")
            events.send(DialogApi.Event.NextReply(reply))
            if (reply is TextReply) {
                val nextAction = if (reply == lastTextReply) {
                    Aimybox.NextAction.byQuestion(response.question)
                } else {
                    Aimybox.NextAction.STANDBY
                }
                aimybox.speak(reply.asTextSpeech(), nextAction).join()
            }
        }
    } catch (e: Throwable) {
        L.e("Failed to parse replies from $response", e)
    }
}