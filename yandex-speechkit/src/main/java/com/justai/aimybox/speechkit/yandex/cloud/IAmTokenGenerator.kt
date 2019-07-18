package com.justai.aimybox.speechkit.yandex.cloud

import com.google.gson.annotations.SerializedName
import com.squareup.okhttp.Callback
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException
import java.util.*

object IAmTokenGenerator {
    private val client = OkHttpClient()

    suspend fun getOAuthToken(yandexPassportOAuthToken: String): String {
        val request = Request.Builder().apply {
            url(BuildConfig.TOKEN_API_URL)
            addHeader("Content-Type", "application/json")
            val body = RequestBody.create(
                MediaType.parse("applicaton/json; charset=utf-8"),
                "{\"yandexPassportOauthToken\": \"$yandexPassportOAuthToken\"}"
            )
            post(body)
        }.build()

        val deferred = CompletableDeferred<IAmToken>()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(request: Request, e: IOException) {
                L.e("Failed to receive OAuth token", e)
                deferred.completeExceptionally(e)
            }

            override fun onResponse(response: Response) {
                val body = response.body().string()
                try {
                    val token = gson.fromJson(body, IAmToken::class.java)
                    deferred.complete(token)
                } catch (e: Throwable) {
                    deferred.completeExceptionally(IOException("Failed to parse token from response: $body", e))
                }
            }
        })

        return deferred.await().token
    }

    private data class IAmToken(
        @SerializedName("iamToken")
        val token: String,
        @SerializedName("expiresAt")
        val expiresAt: Date
    )
}
