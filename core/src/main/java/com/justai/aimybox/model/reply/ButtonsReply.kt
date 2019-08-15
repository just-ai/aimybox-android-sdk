package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.array
import com.github.salomonbrys.kotson.nullString
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
    val buttons: List<String>
) : Reply {

    companion object {
        const val TYPE = "buttons"
    }

    object Parser : Reply.Parser<ButtonsReply> {
        override fun parse(json: JsonObject) = if (json["type"].nullString == TYPE) {
            val buttons = json["buttons"].array.map(JsonElement::string)
            ButtonsReply(json, buttons)
        } else {
            null
        }
    }

}   