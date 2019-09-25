package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply

internal data class AimyboxResponse(
    override val query: String?,
    override val text: String?,
    override val action: String?,
    override val intent: String?,
    override val question: Boolean?,
    override val replies: List<Reply>,
    override val data: JsonObject?,
    override val json: JsonObject
) : Response