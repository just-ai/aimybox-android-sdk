package com.justai.aimybox.speechkit.google.platform

import com.justai.aimybox.core.SpeechToTextException

class GooglePlatformSpeechToTextException(
    val code: Int,
    message: String? = null,
    cause: Throwable? = null
) : SpeechToTextException("Exception [$code]: ${message.orEmpty()}", cause)


class GooglePlatformTextToSpeechException(
    val code: Int? = null,
    message: String? = null,
    cause: Throwable? = null
) : SpeechToTextException(message, cause)