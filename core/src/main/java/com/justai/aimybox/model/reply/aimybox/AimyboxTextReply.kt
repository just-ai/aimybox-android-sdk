package com.justai.aimybox.model.reply.aimybox

import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.TextReply

data class AimyboxTextReply(override val json: JsonObject) : AimyboxReply, TextReply {
    override val text by json.byString
    override val tts by json.byNullableString
    override val language by json.byNullableString
}