package com.justai.aimybox.model.reply.aimybox

import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.ReplyButton


data class AimyboxButtonsReply(
    override val json: JsonObject
) : AimyboxReply, ButtonsReply(json.parseButtons()) {
    companion object {
        private fun JsonObject.parseButtons() =
            get("buttons").array.map { AimyboxReplyButton(it.asJsonObject) }
    }
}

data class AimyboxReplyButton(val json: JsonObject) : ReplyButton(
    json["text"].string,
    json["url"].nullString
)