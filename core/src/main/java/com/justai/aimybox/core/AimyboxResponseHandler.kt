package com.justai.aimybox.core

import androidx.annotation.RequiresPermission
import com.justai.aimybox.Aimybox
import com.justai.aimybox.extensions.className
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.TextReply
import com.justai.aimybox.model.reply.asTextSpeech
import kotlinx.coroutines.launch

internal class AimyboxResponseHandler(
    private val aimybox: Aimybox,
    private var skills: Collection<CustomSkill>
) : AimyboxComponent("Response Handling") {

    internal fun setSkills(newSkills: Collection<CustomSkill>) {
        if (newSkills == skills) {
            cancel()
            skills = newSkills
        }
    }

    @RequiresPermission("android.permission.RECORD_AUDIO")
    internal fun handle(response: Response) {
        skills.find { it.canHandle(response) }?.let { skill ->
            L.i("Suitable skill for action '${response.action}': ${skill.className}")
            launch { skill.onResponse(response, aimybox) }
        } ?: launch { handleDefault(response) }
    }

    @RequiresPermission("android.permission.RECORD_AUDIO")
    private fun handleDefault(response: Response) {
        if (response.action?.isNotBlank() == true) {
            L.i("No suitable sills for action '${response.action}'")
        }

        try {
            response.replies
                .filterIsInstance<TextReply>()
                .map(TextReply::asTextSpeech)
                .let { aimybox.speak(it, Aimybox.NextAction.byQuestion(response.question)) }
        } catch (e: Throwable) {
            L.e("Failed to parse replies from $response", e)
        }
    }

}