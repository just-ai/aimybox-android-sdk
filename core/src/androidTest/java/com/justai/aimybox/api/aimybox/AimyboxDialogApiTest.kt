package com.justai.aimybox.api.aimybox


import org.junit.Before
import org.junit.Test
import java.util.*

class AimyboxDialogApiAndroidTest {

    private lateinit var dialogApi: AimyboxDialogApi
    private lateinit var uuid : String

    companion object {
        private const val AIMYBOX_API_KEY = "Ldf0j7WZi3KwNah2aNeXVIACz0lb9qMH"
        private const val DEFAULT_API_URL = "https://api.aimybox.com/"
    }

    @Before
    fun setUp() {
        uuid =  UUID.randomUUID().toString()
        dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, uuid, url = DEFAULT_API_URL)
    }

    @Test
    fun sendRequest() {
        dialogApi.createRequest("/start")
    }
}