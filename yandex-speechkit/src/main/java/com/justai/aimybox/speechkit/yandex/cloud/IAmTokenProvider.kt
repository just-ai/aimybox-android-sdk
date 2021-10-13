package com.justai.aimybox.speechkit.yandex.cloud

interface IAmTokenProvider {
    val authType: AuthType

    suspend fun getOAuthToken(): String

    class AuthType(val authKeyString: String) {
        companion object {
            val API_KEY = AuthType("Api-Key")
            val BEARER = AuthType("Bearer")
        }
    }
}