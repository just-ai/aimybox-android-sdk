package com.justai.aimybox.speechtotext

import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.mockLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse

class SpeechToTextComponentTest {

    init {
        mockLog()
    }

    val testContext = newSingleThreadContext("Test")

    lateinit var mockDelegate: SpeechToText
    lateinit var eventChannel: Channel<SpeechToText.Event>
    lateinit var exceptionChannel: Channel<AimyboxException>
    lateinit var resultChannel: Channel<SpeechToText.Result>
    lateinit var component: SpeechToTextComponent

    @Before
    fun prepare() {
        mockDelegate = mockk(relaxed = true)
        eventChannel = Channel(Channel.UNLIMITED)
        exceptionChannel = Channel(Channel.UNLIMITED)
        resultChannel = Channel(Channel.UNLIMITED)
        component = SpeechToTextComponent(mockDelegate, eventChannel, exceptionChannel)
        every { mockDelegate.startRecognition() }.returns(resultChannel)
    }

    @After
    fun check() {
        verify { mockDelegate.startRecognition() }
    }

    @Test
    fun recognizeNormalTest() {
        runBlocking(testContext) {

            val deferred = async { component.recognizeSpeech() }

            resultChannel.send(SpeechToText.Result.Partial("1"))
            resultChannel.send(SpeechToText.Result.Partial("1 2"))
            resultChannel.send(SpeechToText.Result.Partial("1 2 3"))

            eventChannel.receive() as SpeechToText.Event.RecognitionPartialResult
            eventChannel.receive() as SpeechToText.Event.RecognitionPartialResult
            eventChannel.receive() as SpeechToText.Event.RecognitionPartialResult

            resultChannel.send(SpeechToText.Result.Final("Final"))

            val result = withTimeout(100) { deferred.await() }

            assert(result == "Final") { "Result is received" }
            assert((eventChannel.receive() as SpeechToText.Event.RecognitionResult).text == "Final")
            assert(resultChannel.isClosedForSend) { "Result channel is closed" }

        }
    }

    @Test
    fun recognizeErrorTest() {
        runBlocking {
            every { mockDelegate.startRecognition() }.returns(resultChannel)
            val deferred = async { component.recognizeSpeech() }

            resultChannel.send(SpeechToText.Result.Exception(SpeechToTextException(RuntimeException())))

            val result = withTimeout(100) { deferred.await() }

            eventChannel.receive() as SpeechToText.Event.EmptyRecognitionResult
            exceptionChannel.receive() as SpeechToTextException

            assert(result == null) { "Result is received" }
            assert(resultChannel.isClosedForSend) { "Result channel is closed" }
            assertFalse("Every job is finished"){ component.hasRunningJobs }
        }
    }


    @Test
    fun recognizeCancelTest() {
        runBlocking {
            val deferred = async { component.recognizeSpeech() }

            component.cancel()

            val result = withTimeout(100) { deferred.await() }

            eventChannel.receive() as SpeechToText.Event.EmptyRecognitionResult
            exceptionChannel.receive() as SpeechToTextException

            assert(result == null) { "Result is received" }
            assert(resultChannel.isClosedForSend) { "Result channel is closed" }
            assertFalse("Every job is finished"){ component.hasRunningJobs }
        }
    }

    @Test
    fun resultChannelCloseTest() {
        runBlocking {
            val deferred = async { component.recognizeSpeech() }

            resultChannel.close()

            component.cancel()

            val result = withTimeout(100) { deferred.await() }

            eventChannel.receive() as SpeechToText.Event.EmptyRecognitionResult
            exceptionChannel.receive() as SpeechToTextException

            assert(result == null) { "Result is received" }
            assert(resultChannel.isClosedForSend) { "Result channel is closed" }
            assertFalse("Every job is finished"){ component.hasRunningJobs }
        }
    }
}