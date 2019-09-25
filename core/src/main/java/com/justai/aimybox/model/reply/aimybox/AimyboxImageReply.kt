package com.justai.aimybox.model.reply.aimybox

import com.github.salomonbrys.kotson.byString
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.ImageReply

/**
 * Reply which contains an image URL.
 * */
data class AimyboxImageReply(override val json: JsonObject) : AimyboxReply, ImageReply {
    /**
     * Image url.
     * */
    override val url by json.byString("imageUrl")
}