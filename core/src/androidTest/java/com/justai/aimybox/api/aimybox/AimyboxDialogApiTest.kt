package com.justai.aimybox.api.aimybox


import com.justai.aimybox.Aimybox
import com.justai.aimybox.core.CustomSkill
import io.mockk.mockk
import org.junit.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.*


class HelloRequestHandlerSkill : CustomSkill<AimyboxRequest, AimyboxResponse> {

    override fun canHandleRequest(query: String): Boolean {
        return query.contains("Hello")
    }

    override suspend fun onRequest(request: AimyboxRequest, aimybox: Aimybox): AimyboxRequest {
        return request.copy(query = request.query + " world!")
    }

}

class OpenRequestHandlerSkill : CustomSkill<AimyboxRequest, AimyboxResponse> {

    override fun canHandleRequest(query: String): Boolean {
        return query.contains("Open")
    }

    override suspend fun onRequest(request: AimyboxRequest, aimybox: Aimybox): AimyboxRequest {
        return request.copy(query = request.query + " the table!")
    }
}

class SwitchedRequestHandlerSkill : CustomSkill<AimyboxRequest, AimyboxResponse> {

    var isRequestProcessed = false

    override fun canHandleRequest(query: String): Boolean {
        return isRequestProcessed
    }

    override suspend fun onRequest(request: AimyboxRequest, aimybox: Aimybox): AimyboxRequest {
        return request.copy(query = "Test passed")
    }


}


class AimyboxDialogApiAndroidTest {

    private lateinit var mockAimyBox: Aimybox

    private lateinit var dialogApi: AimyboxDialogApi
    private lateinit var uuid: String

    private lateinit var customSkills: LinkedHashSet<CustomSkill<AimyboxRequest, AimyboxResponse>>

    companion object {
        private const val AIMYBOX_API_KEY = "key"
        private const val DEFAULT_API_URL = "https://api.aimybox.com/"

        private const val HELLO_QUERY_STRING = "Hello"
        private const val OPEN_QUERY_STRING = "Open"
        private const val HELLO_AND_OPEN_QUERY_STRING = "Open the door"
    }

    @Before
    fun setUp() {

        mockAimyBox = mockk(relaxed = true)

        customSkills = linkedSetOf(
            HelloRequestHandlerSkill(),
            OpenRequestHandlerSkill(),
            SwitchedRequestHandlerSkill()
        )

        uuid = UUID.randomUUID().toString()
        dialogApi = AimyboxDialogApi(
            AIMYBOX_API_KEY, uuid,
            url = DEFAULT_API_URL, customSkills = customSkills
        )
    }

    @Test
    fun sendRequest() {
        dialogApi.createRequest("/start")
    }

    @Test
    fun handleSimpleRequest(): Unit = runBlocking {

        val helloRequest = dialogApi.createRequest(HELLO_QUERY_STRING)
        val openRequest = dialogApi.createRequest(OPEN_QUERY_STRING)
        val hAndORequest = dialogApi.createRequest(HELLO_AND_OPEN_QUERY_STRING)

        val helloResult =
            customSkills.filter { it.canHandleRequest(HELLO_QUERY_STRING) }
                .fold(helloRequest) { request, skill ->
                    skill.onRequest(request, mockAimyBox)
                }

        assertEquals("Hello world!", helloResult.query)

        val openResult =
            customSkills.filter { it.canHandleRequest(OPEN_QUERY_STRING) }
                .fold(openRequest) { request, skill ->
                    skill.onRequest(request, mockAimyBox)
                }

        assertEquals("Open the table!", openResult.query)

        val hAndOResult =
            customSkills.filter { it.canHandleRequest(HELLO_AND_OPEN_QUERY_STRING) }
                .fold(hAndORequest) { request, skill ->
                    skill.onRequest(request, mockAimyBox)
                }

        assertEquals("Open the door the table!", hAndOResult.query)

    }

    //it's important to remember the order of skills
    @Test
    fun handleManagedRequest(): Unit = runBlocking {

        val managedRequest = dialogApi.createRequest(HELLO_AND_OPEN_QUERY_STRING)

        val resultRequest1 =
            customSkills.filter { it.canHandleRequest(HELLO_AND_OPEN_QUERY_STRING) }
                .fold(managedRequest) { request, skill ->
                    skill.onRequest(request, mockAimyBox)
                }

        assertEquals("Open the door the table!", resultRequest1.query)

        customSkills.iterator().forEach { skill ->
            if (skill is SwitchedRequestHandlerSkill) {
                skill.isRequestProcessed = true
            }
        }

        val resultRequest2 =
            customSkills.filter { it.canHandleRequest(HELLO_AND_OPEN_QUERY_STRING) }
                .fold(managedRequest) { request, skill ->
                    skill.onRequest(request, mockAimyBox)
                }
        assertEquals("Test passed", resultRequest2.query)
    }
}