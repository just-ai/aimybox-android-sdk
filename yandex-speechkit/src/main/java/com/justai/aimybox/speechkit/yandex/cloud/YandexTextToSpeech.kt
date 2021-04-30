package com.justai.aimybox.speechkit.yandex.cloud

import android.content.Context
import com.justai.aimybox.speechtotext.SampleRate

class YandexTextToSpeech(
    context: Context,
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    defaultLanguage: Language,
    config: Config
): AbstractYandexTextToSpeech<YandexTextToSpeech.Config>(
    context, iAmTokenProvider, folderId, defaultLanguage, config
) {
    override val api: AbstractYandexSynthesisApi<Config> = YandexSynthesisApi(iAmTokenProvider, folderId, config)

    data class Config(
        override val apiUrl: String = "https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize",
        override val voice: Voice = Voice.ALYSS,
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_48KHZ,
        val emotion: Emotion = Emotion.NEUTRAL,
        val speed: Speed = Speed.DEFAULT,
        override val enableLoggingData: Boolean = false,
    ) : BaseConfig(apiUrl, voice, enableLoggingData)
}