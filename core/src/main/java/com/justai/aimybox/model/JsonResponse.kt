package com.justai.aimybox.model

import com.github.salomonbrys.kotson.nullBool
import com.github.salomonbrys.kotson.nullObj
import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.Reply

open class JsonResponse(
    val json: JsonObject,
    override val query: String? = json.get("query").nullString,
    override val action: String? = json.get("action").nullString,
    override val intent: String? = json.get("intent").nullString,
    override val question: Boolean? = json.get("question").nullBool,
    override val replies: List<Reply> = emptyList(),
    val data: JsonObject? = json.get("data").nullObj
) : Response