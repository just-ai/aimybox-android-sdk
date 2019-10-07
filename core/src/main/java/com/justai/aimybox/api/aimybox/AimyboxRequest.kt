package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.justai.aimybox.model.Request

data class AimyboxRequest(
    @SerializedName("query")
    override val query: String,
    @SerializedName("key")
    val apiKey: String,
    @SerializedName("unit")
    val unitId: String,
    @SerializedName("data")
    val data: JsonObject? = JsonObject()
) : Request