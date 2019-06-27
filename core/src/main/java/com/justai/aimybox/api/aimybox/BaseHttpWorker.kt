package com.justai.aimybox.api.aimybox

import com.google.gson.JsonObject

internal abstract class BaseHttpWorker {
    abstract suspend fun requestAsync(request: AimyboxRequest): JsonObject
}

