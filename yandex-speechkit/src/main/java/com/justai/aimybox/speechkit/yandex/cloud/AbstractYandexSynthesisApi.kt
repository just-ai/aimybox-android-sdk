package com.justai.aimybox.speechkit.yandex.cloud

interface AbstractYandexSynthesisApi {
    suspend fun request(
        text: String,
        language: Language
    ): ByteArray
}