package com.justai.aimybox.model.reply.aimybox

import com.google.gson.JsonObject

/**
 * Represents an unresolved reply type.
 * */
data class UnknownAimyboxReply(override val json: JsonObject) : AimyboxReply