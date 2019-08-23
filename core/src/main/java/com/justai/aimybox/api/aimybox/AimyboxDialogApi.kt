package com.justai.aimybox.api.aimybox

import android.net.Uri
import android.os.Build
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.reply.ButtonsReply
import com.justai.aimybox.model.reply.ImageReply
import com.justai.aimybox.model.reply.Reply
import com.justai.aimybox.model.reply.TextReply

/**
 * Aimybox dialog api implementation.
 *
 * @param apiKey your project API key
 * @param unitId unique device identifier
 * @param url Aimybox dialog API URL
 * */
class AimyboxDialogApi(
    private val apiKey: String,
    private val unitId: String,
    url: String = DEFAULT_API_URL,
    private val replyTypes: Map<String, Class<out Reply>> = DEFAULT_REPLY_TYPES
) : DialogApi {

    companion object {
        private const val DEFAULT_API_URL = "https://api.aimybox.com/"
        val DEFAULT_REPLY_TYPES = mapOf(
            "text" to TextReply::class.java,
            "image" to ImageReply::class.java,
            "buttons" to ButtonsReply::class.java
        )
    }

    private val baseUrl: String
    private val path: String

    init {
        val uri = Uri.parse(url)
        baseUrl = uri.scheme?.let { "$it://" }.orEmpty() + uri.authority
        path = uri.path ?: "/"
    }

    private val httpWorker = getHttpWorker(baseUrl, path)

    override suspend fun send(request: Request): Response? {
        val apiRequest = AimyboxRequest(request.query, apiKey, unitId, request.data)
        val apiResponse = httpWorker.requestAsync(apiRequest)
        val response = AimyboxResponse.fromJson(apiResponse, replyTypes)
        return response.run { Response(query, text, action, intent, question, replies, source) }
    }

    override fun destroy() {
        //Do nothing
    }

    @Suppress("DEPRECATION")
    private fun getHttpWorker(baseUrl: String, path: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RetrofitHttpWorker(baseUrl, path)
        } else {
            LegacyHttpWorker(baseUrl + path)
        }

}