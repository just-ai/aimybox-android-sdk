package com.justai.aimybox.model

import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.reply.Reply

/**
 * Response model, which is used across the library.
 * You can parse additional data from [json] in your [CustomSkill].
 * */

//TODO docs
interface Response {
    val query: String?
    val action: String?
    val intent: String?
    val question: Boolean?
    val replies: List<Reply>
}
