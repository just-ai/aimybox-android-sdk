package com.justai.aimybox.speechkit.tinkoff

import com.justai.aimybox.core.SpeechToTextException
import com.justai.aimybox.core.TextToSpeechException


class TinkoffCloudTextToSpeechException(
    message: String? = null,
    cause: Throwable? = null
) : TextToSpeechException(message, cause)

class TinkoffCloudSpeechToTextException(
    message: String? = null,
    cause: Throwable? = null
) : SpeechToTextException(message, cause)