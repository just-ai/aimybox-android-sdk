package com.justai.aimybox.api.aimybox

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal class AimyboxRetrofit(baseUrl: String, private val path: String) {
    private val retrofit = Retrofit.Builder()
        .client(createHttpClient())
        .baseUrl(baseUrl)
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .addConverterFactory(GsonConverterFactory.create(gsonInstance))
        .build()

    private val api = retrofit.create(AimyboxApi::class.java)

    suspend fun requestAsync(request: AimyboxRequest) =
        api.performRequestAsync(path, request).await()

    private fun createHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(loggingInterceptor)

        return builder.build()
    }
}