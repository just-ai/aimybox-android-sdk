package com.justai.aimybox.speechkit.google.cloud.model

enum class RecognitionModel(internal val stringValue: String) {
    COMMAND_AND_SEARCH("command_and_search"),
    PHONE_CALL("phone_call"),
    VIDEO("video"),
    DEFAULT("default")
}