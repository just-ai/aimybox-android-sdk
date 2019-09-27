package com.justai.aimybox.speechtotext

import com.justai.aimybox.BaseCoroutineTest
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.SpeechToTextException
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class SpeechToTextComponentTest : BaseCoroutineTest() {

    private lateinit var mockDelegate: SpeechToText
    private lateinit var eventChannel: Channel<SpeechToText.Event>
    private lateinit var exceptionChannel: Channel<AimyboxException>
    private lateinit var resultChannel: Channel<SpeechToText.Result>
    private lateinit var component: SpeechToTextComponent

    @Before
    fun setUp() {
        mockDelegate = mockk(relaxed = true)
        eventChannel = Channel(Channel.UNLIMITED)
        exceptionChannel = Channel(Channel.UNLIMITED)
        resultChannel = Channel(Channel.UNLIMITED)
        component = SpeechToTextComponent(mockDelegate, eventChannel, exceptionChannel)

        every { mockDelegate.startRecognition() } returns resultChannel
        every { mockDelegate.recognitionTimeoutMs } returns 5000
    }

    @Test
    fun `Regular recognition`() {
        runInTestContext {
            val deferred = async { component.recognizeSpeech() }

            assertSame(eventChannel.receive(), SpeechToText.Event.RecognitionStarted)

            verify { mockDelegate.startRecognition() }

            resultChannel.send(SpeechToText.Result.Partial("1"))
            resultChannel.send(SpeechToText.Result.Partial("1 2"))
            resultChannel.send(SpeechToText.Result.Partial("1 2 3"))

            assert(eventChannel.receive() is SpeechToText.Event.RecognitionPartialResult)
            assert(eventChannel.receive() is SpeechToText.Event.RecognitionPartialResult)
            assert(eventChannel.receive() is SpeechToText.Event.RecognitionPartialResult)

            resultChannel.send(SpeechToText.Result.Final("Final"))

            assertEquals("Final", deferred.await())
            assertEquals("Final", (eventChannel.receive() as SpeechToText.Event.RecognitionResult).text)
            checkNoRunningJobs()
        }
    }

    @Test
    fun `Recognition with error`() {
        runInTestContext {
            val deferred = async { component.recognizeSpeech() }

            assertSame(eventChannel.receive(), SpeechToText.Event.RecognitionStarted)

            verify { mockDelegate.startRecognition() }

            resultChannel.send(SpeechToText.Result.Exception(SpeechToTextException("Error")))

            assertSame(eventChannel.receive(), SpeechToText.Event.EmptyRecognitionResult)
            assert(exceptionChannel.receive() is SpeechToTextException)

            assertNull(deferred.await())
            assert(resultChannel.isClosedForSend)
            checkNoRunningJobs()
        }
    }

    @Test
    fun `Recognition canceled`() {
        runInTestContext {
            val deferred = async { component.recognizeSpeech() }
            assertSame(eventChannel.receive(), SpeechToText.Event.RecognitionStarted)

            verify { mockDelegate.startRecognition() }

            component.cancel()
            assertSame(eventChannel.receive(), SpeechToText.Event.RecognitionCancelled)

            coVerify { mockDelegate.cancelRecognition() }

            assertFails { deferred.await() }
            assert(resultChannel.isClosedForSend)
            checkNoRunningJobs()
        }
    }

    @Test
    fun `Recognition result channel closed`() {
        runInTestContext {
            val deferred = async { component.recognizeSpeech() }
            assertSame(eventChannel.receive(), SpeechToText.Event.RecognitionStarted)

            verify { mockDelegate.startRecognition() }

            resultChannel.close()

            assertSame(eventChannel.receive(), SpeechToText.Event.EmptyRecognitionResult)

            assertNull(deferred.await())
            assert(resultChannel.isClosedForSend)
            checkNoRunningJobs()
        }
    }

    private fun checkNoRunningJobs() = assertFalse(
        component.hasRunningJobs,
        "Component has running jobs ${component.coroutineContext[Job]?.children?.toList()}"
    )
}