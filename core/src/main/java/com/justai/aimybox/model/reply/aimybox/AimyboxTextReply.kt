package com.justai.aimybox.model.reply.aimybox

import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.TextReply

data class AimyboxTextReply(override val json: JsonObject) : TextReply(
    json["text"].string,
    json["tts"].nullString,
    json["language"].nullString
), AimyboxReply