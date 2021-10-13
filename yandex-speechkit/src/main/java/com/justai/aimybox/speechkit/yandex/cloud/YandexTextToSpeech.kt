package com.justai.aimybox.speechkit.yandex.cloud

import android.content.Context
import com.justai.aimybox.speechtotext.SampleRate

@Deprecated(
    message = "Use YandexTextToSpeech.V1 instead.",
    replaceWith =
        ReplaceWith("YandexTextToSpeech.V1(context, iAmTokenProvider, folderId, defaultLanguage, config)")
)
fun YandexTextToSpeech(
    context: Context,
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    defaultLanguage: Language,
    config: YandexTextToSpeech.Config = YandexTextToSpeech.Config()
) = YandexTextToSpeech.V1(context, iAmTokenProvider, folderId, defaultLanguage, config)

class YandexTextToSpeech private constructor() {
    companion object {
        fun V1(
            context: Context,
            iAmTokenProvider: IAmTokenProvider,
            folderId: String,
            defaultLanguage: Language,
            config: Config
        ) = YandexTextToSpeechV1(context, iAmTokenProvider, folderId, defaultLanguage, config)

        fun V3(
            context: Context,
            iAmTokenProvider: IAmTokenProvider,
            folderId: String,
            defaultLanguage: Language,
            config: ConfigV3
        ) = YandexTextToSpeechV3(context, iAmTokenProvider, folderId, defaultLanguage, config)
    }

    data class Config(
        override val apiUrl: String = "https://tts.api.cloud.yandex.net/speech/v1/tts:synthesize",
        override val voice: Voice = Voice.ALYSS,
        val sampleRate: SampleRate = SampleRate.SAMPLE_RATE_48KHZ,
        val emotion: Emotion = Emotion.NEUTRAL,
        val speed: Speed = Speed.DEFAULT,
        override val enableLoggingData: Boolean = false,
    ) : AbstractYandexTextToSpeech.BaseConfig(apiUrl, voice, enableLoggingData)


    data class ConfigV3(
        override val apiUrl: String = "tts.api.cloud.yandex.net",
        val apiPort: Int = 443,
        override val voice: Voice = Voice.V3.KUZNETSOV,
        val speed: Speed = Speed.DEFAULT,
        val volume: Volume = Volume.DEFAULT,
        override val enableLoggingData: Boolean = false,
        val pinningConfig: PinningConfig? = null
    ) : AbstractYandexTextToSpeech.BaseConfig(apiUrl, voice, enableLoggingData)
}