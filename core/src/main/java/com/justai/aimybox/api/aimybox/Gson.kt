package com.justai.aimybox.api.aimybox

import com.google.gson.Gson

internal val gsonInstance = Gson()
internal fun Any.toJson() = gsonInstance.toJson(this)!!