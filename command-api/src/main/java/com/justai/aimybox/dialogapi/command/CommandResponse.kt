package com.justai.aimybox.dialogapi.command

import androidx.annotation.RawRes
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply

class CommandResponse(
    override val query: String?,
    @RawRes val commandId: Int
): Response {

    override val action: String? = null

    override val intent: String? = null

    override val question: Boolean? = false

    override val replies: List<Reply> = emptyList()
}