package com.justai.aimybox.speechkit.yandex.cloud

import android.content.Context

class YandexTextToSpeechV3(
    context: Context,
    iAmTokenProvider: IAmTokenProvider,
    folderId: String,
    defaultLanguage: Language,
    config: YandexTextToSpeech.ConfigV3 = YandexTextToSpeech.ConfigV3()
): AbstractYandexTextToSpeech<YandexTextToSpeech.ConfigV3>(
    context, iAmTokenProvider, folderId, defaultLanguage, config
) {
    override val api: AbstractYandexSynthesisApi = YandexSynthesisApiV3(iAmTokenProvider, folderId, config)
}