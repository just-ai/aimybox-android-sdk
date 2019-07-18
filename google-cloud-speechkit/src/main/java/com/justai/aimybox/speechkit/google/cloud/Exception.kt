package com.justai.aimybox.speechkit.google.cloud

import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.core.TextToSpeechException

class GoogleCloudTextToSpeechException(
    message: String? = null,
    cause: Throwable? = null
) : TextToSpeechException(message, cause)

class GoogleCloudSpeechToTextException(
    message: String? = null,
    cause: Throwable? = null
) : SpeechToTextException(message, cause)