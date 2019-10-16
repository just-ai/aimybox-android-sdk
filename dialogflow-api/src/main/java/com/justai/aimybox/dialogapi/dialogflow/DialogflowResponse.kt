package com.justai.aimybox.dialogapi.dialogflow

import com.google.protobuf.Struct
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.Reply

data class DialogflowResponse(
    override val query: String?,
    override val action: String?,
    override val intent: String?,
    override val question: Boolean?,
    override val replies: List<Reply>,
    val parameters: Struct
) : Response