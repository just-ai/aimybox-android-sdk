package com.justai.aimybox.api.aimybox


import com.justai.aimybox.Aimybox
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.Response
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.assertTrue


class RequestHandlerSkill : CustomSkill<AimyboxRequest, AimyboxResponse> {

    override fun canHandleRequest(query: String): Boolean {
        return query.contains("Hello")
    }

    override fun canHandle(response: AimyboxResponse) =
        response.action == "openSettings"

    override suspend fun onResponse(
        response: AimyboxResponse,
        aimybox: Aimybox,
        defaultHandler: suspend (Response) -> Unit
    ) {

    }
}


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
        val s = RequestHandlerSkill()
        val skills = linkedSetOf<CustomSkill<AimyboxRequest, AimyboxResponse>>(RequestHandlerSkill())
        dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, uuid, url = DEFAULT_API_URL, customSkills = skills)
    }

    @Test
    fun sendRequest() {
        dialogApi.createRequest("/start")
    }
    
}