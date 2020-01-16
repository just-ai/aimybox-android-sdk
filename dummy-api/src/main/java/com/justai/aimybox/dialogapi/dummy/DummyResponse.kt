package com.justai.aimybox.assistant.api

import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply
import com.justai.aimybox.model.reply.TextReply

class DummyResponse(
    override val query: String?
): Response {
    override val action: String? = null
    override val intent: String? = null
    override val question: Boolean? = false
    override val replies: List<Reply> = listOf(TextReply(query ?: "", null, null))
}