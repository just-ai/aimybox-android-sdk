package com.justai.aimybox.speechkit.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit

internal class JASKServiceFactory(baseUrl: String, okHttpClient: OkHttpClient) {

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .build()

    fun <S> createService(serviceClass: Class<S>): S {
        return retrofit.create(serviceClass)
    }
}