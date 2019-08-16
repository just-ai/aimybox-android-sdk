package com.justai.aimybox.api.aimybox

import com.github.salomonbrys.kotson.nullBool
import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.justai.aimybox.core.L
import com.justai.aimybox.model.reply.Reply

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

        fun fromJson(json: JsonObject, parsers: Set<Reply.Parser<*>>) = AimyboxResponse(
            json["query"].nullString,
            json["text"].nullString,
            json["action"].nullString,
            json["intent"].nullString,
            json["question"].nullBool,
            json["replies"]
                ?.takeIf(JsonElement::isJsonArray)
                ?.asJsonArray
                ?.filterIsInstance(JsonObject::class.java)
                ?.mapNotNull { parseReply(it, parsers) }
                .orEmpty(),
            json
        )

        private fun parseReply(json: JsonObject, parsers: Set<Reply.Parser<*>>): Reply? {
            parsers.forEach { parser ->
                parser.parse(json)?.let { parsedReply ->
                    return parsedReply
                }
            }
            L.w("No parsers found for reply: $json")
            return null
        }

    }
}