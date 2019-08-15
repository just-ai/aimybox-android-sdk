package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject

/**
 * Reply which contains an image URL.
 * */
data class ImageReply(
    override val sourceJson: JsonObject,
    /**
     * Image url.
     * */
    val url: String = sourceJson["imageUlr"].string
) : Reply {

    companion object {
        const val TYPE = "image"
    }

    object Parser : Reply.Parser<ImageReply> {
        override fun parse(json: JsonObject) = if (json["type"].nullString == TYPE) {
            json["imageUlr"].nullString?.let { imageUrl ->
                ImageReply(json, imageUrl)
            }
        } else {
            null
        }
    }

}