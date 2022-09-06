package com.justai.aimybox.speechkit.tinkoff

import android.Manifest
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.justai.aimybox.api.aimybox.EventBus
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.texttospeech.TextToSpeech
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class TinkoffTextToSpeechTest {

    companion object {
        const val TEST_PHRASE = "Привет мир"
    }

    private lateinit var tts:  TinkoffTextToSpeech
    private lateinit var androidContext: Context
    private lateinit var eventBus: EventBus<TextToSpeech.Event>

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET)

    @Before
    fun init(){
        androidContext = ApplicationProvider.getApplicationContext()
        eventBus = EventBus()

        val config = TinkoffTextToSpeech.Config()
        val tokenProvider = TokenProviderImpl(BuildConfig.tinkoffApiKey, BuildConfig.tinkoffSecretKey, "tinkoff.cloud.tts")
        tts = TinkoffTextToSpeech(
            context = androidContext,
            tokenProvider = tokenProvider,
            config = config,
        )

    }

    @ExperimentalCoroutinesApi
    @Test
    fun speak() = runTest {
        tts.speak(TextSpeech(TEST_PHRASE))
    }
}