package com.justai.aimybox.api.aimybox

import android.net.Uri
import com.github.salomonbrys.kotson.nullBool
import com.github.salomonbrys.kotson.nullObj
import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.L
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
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
    private val replyTypes: Map<String, Class<out AimyboxReply>> = DEFAULT_REPLY_TYPES
) : DialogApi {

    companion object {
        private const val DEFAULT_API_URL = "https://api.aimybox.com/"
        val DEFAULT_REPLY_TYPES = mapOf(
            "text" to AimyboxTextReply::class.java,
            "image" to AimyboxImageReply::class.java,
            "buttons" to AimyboxButtonsReply::class.java
        )
    }

    private val baseUrl: String
    private val path: String

    init {
        val uri = Uri.parse(url)
        baseUrl = uri.scheme?.let { "$it://" }.orEmpty() + uri.authority
        path = uri.path ?: "/"
    }

    private val retrofit = AimyboxRetrofit(baseUrl, path)

    override suspend fun send(request: Request): Response {
        val apiRequest = AimyboxRequest(request.query, apiKey, unitId, request.data)
        val apiResponse = retrofit.requestAsync(apiRequest)
        return parseResponse(apiResponse, replyTypes)
    }

    private fun parseResponse(
        json: JsonObject,
        replyTypes: Map<String, Class<out AimyboxReply>>
    ) = AimyboxResponse(
        json["query"].nullString,
        json["text"].nullString,
        json["action"].nullString,
        json["intent"].nullString,
        json["question"].nullBool,
        json["replies"].parseReplies(replyTypes),
        json["data"].nullObj,
        json
    )

    private fun JsonElement?.parseReplies(replyTypes: Map<String, Class<out AimyboxReply>>) =
        this?.takeIf(JsonElement::isJsonArray)
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