package com.justai.aimybox.texttospeech

import com.justai.aimybox.BaseCoroutineTest
import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.model.TextSpeech
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse

class TextToSpeechComponentTest : BaseCoroutineTest() {

    private lateinit var mockDelegate: TextToSpeech
    private lateinit var eventBus: EventBus<TextToSpeech.Event>
    private lateinit var exceptionBus: EventBus<AimyboxException>
    private lateinit var component: TextToSpeechComponent

    private val testSpeechList = listOf(TextSpeech("Hello"), TextSpeech("World"))

    @Before
    fun setUp() {
        mockDelegate = mockk(relaxed = true)
        eventBus = EventBus()
        exceptionBus = EventBus()
        component = TextToSpeechComponent(mockDelegate, eventBus, exceptionBus)

        coEvery { mockDelegate.synthesize(testSpeechList) } coAnswers {
            testSpeechList.forEach {
                eventBus.invokeEvent(TextToSpeech.Event.SpeechStarted(it))
                println("Speaking $it")
                delay(300)
                eventBus.invokeEvent(TextToSpeech.Event.SpeechEnded(it))
            }
        }
    }

//    @Test
//    fun `Regular synthesis`() {
//        runInTestContext {
//
//            launch { component.speak(testSpeechList) }
//
//            assert(eventBus.receive() is TextToSpeech.Event.SpeechSequenceStarted)
//
//            repeat(testSpeechList.size) {
//                assert(eventBus.receive() is TextToSpeech.Event.SpeechStarted)
//                assert(eventBus.receive() is TextToSpeech.Event.SpeechEnded)
//            }
//
//            assert(eventBus.receive() is TextToSpeech.Event.SpeechSequenceCompleted)
//
//            coVerify { mockDelegate.synthesize(testSpeechList ) }
//            checkNoRunningJobs()
//        }
//    }
//
//
//    @Test
//    fun `Synthesis cancellation`() {
//        runInTestContext {
//            launch { component.speak(testSpeechList) }
//
//
//            assert(eventBus.receive() is TextToSpeech.Event.SpeechSequenceStarted)
//            assert(eventBus.receive() is TextToSpeech.Event.SpeechStarted)
//
//            coVerify { mockDelegate.synthesize(testSpeechList) }
//
//
//            delay(400)
//            assert(eventBus.receive() is TextToSpeech.Event.SpeechEnded)
//            assert(eventBus.receive() is TextToSpeech.Event.SpeechStarted)
//
//            component.cancelRunningJob()
//
//            coVerify { mockDelegate.stop() }
//
//            checkNoRunningJobs()
//        }
//    }
//
//
//    private fun checkNoRunningJobs() = assertFalse(
//        component.hasRunningJobs,
//        "Component has running jobs ${component.coroutineContext[Job]?.children?.toList()}"
//    )
}