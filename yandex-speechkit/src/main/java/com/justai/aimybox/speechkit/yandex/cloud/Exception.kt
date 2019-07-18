package com.justai.aimybox.speechkit.yandex.cloud

import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.core.TextToSpeechException

class YandexCloudTextToSpeechException(
    message: String? = null,
    cause: Throwable? = null
) : TextToSpeechException(message, cause)

class YandexCloudSpeechToTextException(
    message: String? = null,
    cause: Throwable? = null
) : SpeechToTextException(message, cause)