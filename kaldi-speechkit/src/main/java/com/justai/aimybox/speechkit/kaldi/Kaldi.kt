package com.justai.aimybox.speechkit.kaldi

import org.json.JSONObject

fun String.parseResult(): String {
    return JSONObject(this).optString("text")
}

fun String.parsePartial(): String {
    return JSONObject(this).optString("partial")
}