package com.justai.aimybox.model.reply

import com.google.gson.JsonObject

/**
 * Represents an unresolved reply type.
 * */
data class UnknownReply(override val json: JsonObject) : Reply