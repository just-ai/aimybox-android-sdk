package com.justai.aimybox.speechkit.yandex.cloud

interface IAmTokenProvider {

    suspend fun getOAuthToken(): String
}