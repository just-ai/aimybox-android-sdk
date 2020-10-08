package com.justai.aimybox.api.aimybox

import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.aimybox.*

object AimyboxUtils {

    val DEFAULT_REPLY_TYPES = mapOf(
        "text" to AimyboxTextReply::class.java,
        "audio" to AimyboxAudioReply::class.java,
        "image" to AimyboxImageReply::class.java,
        "buttons" to AimyboxButtonsReply::class.java
    )

    fun parseReplies(json: JsonObject, replyTypes: Map<String, Class<out AimyboxReply>> = DEFAULT_REPLY_TYPES) =
        json.get("replies")?.takeIf(JsonElement::isJsonArray)
            ?.asJsonArray
            ?.filterIsInstance(JsonObject::class.java)
            ?.map { resolveReplyType(it, replyTypes) }
            .orEmpty()

    private fun resolveReplyType(
        json: JsonObject,
        replyTypes: Map<String, Class<out AimyboxReply>>
    ): AimyboxReply {
        val type = json["type"].nullString
        val replyClass = replyTypes[type] ?: UnknownAimyboxReply::class.java
        require(replyClass.constructors.size == 1) {
            "AimyboxReply must have strictly one constructor. See AimyboxReply interface documentation."
        }
        return replyClass.constructors.first().newInstance(json) as AimyboxReply
    }
}