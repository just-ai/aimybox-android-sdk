package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject
import com.justai.aimybox.model.JsonResponse
import com.justai.aimybox.model.reply.Reply

class AimyboxResponse(
    json: JsonObject,
    replies: List<Reply>
) : JsonResponse(json, replies = replies)