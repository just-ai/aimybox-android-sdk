package com.justai.aimybox.speechkit

import com.justai.aimybox.core.TextToSpeechException

class JustAITextToSpeechException(message: String? = null, cause: Throwable? = null) :
    TextToSpeechException(message, cause) {
}