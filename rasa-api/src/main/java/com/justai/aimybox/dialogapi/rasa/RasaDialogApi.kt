package com.justai.aimybox.dialogapi.rasa

import com.github.salomonbrys.kotson.nullString
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.reply.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.LinkedHashSet

/**
 * Rasa.ai dialog API implementation
 *
 * @param sender a unique user's identifier
 * @param url Rasa's REST webhook URL
 */
class RasaDialogApi(
    private val sender: String,
    private val url: String,
    override val customSkills: LinkedHashSet<CustomSkill<RasaRequest, RasaResponse>> = linkedSetOf()
): DialogApi<RasaRequest, RasaResponse>() {

    private val httpClient: OkHttpClient
    private val gson = Gson()

    init {
        val builder = OkHttpClient.Builder()

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(loggingInterceptor)

        httpClient = builder.build()
    }

    override fun createRequest(query: String) =
        RasaRequest(query, sender)

    override suspend fun send(request: RasaRequest): RasaResponse {
        val req = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(request).toRequestBody())
            .build()

        val res = httpClient.newCall(req).execute()
        val array = JsonParser().parse(res.body?.string()).asJsonArray
        return RasaResponse(
            request.query,
            replies = parseReplies(array)
        )
    }

    private fun parseReplies(res: JsonArray): List<Reply> {
        val replies = ArrayList<Reply>()
        res.map { it.asJsonObject }.forEach { json ->
            if (json.has("text"))
                replies.add(TextReply(json["text"].asString, null))

            if (json.has("image"))
                replies.add(ImageReply(json["image"].asString))

            if (json.has("buttons"))
                replies.add(ButtonsReply(
                    json["buttons"].asJsonArray.map { it.asJsonObject }.map { btn ->
                        ReplyButton(btn["title"].asString, null, btn["payload"].nullString)
                    }
                ))
        }

        return replies
    }
}