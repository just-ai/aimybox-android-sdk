package com.justai.aimybox.speechkit.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


internal interface TTSService {
    @FormUrlEncoded
    @POST("synthesize")
    suspend fun createTask(@Header("api-key") apiKey : String,
            @Field("text") text: String) : ResponseBody?
}