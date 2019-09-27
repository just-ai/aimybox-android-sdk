package com.justai.aimybox.model.reply

/**
 * Designed to create clickable buttons with which the user can quickly respond without using STT.
 * */
open class ButtonsReply(val buttons: List<ReplyButton>) : Reply

open class ReplyButton(val text: String, val url: String?)
