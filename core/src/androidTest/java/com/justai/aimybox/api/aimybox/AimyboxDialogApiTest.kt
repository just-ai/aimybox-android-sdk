package com.justai.aimybox.api.aimybox


import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.ApiException
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.broadcast
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.util.*


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

    private val requestSlot = slot<Request>()
    private val throwableSlot = slot<Throwable>()
    private lateinit var mockAimyBox: Aimybox
    private lateinit var dialogApiEvents : Channel<DialogApi.Event>
    private lateinit var exceptions: Channel<AimyboxException>
    private lateinit var dialogApi: AimyboxDialogApi
    private lateinit var uuid : String


    companion object {
        private const val AIMYBOX_API_KEY = "Ldf0j7WZi3KwNah2aNeXVIACz0lb9qMH"
        private const val DEFAULT_API_URL = "https://api.aimybox.com/"

        private const val QUERY_STRING = "Hello world"
    }

    @OptIn(ObsoleteCoroutinesApi::class)
    @Before
    fun setUp() {

        mockAimyBox = mockk<Aimybox>(relaxed = true)
        dialogApiEvents = Channel(Channel.UNLIMITED)
        exceptions = Channel(Channel.UNLIMITED)


     //   coEvery { mockAimyBox.dialogApiEvents }  mockk()
        coEvery { mockAimyBox.exceptions } coAnswers {exceptions.broadcast()}

//                coEvery {  val request = capture(requestSlot)
//            mockAimyBox.dialogApiEvents.send(DialogApi.Event.RequestSent(request)) } coAnswers {
//            dialogApiEvents.send(DialogApi.Event.RequestSent(request = requestSlot.captured))
//        }
//        coEvery {
//            val e = capture(throwableSlot)
//            mockAimyBox.exceptions.send(ApiException(cause = e))
//        } coAnswers {
//            exceptions.send(ApiException(cause = throwableSlot.captured))
//        }
        uuid =  UUID.randomUUID().toString()
        val s = RequestHandlerSkill()
        val skills = linkedSetOf<CustomSkill<AimyboxRequest, AimyboxResponse>>(RequestHandlerSkill())
        dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, uuid, url = DEFAULT_API_URL, customSkills = skills)
    }

    @Test
    fun sendRequest() {
        dialogApi.createRequest("/start")
    }

    @Test
    fun sendFullPathRequest() : Unit = runBlocking{

        dialogApi.send(QUERY_STRING, aimybox = mockAimyBox)
    }

}