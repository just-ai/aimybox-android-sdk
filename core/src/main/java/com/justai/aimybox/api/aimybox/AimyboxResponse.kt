package com.justai.aimybox.api.aimybox

import com.github.salomonbrys.kotson.nullBool
import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.justai.aimybox.core.L
import com.justai.aimybox.model.reply.Reply
import com.justai.aimybox.model.reply.UnknownReply

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

        fun fromJson(json: JsonObject, replyTypes: Map<String, Class<out Reply>>) = AimyboxResponse(
            json["query"].nullString,
            json["text"].nullString,
            json["action"].nullString,
            json["intent"].nullString,
            json["question"].nullBool,
            json["replies"]
                ?.takeIf(JsonElement::isJsonArray)
                ?.asJsonArray
                ?.filterIsInstance(JsonObject::class.java)
                ?.map { resolveReplyType(it, replyTypes) }
                .orEmpty(),
            json
        )

        private fun resolveReplyType(json: JsonObject, replyTypes: Map<String, Class<out Reply>>): Reply {
            val type = json["type"].nullString
            val replyClass = replyTypes[type] ?: UnknownReply::class.java
            if (replyClass == UnknownReply::class.java) {
                L.w("Reply with type \"$type\" is unresolved. Register reply class in AimyboxDialogApi constructor.")
            }
            require(replyClass.constructors.size == 1) {
                "Reply must have strictly one constructor. See Reply interface documentation."
            }
            return replyClass.constructors.first().newInstance(json) as Reply
        }
    }
}