package com.justai.aimybox.api.aimybox

import android.os.Build
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response

/**
 * Aimybox dialog api implementation.
 *
 * @param apiKey your project API key
 * @param unitId unique device identifier
 * @param url Aimybox dialog API URL
 * */
class AimyboxDialogApi(
    var apiKey: String,
    private val unitId: String,
    url: String = "https://api.aimybox.com/"
) : DialogApi {

    private val httpWorker = getHttpWorker(url)

    override suspend fun send(request: Request): Response? {
        val apiRequest = AimyboxRequest(request.query, apiKey, unitId, request.data)
        val apiResponse = AimyboxResponse.fromJson(httpWorker.requestAsync(apiRequest))
        return apiResponse.run { Response(query, text, action, intent, question, replies, source) }
    }

    override fun destroy() {
        //Do nothing
    }

    @Suppress("DEPRECATION")
    private fun getHttpWorker(apiUrl: String) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        RetrofitHttpWorker(apiUrl)
    } else {
        LegacyHttpWorker(apiUrl)
    }

}