package com.justai.aimybox.speechkit.yandex.cloud

import android.content.Context

class YandexTextToSpeechV1(
    context: Context,
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    defaultLanguage: Language,
    config: YandexTextToSpeech.Config = YandexTextToSpeech.Config()
): AbstractYandexTextToSpeech<YandexTextToSpeech.Config>(
    context, iAmTokenProvider, folderId, defaultLanguage, config
) {
    override val api: AbstractYandexSynthesisApi = YandexSynthesisApi(iAmTokenProvider, folderId, config)

    fun changeConfig(config: YandexTextToSpeech.Config) {
        (api as YandexSynthesisApi).config = config
    }
}