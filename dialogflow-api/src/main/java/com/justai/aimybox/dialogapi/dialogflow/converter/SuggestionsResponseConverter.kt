package com.justai.aimybox.dialogapi.dialogflow.converter

import com.google.cloud.dialogflow.v2.Intent
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.ReplyButton

object SuggestionsResponseConverter {

    fun convert(msg: List<Intent.Message.Suggestion>) =
        ButtonsReply(msg.map { ReplyButton(it.title, null) })
}