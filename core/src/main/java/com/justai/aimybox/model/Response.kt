package com.justai.aimybox.model

import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.reply.Reply

/**
 * Response model, which is used across the library.
 * You can parse additional data from [json] in your [CustomSkill].
 * */
open class Response(
    val query: String? = null,
    val action: String? = null,
    val intent: String? = null,
    val question: Boolean? = null,
    val replies: List<Reply> = emptyList(),
    val data: Any? = null
)
