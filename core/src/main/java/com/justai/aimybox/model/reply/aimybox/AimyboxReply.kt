package com.justai.aimybox.model.reply.aimybox

import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.Reply

/**
 * Reply is a unit of a composite response. One response contains from 0 to infinite replies.
 * Implementation of the interface must have strictly one constructor with single parameter
 * which is overrides [json].
 * */
interface AimyboxReply : Reply {
    val json: JsonObject
}