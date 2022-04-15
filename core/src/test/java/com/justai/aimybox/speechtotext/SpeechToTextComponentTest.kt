package com.justai.aimybox.speechtotext

import com.justai.aimybox.BaseCoroutineTest
import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.logging.Logger
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame


class SpeechToTextComponentTest : BaseCoroutineTest() {

    private lateinit var mockDelegate: SpeechToText
    private lateinit var eventBus: EventBus<SpeechToText.Event>
    private lateinit var exceptionBus: EventBus<AimyboxException>
    private lateinit var result: EventBus<SpeechToText.Result>
    private lateinit var component: SpeechToTextComponent
    private lateinit var logger : Logger

    private val events = listOf(SpeechToText.Result.Partial("1"),
        SpeechToText.Result.Partial("1 2"),
        SpeechToText.Result.Partial("1 2 3"),
        SpeechToText.Result.Final("Final")
    )

    private val exceptionEvents = listOf(SpeechToText.Result.Exception(SpeechToTextException("Error")))

    @Before
    fun setUp() {
        mockDelegate = mockk(relaxed = true)
        eventBus = EventBus()
        exceptionBus = EventBus()
        result = EventBus()
        component = SpeechToTextComponent(mockDelegate, eventBus, exceptionBus)

        logger = Logger("Test")

        every {  mockDelegate.startRecognition() } returns result.events
        every { mockDelegate.recognitionTimeoutMs } returns 2000
    }

    @Test
    fun `Regular recognition`() {
        runInTestContext {

            every {  mockDelegate.startRecognition() } returns  result.events

            logger.i("Begin test")

            val deferred = async {
                val recognizedSpeech = component.recognizeSpeech()
                verify { mockDelegate.startRecognition() }
                recognizedSpeech
            }

            delay(7000)

                logger.i("Start sending events")
                result.invokeEvent(SpeechToText.Result.Partial("1"))
                result.invokeEvent(SpeechToText.Result.Partial("1 2"))
                result.invokeEvent(SpeechToText.Result.Partial("1 2 3"))

                result.invokeEvent(SpeechToText.Result.Final("Final"))

            launch {
                eventBus.events.collect { event ->
                         logger.i("event: $event")
                    }
            }

           val speech = deferred.await()
           logger.i("recognition result: $speech")


            //     assertEquals("Final", deferred.await())
        //    assertEquals("Final", (eventBus.events.last() as SpeechToText.Event.RecognitionResult).text)
            //    checkNoRunningJobs()
            coroutineContext.cancelChildren()
        }
    }

    @Test
    fun `Recognition with error`() {
        runInTestContext {
            val deferred = async {
                component.recognizeSpeech()
                verify { mockDelegate.startRecognition() }
            }

            delay(500)

            launch {
                eventBus.events.collect { event ->
                    logger.i("event: $event")
                }
            }


           // assertSame(eventBus.receive(), SpeechToText.Event.RecognitionStarted)
            eventBus.events.collect { event ->
                assertSame(event, SpeechToText.Event.RecognitionStarted)
            }


            //

            result.invokeEvent(SpeechToText.Result.Exception(SpeechToTextException("Error")))

            eventBus.events.collect { event ->
                assertSame(event, SpeechToText.Event.EmptyRecognitionResult)
                assert(event is SpeechToTextException)
            }

//            assertSame(eventBus.receive(), SpeechToText.Event.EmptyRecognitionResult)
//            assert(exceptionBus.receive() is SpeechToTextException)

          //  assertNull(deferred.await())
           // assert(result.isClosedForSend)
           // checkNoRunningJobs()
        }
    }

    @Test
    fun `Recognition canceled`() {
        runInTestContext {
            //val deferred = async { component.recognizeSpeech() }
            component.recognizeSpeech()

            //assertSame(eventBus.receive(), SpeechToText.Event.RecognitionStarted)
            eventBus.events.collect { event ->
                assertSame(event, SpeechToText.Event.RecognitionStarted)
            }

            verify { mockDelegate.startRecognition() }

            component.cancelRunningJob()

            //assertSame(eventBus.receive(), SpeechToText.Event.RecognitionCancelled)

            eventBus.events.collect { event ->
                assertSame(event, SpeechToText.Event.RecognitionCancelled)
            }

            coVerify { mockDelegate.cancelRecognition() }

//            assertFails { deferred.await() }
//            assert(result.isClosedForSend)
//            checkNoRunningJobs()
        }
    }

//    @Test
//    fun `Recognition result channel closed`() {
//        runInTestContext {
//            val deferred = async { component.recognizeSpeech() }
//            assertSame(eventBus.receive(), SpeechToText.Event.RecognitionStarted)
//
//            verify { mockDelegate.startRecognition() }
//
//            result.close()
//
//            assertSame(eventBus.receive(), SpeechToText.Event.EmptyRecognitionResult)
//
//            assertNull(deferred.await())
//            assert(result.isClosedForSend)
//            checkNoRunningJobs()
//        }
//    }
//
//    private fun checkNoRunningJobs() = assertFalse(
//        component.hasRunningJobs,
//        "Component has running jobs ${component.coroutineContext[Job]?.children?.toList()}"
//    )
}