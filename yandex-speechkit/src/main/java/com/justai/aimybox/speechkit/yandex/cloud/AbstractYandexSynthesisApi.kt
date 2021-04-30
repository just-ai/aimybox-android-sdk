package com.justai.aimybox.speechkit.yandex.cloud

abstract class AbstractYandexSynthesisApi<T: AbstractYandexTextToSpeech.BaseConfig>(
    protected val iAmTokenProvider: IAmTokenProvider,
    protected val folderId: String,
    protected open val config: T,
) {
    abstract suspend fun request(
        text: String,
        language: Language,
        config: T
    ): ByteArray
}