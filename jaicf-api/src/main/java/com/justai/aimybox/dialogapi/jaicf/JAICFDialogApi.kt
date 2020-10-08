package com.justai.aimybox.dialogapi.jaicf

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.api.aimybox.AimyboxRequest
import com.justai.aimybox.api.aimybox.AimyboxResponse
import com.justai.aimybox.api.aimybox.AimyboxUtils
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.reply.aimybox.AimyboxReply
import com.justai.jaicf.BotEngine
import com.justai.jaicf.activator.regex.RegexActivator
import com.justai.jaicf.channel.aimybox.AimyboxChannel
import com.justai.jaicf.channel.http.HttpBotRequest
import com.justai.jaicf.model.scenario.ScenarioModel

class JAICFDialogApi(
    private val unitId: String,
    engine: BotEngine,
    override val customSkills: LinkedHashSet<CustomSkill<AimyboxRequest, AimyboxResponse>> = linkedSetOf(),
    private val replyTypes: Map<String, Class<out AimyboxReply>> = AimyboxUtils.DEFAULT_REPLY_TYPES
): DialogApi<AimyboxRequest, AimyboxResponse>() {

    private val channel = AimyboxChannel(engine)

    private val gson = Gson()
    private val parser = JsonParser()

    constructor(
        unitId: String,
        model: ScenarioModel,
        customSkills: LinkedHashSet<CustomSkill<AimyboxRequest, AimyboxResponse>> = linkedSetOf(),
        replyTypes: Map<String, Class<out AimyboxReply>> = AimyboxUtils.DEFAULT_REPLY_TYPES
    ): this(
        unitId,
        BotEngine(model, activators = arrayOf(RegexActivator)),
        customSkills,
        replyTypes
    )

    override fun createRequest(query: String) = AimyboxRequest(query, "", unitId)

    suspend fun resetSession() = send(createRequest("/reset"))

    override suspend fun send(request: AimyboxRequest): AimyboxResponse {
        val response = channel.process(HttpBotRequest(gson.toJson(request).byteInputStream()))
        val json = parser.parse(response!!.output.toString("UTF-8")).asJsonObject
        return AimyboxResponse(json, AimyboxUtils.parseReplies(json, replyTypes))
    }

}