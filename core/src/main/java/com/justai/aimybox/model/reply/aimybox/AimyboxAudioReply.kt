package com.justai.aimybox.model.reply.aimybox

import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.AudioReply

/**
 * Reply which contains an audio URL.
 */
data class AimyboxAudioReply(
    override val json: JsonObject
): AudioReply(json["audioUrl"].string), AimyboxReply