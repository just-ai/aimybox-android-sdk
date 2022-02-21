package com.justai.aimybox.api.aimybox

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotEquals
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.util.*

@Config(manifest= Config.NONE)
@RunWith(RobolectricTestRunner::class)
class AimyboxDialogApiTest {

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
    fun `send start request`() {
        runBlocking {
            val request = dialogApi.createRequest("/start")
            val response = dialogApi.send(request)
            assertNotEquals(0, response.json.size())
        }
    }
}