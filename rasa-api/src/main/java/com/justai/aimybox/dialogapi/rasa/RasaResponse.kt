package com.justai.aimybox.dialogapi.rasa

import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply

data class RasaResponse(
    override val query: String?,
    override val action: String? = null,
    override val intent: String? = null,
    override val question: Boolean? = true,
    override val replies: List<Reply>
): Response