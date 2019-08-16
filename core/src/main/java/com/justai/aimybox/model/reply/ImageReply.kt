package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.byString
import com.google.gson.JsonObject

/**
 * Reply which contains an image URL.
 * */
data class ImageReply(override val json: JsonObject) : Reply {
    /**
     * Image url.
     * */
    val url by json.byString("imageUlr")
}