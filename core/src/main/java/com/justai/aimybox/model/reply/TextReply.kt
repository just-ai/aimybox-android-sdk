package com.justai.aimybox.model.reply

import com.justai.aimybox.model.TextSpeech

/**
 * Represents text reply, which should be synthesized and/or displayed in the UI.
 * */
open class TextReply(
    /**
     * Text to be synthesized. Can include SSML and other markup, and therefore it should not be displayed in UI.
     * */
    val tts: String? = null,
    /**
     * Text to show in the UI. Also, this text should be synthesized, if [tts] text is null.
     * */
    val text: String = "",
    /**
     * The language of the reply.
     * */
    val language: String? = null
) : Reply

@Suppress("SENSELESS_COMPARISON")
fun TextReply.asTextSpeech(): TextSpeech {
    val txt = tts ?: text
    return TextSpeech(if (txt != null) txt else "", language)
}

