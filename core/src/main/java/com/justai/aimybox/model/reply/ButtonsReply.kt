package com.justai.aimybox.model.reply

/**
 * Designed to create clickable buttons with which the user can quickly respond without using STT.
 * */
interface ButtonsReply : Reply {
    val buttons: List<ReplyButton>
}

interface ReplyButton {
    val text: String
    val url: String?
}