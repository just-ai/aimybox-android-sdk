package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.Reply

data class UiEvent(
    val action: String,
    val replies: List<Reply> = emptyList(),
    val data: JsonObject? = null
)