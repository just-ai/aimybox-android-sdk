package com.justai.aimybox.speechkit.yandex.cloud

import android.content.Context

class YandexTextToSpeechV3(
    context: Context,
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    defaultLanguage: Language,
    config: Config
): AbstractYandexTextToSpeech<YandexTextToSpeechV3.Config>(
    context, iAmTokenProvider, folderId, defaultLanguage, config
) {
    override val api: AbstractYandexSynthesisApi<Config> = YandexSynthesisApiV3(iAmTokenProvider, folderId, config)

    data class Config(
        override val apiUrl: String = "tts.api.cloud.yandex.net",
        val apiPort: Int = 443,
        override val voice: Voice = Voice.V3.KUZNETSOV,
        override val enableLoggingData: Boolean = false,
    ) : BaseConfig(apiUrl, voice, enableLoggingData)
}