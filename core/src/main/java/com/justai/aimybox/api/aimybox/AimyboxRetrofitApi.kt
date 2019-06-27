package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject
import kotlinx.coroutines.Deferred
import retrofit2.http.Body
import retrofit2.http.POST

internal interface AimyboxRetrofitApi {
    @POST("/api/request")
    fun performRequestAsync(@Body request: AimyboxRequest) : Deferred<JsonObject>
}