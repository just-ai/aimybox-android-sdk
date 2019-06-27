package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.nullString
import com.github.salomonbrys.kotson.string
import com.google.gson.JsonObject
import com.justai.aimybox.model.TextSpeech

/**
 * Represents text reply, which should be synthesized and/or displayed in the UI.
 * */
data class TextReply internal constructor(override val sourceJson: JsonObject) :
    Reply {
    companion object {
        const val TYPE = "text"
    }

    override val type: String = TYPE

    /**
     * Text to show in the UI. Also, this text should be synthesized, if [tts] text is null.
     * */
    val text: String = sourceJson["text"].string

    /**
     * Text to be synthesized. Can include SSML and other markup, and therefore it should not be displayed in UI.
     * */
    val tts: String? = sourceJson["tts"].nullString

    /**
     * The language of the reply.
     * */
    val language: String? = sourceJson["lang"].nullString
}

fun TextReply.asTextSpeech() = TextSpeech(tts ?: text, language)