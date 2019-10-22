package com.justai.aimybox.dialogapi.rasa

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.justai.aimybox.model.Request

data class RasaRequest(
    @SerializedName("message")
    override val query: String,
    @SerializedName("sender")
    val sender: String,
    @SerializedName("metadata")
    val data: JsonObject? = JsonObject()
): Request