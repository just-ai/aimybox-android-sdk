package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonObject
import com.justai.aimybox.model.TextSpeech

/**
 * Represents text reply, which should be synthesized and/or displayed in the UI.
 * */
data class TextReply(
    override val sourceJson: JsonObject,
    /**
     * Text to show in the UI. Also, this text should be synthesized, if [tts] text is null.
     * */
    val text: String,
    /**
     * Text to be synthesized. Can include SSML and other markup, and therefore it should not be displayed in UI.
     * */
    val tts: String?,
    /**
     * The language of the reply.
     * */
    val language: String?
) : Reply {
    companion object {
        const val TYPE = "text"
    }

    object Parser : Reply.Parser<TextReply> {
        override fun parse(json: JsonObject): TextReply? {
            return if (json["type"].nullString == TYPE) {
                json["text"].nullString?.let { text ->
                    val tts = json["tts"].nullString
                    val language = json["lang"].nullString
                    TextReply(json, text, tts, language)
                }
            } else {
                null
            }
        }
    }
}

fun TextReply.asTextSpeech() = TextSpeech(tts ?: text, language)