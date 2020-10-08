package com.justai.aimybox.api.aimybox

import android.net.Uri
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.reply.aimybox.*

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
    override val customSkills: LinkedHashSet<CustomSkill<AimyboxRequest, AimyboxResponse>> = linkedSetOf(),
    private val replyTypes: Map<String, Class<out AimyboxReply>> = AimyboxUtils.DEFAULT_REPLY_TYPES
) : DialogApi<AimyboxRequest, AimyboxResponse>() {

    companion object {
        private const val DEFAULT_API_URL = "https://api.aimybox.com/"
    }

    private val retrofit: AimyboxRetrofit

    init {
        val uri = Uri.parse(url)
        val baseUrl = uri.scheme?.let { "$it://" }.orEmpty() + uri.authority
        val path = uri.path ?: "/"
        retrofit = AimyboxRetrofit(baseUrl, path)
    }

    override fun createRequest(query: String) = AimyboxRequest(query, apiKey, unitId)

    suspend fun resetSession() = send(createRequest("/reset"))

    override suspend fun send(request: AimyboxRequest): AimyboxResponse {
        val json = retrofit.request(request)
        return AimyboxResponse(json, AimyboxUtils.parseReplies(json, replyTypes))
    }
}