package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject
import kotlinx.coroutines.Deferred
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

internal interface AimyboxApi {
    @POST("{path}")
    fun performRequestAsync(
        @Path("path", encoded = true) path: String,
        @Body request: AimyboxRequest
    ): Deferred<JsonObject>
}