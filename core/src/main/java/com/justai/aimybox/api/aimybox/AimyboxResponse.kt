package com.justai.aimybox.api.aimybox

import com.github.salomonbrys.kotson.nullBool
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.ImageReply
import com.justai.aimybox.model.reply.Reply
import com.justai.aimybox.model.reply.TextReply

internal data class AimyboxResponse(
    val query: String?,
    val text: String?,
    val action: String?,
    val intent: String?,
    val question: Boolean?,
    val replies: List<Reply>,
    val source: JsonObject
) {
    companion object {
        fun fromJson(json: JsonObject) = AimyboxResponse(
            json["query"].nullString,
            json["text"].nullString,
            json["action"].nullString,
            json["intent"].nullString,
            json["question"].nullBool,
            json["replies"]
                ?.takeIf(JsonElement::isJsonArray)
                ?.asJsonArray
                ?.filterIsInstance(JsonObject::class.java)
                ?.map(::parseReply)
                .orEmpty(),
            json
        )

        private fun parseReply(sourceJson: JsonObject) = when (val type = sourceJson["type"].string) {
            TextReply.TYPE -> TextReply(sourceJson)
            ImageReply.TYPE -> ImageReply(sourceJson)
            ButtonsReply.TYPE -> ButtonsReply(sourceJson)
            else -> throw IllegalArgumentException("Unknown reply type $type")
        }
    }
}