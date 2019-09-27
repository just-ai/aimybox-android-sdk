package com.justai.aimybox.model

import com.github.salomonbrys.kotson.nullBool
import com.github.salomonbrys.kotson.nullObj
import com.github.salomonbrys.kotson.nullString
import com.google.gson.JsonObject
import com.justai.aimybox.model.reply.Reply

class JsonResponse(
    val json: JsonObject,
    query: String? = json.get("query").nullString,
    action: String? = json.get("action").nullString,
    intent: String? = json.get("intent").nullString,
    question: Boolean? = json.get("question").nullBool,
    replies: List<Reply> = emptyList(),
    data: JsonObject? = json.get("data").nullObj
) : Response(query, action, intent, question, replies, data)