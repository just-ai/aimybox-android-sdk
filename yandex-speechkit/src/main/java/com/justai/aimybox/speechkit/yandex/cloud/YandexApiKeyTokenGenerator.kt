package com.justai.aimybox.speechkit.yandex.cloud

open class YandexApiKeyTokenGenerator(open val apiKey: String): IAmTokenProvider {
    override val authType = IAmTokenProvider.AuthType.API_KEY
    override suspend fun getOAuthToken() = apiKey
}