package com.justai.aimybox.model.reply

/**
 * Reply which contains an image URL.
 * */
interface ImageReply : Reply {
    /**
     * Image url.
     * */
    val url: String
}