package com.justai.aimybox.speechtotext

import com.justai.aimybox.BaseCoroutineTest
import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.SpeechToTextException
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

class SpeechToTextComponentTest : BaseCoroutineTest() {

    private lateinit var mockDelegate: SpeechToText
    private lateinit var eventBus: EventBus<SpeechToText.Event>
    private lateinit var exceptionBus: EventBus<AimyboxException>
    private lateinit var result: EventBus<SpeechToText.Result>
    private lateinit var component: SpeechToTextComponent

    @Before
    fun setUp() {
        mockDelegate = mockk(relaxed = true)
        eventBus = EventBus()
        exceptionBus = EventBus()
        result = EventBus()
        component = SpeechToTextComponent(mockDelegate, eventBus, exceptionBus)

        every { mockDelegate.startRecognition() } returns result
        every { mockDelegate.recognitionTimeoutMs } returns 5000
    }

    @Test
    fun `Regular recognition`() {
        runInTestContext {
            val deferred = async { component.recognizeSpeech() }

            assertSame(eventBus.receive(), SpeechToText.Event.RecognitionStarted)

            verify { mockDelegate.startRecognition() }

            result.send(SpeechToText.Result.Partial("1"))
            result.send(SpeechToText.Result.Partial("1 2"))
            result.send(SpeechToText.Result.Partial("1 2 3"))

            assert(eventBus.receive() is SpeechToText.Event.RecognitionPartialResult)
            assert(eventBus.receive() is SpeechToText.Event.RecognitionPartialResult)
            assert(eventBus.receive() is SpeechToText.Event.RecognitionPartialResult)

            result.send(SpeechToText.Result.Final("Final"))

            assertEquals("Final", deferred.await())
            assertEquals("Final", (eventBus.receive() as SpeechToText.Event.RecognitionResult).text)
            checkNoRunningJobs()
        }
    }

    @Test
    fun `Recognition with error`() {
        runInTestContext {
            val deferred = async { component.recognizeSpeech() }

            assertSame(eventBus.receive(), SpeechToText.Event.RecognitionStarted)

            verify { mockDelegate.startRecognition() }

            result.send(SpeechToText.Result.Exception(SpeechToTextException("Error")))

            assertSame(eventBus.receive(), SpeechToText.Event.EmptyRecognitionResult)
            assert(exceptionBus.receive() is SpeechToTextException)

            assertNull(deferred.await())
            assert(result.isClosedForSend)
            checkNoRunningJobs()
        }
    }

    @Test
    fun `Recognition canceled`() {
        runInTestContext {
            val deferred = async { component.recognizeSpeech() }
            assertSame(eventBus.receive(), SpeechToText.Event.RecognitionStarted)

            verify { mockDelegate.startRecognition() }

            component.cancelRunningJob()
            assertSame(eventBus.receive(), SpeechToText.Event.RecognitionCancelled)

            coVerify { mockDelegate.cancelRecognition() }

            assertFails { deferred.await() }
            assert(result.isClosedForSend)
            checkNoRunningJobs()
        }
    }

    @Test
    fun `Recognition result channel closed`() {
        runInTestContext {
            val deferred = async { component.recognizeSpeech() }
            assertSame(eventBus.receive(), SpeechToText.Event.RecognitionStarted)

            verify { mockDelegate.startRecognition() }

            result.close()

            assertSame(eventBus.receive(), SpeechToText.Event.EmptyRecognitionResult)

            assertNull(deferred.await())
            assert(result.isClosedForSend)
            checkNoRunningJobs()
        }
    }

    private fun checkNoRunningJobs() = assertFalse(
        component.hasRunningJobs,
        "Component has running jobs ${component.coroutineContext[Job]?.children?.toList()}"
    )
}