package com.justai.aimybox.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.reply.Reply

/**
 * Response model, which is used across the library.
 * You can parse additional data from [source] JSON in your [CustomSkill].
 * */
data class Response(
    val query: String? = null,
    val text: String? = null,
    val action: String? = null,
    val intent: String? = null,
    val question: Boolean? = null,
    val replies: List<Reply> = emptyList(),
    @SerializedName("data")
    val data: JsonObject? = null,
    val source: JsonObject = JsonObject()
)