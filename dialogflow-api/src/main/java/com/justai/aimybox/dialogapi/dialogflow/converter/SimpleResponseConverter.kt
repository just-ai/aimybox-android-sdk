package com.justai.aimybox.dialogapi.dialogflow.converter

import com.google.cloud.dialogflow.v2.Intent
import com.justai.aimybox.model.reply.TextReply

object SimpleResponseConverter:
    ResponseConverter<Intent.Message.SimpleResponse, TextReply> {

    override fun convert(msg: Intent.Message.SimpleResponse) =
        TextReply(
            text = msg.displayText
                ?.takeIf { it.isNotEmpty() }
                ?: msg.textToSpeech,
            tts = msg.textToSpeech,
            language = null
        )
}