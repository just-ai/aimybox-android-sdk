package com.justai.aimybox.api

import com.google.gson.JsonObject
import com.justai.aimybox.BaseCoroutineTest
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.ApiRequestTimeoutException
import com.justai.aimybox.model.Request
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class DialogApiComponentTest : BaseCoroutineTest() {

    private lateinit var mockDelegate: DialogApi
    private lateinit var exceptionChannel: Channel<AimyboxException>
    private lateinit var eventChannel: Channel<DialogApi.Event>
    private lateinit var component: DialogApiComponent

    @Before
    fun setUp() {
        mockDelegate = mockk(relaxed = true)
        exceptionChannel = Channel(Channel.UNLIMITED)
        eventChannel = Channel(Channel.UNLIMITED)
        component = DialogApiComponent(mockDelegate, eventChannel, exceptionChannel)
    }

    @Test
    fun `Regular request`() {
        runInTestContext {
            val testRequest = Request("Test query")
            val testResponse =
                AimyboxResponse(testRequest.query, "Test response", json = JsonObject())

            every { mockDelegate.getRequestTimeoutMs() } returns 300
            coEvery { mockDelegate.send(testRequest) } coAnswers {
                delay(100)
                testResponse
            }

            val response = async { component.send(testRequest) }

            assert(eventChannel.receive() is DialogApi.Event.RequestSent)
            assertSame(response.await(), testResponse)
            assert(eventChannel.receive() is DialogApi.Event.ResponseReceived)
            checkNoRunningJobs()
        }
    }

    @Test
    fun `Request timeout`() {
        runInTestContext {
            val testRequest = Request("Test query")
            val testResponse =
                AimyboxResponse(testRequest.query, "Test response", json = JsonObject())

            every { mockDelegate.getRequestTimeoutMs() } returns 300
            coEvery { mockDelegate.send(testRequest) } coAnswers {
                delay(500)
                testResponse
            }

            val response = async { component.send(testRequest) }

            assert(eventChannel.receive() is DialogApi.Event.RequestSent)
            assertNull(response.await())
            assert(exceptionChannel.receive() is ApiRequestTimeoutException)
            checkNoRunningJobs()
        }
    }

    @Test
    fun `Request cancelled`() {
        runInTestContext {
            val testRequest = Request("Test query")
            val testResponse =
                AimyboxResponse(testRequest.query, "Test response", json = JsonObject())

            every { mockDelegate.getRequestTimeoutMs() } returns 300
            coEvery { mockDelegate.send(testRequest) } coAnswers {
                delay(500)
                testResponse
            }

            val response = async { component.send(testRequest) }

            assert(eventChannel.receive() is DialogApi.Event.RequestSent)

            delay(100)
            component.cancel()

            assertNull(response.await())
            assert(eventChannel.receive() is DialogApi.Event.RequestCancelled)
            checkNoRunningJobs()
        }
    }

    private fun checkNoRunningJobs() = assertFalse(
        component.hasRunningJobs,
        "Component has running jobs ${component.coroutineContext[Job]?.children?.toList()}"
    )
}