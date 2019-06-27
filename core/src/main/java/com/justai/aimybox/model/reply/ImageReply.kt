package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject

/**
 * Reply which contains an image URL.
 * */
data class ImageReply internal constructor(
    override val sourceJson: JsonObject,
    /**
     * Image url.
     * */
    val url: String = sourceJson["imageUlr"].string
) : Reply {
    companion object {
        const val TYPE = "image"
    }

    override val type = TYPE
}