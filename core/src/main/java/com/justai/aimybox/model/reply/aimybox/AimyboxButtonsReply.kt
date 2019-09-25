package com.justai.aimybox.model.reply.aimybox

import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.ReplyButton


data class AimyboxButtonsReply(override val json: JsonObject) : AimyboxReply, ButtonsReply {
    override val buttons by lazy {
        json["buttons"].array.map { AimyboxReplyButton(it.asJsonObject) }
    }
}

data class AimyboxReplyButton(val json: JsonObject) : ReplyButton {
    override val text by json.byString
    override val url by json.byNullableString
}