package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.google.gson.JsonObject

/**
 * Designed to create clickable buttons with which the user can quickly respond without using STT.
 * */
data class ButtonsReply(override val json: JsonObject) : Reply {
    val buttons by lazy { json.array.map { ReplyButton(it.asJsonObject) } }
}

data class ReplyButton(val json: JsonObject) {
    val text by json.byString
    val tts by json.byNullableString
}