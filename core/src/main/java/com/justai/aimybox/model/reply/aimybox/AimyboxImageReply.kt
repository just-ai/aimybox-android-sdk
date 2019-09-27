package com.justai.aimybox.model.reply.aimybox

import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.ImageReply

/**
 * Reply which contains an image URL.
 * */
data class AimyboxImageReply(
    override val json: JsonObject
) : ImageReply(json["imageUrl"].string), AimyboxReply