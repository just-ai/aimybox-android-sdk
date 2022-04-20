package com.justai.aimybox.speechtotext

import com.justai.aimybox.BaseCoroutineTest
import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.logging.Logger
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue


class SpeechToTextComponentTest : BaseCoroutineTest() {

    private lateinit var mockDelegate: SpeechToText
    private lateinit var eventBus: EventBus<SpeechToText.Event>
    private lateinit var exceptionBus: EventBus<AimyboxException>
    private lateinit var result: EventBus<SpeechToText.Result>
    private lateinit var component: SpeechToTextComponent
    private lateinit var logger: Logger

    private val exceptionEvents =
        listOf(SpeechToText.Result.Exception(SpeechToTextException("Error")))

    @Before
    fun setUp() {
        mockDelegate = mockk(relaxed = true)
        eventBus = EventBus()
        exceptionBus = EventBus()
        result = EventBus()
        component = SpeechToTextComponent(mockDelegate, eventBus, exceptionBus)

        logger = Logger("Test")

        every { mockDelegate.startRecognition() } returns result.events
        every { mockDelegate.recognitionTimeoutMs } returns 5000
    }

    @Test
    fun `Regular recognition`() {
        runInTestContext {

            eventBus.events
                .withIndex()
                .onEach { indexedEvent ->
                    when (indexedEvent.index) {
                        0 -> assertTrue(indexedEvent.value is SpeechToText.Event.RecognitionStarted)
                    }
                }
                .launchIn(this)

            result.events
                .withIndex()
                .onEach { indexedEvent ->
                    when (indexedEvent.index) {
                        0, 1, 2 -> assertTrue(indexedEvent.value is SpeechToText.Result.Partial)
                        else -> assertTrue(indexedEvent.value is SpeechToText.Result.Final)
                    }
                }
                .launchIn(this)

            val deferred = async {
                val recognizedSpeech = component.recognizeSpeech()
                verify { mockDelegate.startRecognition() }
                recognizedSpeech
            }

            delay(500)

            result.invokeEvent(SpeechToText.Result.Partial("1"))
            result.invokeEvent(SpeechToText.Result.Partial("1 2"))
            result.invokeEvent(SpeechToText.Result.Partial("1 2 3"))

            result.invokeEvent(SpeechToText.Result.Final("Final"))

            assertEquals("Final", deferred.await())

            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `Recognition with error`() {
        runInTestContext {

            eventBus.events
                .withIndex()
                .onEach { indexedEvent ->
                    logger.e("Events bus: index: ${indexedEvent.index}, value: ${indexedEvent.value}")
                    when (indexedEvent.index) {
                        0 -> assertTrue(indexedEvent.value is SpeechToText.Event.RecognitionStarted)
                    }
                }
                .launchIn(this)

            result.events
                .withIndex()
                .onEach { indexedEvent ->
                    logger.e("Result bus: index: ${indexedEvent.index}, value: ${indexedEvent.value}")
                }
                .launchIn(this)

            exceptionBus.events
                .onEach { event ->
                    logger.e("Exception bus: ${event}")
                }
                .launchIn(this)

            val deferred = async {
                val recognizedSpeech = component.recognizeSpeech()
                verify { mockDelegate.startRecognition() }
                recognizedSpeech
            }

            delay(500)

            result.invokeEvent(SpeechToText.Result.Exception(SpeechToTextException("Error")))

            val result = deferred.await()
            assertNull(result)

            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `Recognition canceled`() {
        runInTestContext {
            eventBus.events
                .withIndex()
                .onEach { indexedEvent ->
                    logger.e("Events bus: index: ${indexedEvent.index}, value: ${indexedEvent.value}")
                    when (indexedEvent.index) {
                        0 -> assertTrue(indexedEvent.value is SpeechToText.Event.RecognitionStarted)
                    }
                }
                .launchIn(this)

            val deferred = async {
                val recognizedSpeech = component.recognizeSpeech()
                verify { mockDelegate.startRecognition() }
                recognizedSpeech
            }

            delay(500)

            component.cancelRunningJob()

            coVerify { mockDelegate.cancelRecognition() }

            assertNull(deferred.await())

            coroutineContext.cancelChildren()
        }
    }
    
}