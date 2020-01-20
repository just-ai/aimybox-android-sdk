package com.justai.aimybox.dialogapi.command

import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply

class CommandResponse(
    override val query: String?,
    override val action: String?,
    override val intent: String?
): Response {

    override val question: Boolean? = false

    override val replies: List<Reply> = emptyList()
}