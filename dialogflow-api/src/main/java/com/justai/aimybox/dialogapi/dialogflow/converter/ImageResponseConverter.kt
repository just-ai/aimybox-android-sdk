package com.justai.aimybox.dialogapi.dialogflow.converter

import com.google.cloud.dialogflow.v2.Intent
import com.justai.aimybox.model.reply.ImageReply

object ImageResponseConverter:
    ResponseConverter<Intent.Message.Image, ImageReply> {

    override fun convert(msg: Intent.Message.Image) = ImageReply(msg.imageUri)
}