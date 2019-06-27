package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

internal data class AimyboxRequest(
    @SerializedName("query")
    val query: String,
    @SerializedName("key")
    val apiKey: String,
    @SerializedName("unit")
    val unitId: String,
    @SerializedName("data")
    val data: JsonObject
)