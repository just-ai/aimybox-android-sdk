package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply

internal data class AimyboxResponse(
    override val query: String?,
    override val text: String?,
    override val action: String? = null,
    override val intent: String? = null,
    override val question: Boolean? = null,
    override val replies: List<Reply> = emptyList(),
    override val data: JsonObject? = null,
    override val json: JsonObject
) : Response