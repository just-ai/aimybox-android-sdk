package com.justai.aimybox.model

import com.google.gson.JsonObject
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.CustomSkill

/**
 * Request model, which is used across the library.
 * You can extend it by adding some fields to [data] JSON in [CustomSkill] or custom [DialogApi].
 * */
data class Request(
    /**
     * User input, recognized by STT or manually entered.
     * */
    val query: String,
    val data: JsonObject = JsonObject()
)