package com.justai.aimybox.dialogapi.dialogflow.converter

import com.google.cloud.dialogflow.v2.Intent
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.Reply
import com.justai.aimybox.model.reply.ReplyButton
import com.justai.aimybox.model.reply.TextReply

object BasicCardResponseConverter {

    fun convert(msg: Intent.Message.BasicCard): List<Reply> {
        val replies = ArrayList<Reply>()

        if (msg.hasImage()) {
            replies.add(ImageResponseConverter.convert(msg.image))
        }

        replies.add(TextReply(msg.formattedText, null))

        if (msg.buttonsList.isNotEmpty()) {
            replies.add(ButtonsReply(msg.buttonsList.map {
                ReplyButton(it.title, it.openUriAction.uri)
            }))
        }

        return replies
    }
}