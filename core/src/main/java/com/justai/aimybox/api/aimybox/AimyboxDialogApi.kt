package com.justai.aimybox.api.aimybox

import android.net.Uri
import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
    private val replyTypes: Map<String, Class<out AimyboxReply>> = DEFAULT_REPLY_TYPES
) : DialogApi<AimyboxRequest, AimyboxResponse>() {

    companion object {
        private const val DEFAULT_API_URL = "https://api.aimybox.com/"
        val DEFAULT_REPLY_TYPES = mapOf(
            "text" to AimyboxTextReply::class.java,
            "audio" to AimyboxAudioReply::class.java,
            "image" to AimyboxImageReply::class.java,
            "buttons" to AimyboxButtonsReply::class.java
        )
    }

    private val retrofit: AimyboxRetrofit

    init {
        val uri = Uri.parse(url)
        val baseUrl = uri.scheme?.let { "$it://" }.orEmpty() + uri.authority
        val path = uri.path ?: "/"
        retrofit = AimyboxRetrofit(baseUrl, path)
    }

    override fun createRequest(query: String) = AimyboxRequest(query, apiKey, unitId)

    override suspend fun send(request: AimyboxRequest): AimyboxResponse {
        val json = retrofit.request(request)
        return AimyboxResponse(json, parseReplies(json, replyTypes))
    }

    private fun parseReplies(json: JsonObject, replyTypes: Map<String, Class<out AimyboxReply>>) =
        json.get("replies")?.takeIf(JsonElement::isJsonArray)
            ?.asJsonArray
            ?.filterIsInstance(JsonObject::class.java)
            ?.map { resolveReplyType(it, replyTypes) }
            .orEmpty()

    private fun resolveReplyType(
        json: JsonObject,
        replyTypes: Map<String, Class<out AimyboxReply>>
    ): AimyboxReply {
        val type = json["type"].nullString
        val replyClass = replyTypes[type] ?: UnknownAimyboxReply::class.java
        if (replyClass == UnknownAimyboxReply::class.java) L.w(
            """AimyboxReply with type "$type" is unresolved. 
                |register reply class in AimyboxDialogApi constructor.""".trimMargin()
        )
        require(replyClass.constructors.size == 1) {
            "AimyboxReply must have strictly one constructor. See AimyboxReply interface documentation."
        }
        return replyClass.constructors.first().newInstance(json) as AimyboxReply
    }
}