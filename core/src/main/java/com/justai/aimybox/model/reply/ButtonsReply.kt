package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Designed to create clickable buttons with which the user can quickly respond without using STT.
 * */
data class ButtonsReply(
    override val sourceJson: JsonObject,
    /**
     * A list of buttons to display.
     * */
    val buttons: List<String> = sourceJson["buttons"].array.map(JsonElement::string)
) : Reply {
    companion object {
        const val TYPE = "buttons"
    }

    override val type = ImageReply.TYPE
}