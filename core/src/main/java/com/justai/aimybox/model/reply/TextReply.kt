package com.justai.aimybox.model.reply

import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.google.gson.JsonObject
import com.justai.aimybox.model.TextSpeech

/**
 * Represents text reply, which should be synthesized and/or displayed in the UI.
 * */
data class TextReply(override val json: JsonObject) : Reply {

    /**
     * Text to show in the UI. Also, this text should be synthesized, if [tts] text is null.
     * */
    val text by json.byString
    /**
     * Text to be synthesized. Can include SSML and other markup, and therefore it should not be displayed in UI.
     * */
    val tts by json.byNullableString
    /**
     * The language of the reply.
     * */
    val language by json.byNullableString
}

fun TextReply.asTextSpeech() = TextSpeech(tts ?: text, language)