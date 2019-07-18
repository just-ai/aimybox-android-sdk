package com.justai.aimybox.speechkit.houndify

import com.justai.aimybox.core.SpeechToTextException

class HoundifySpeechToTextException(
    message: String? = null,
    cause: Throwable? = null
) : SpeechToTextException(message, cause)