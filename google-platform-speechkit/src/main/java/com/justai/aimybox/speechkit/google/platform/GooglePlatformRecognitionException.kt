package com.justai.aimybox.speechkit.google.platform

import com.justai.aimybox.core.SpeechToTextException

class GooglePlatformRecognitionException(code: Int, cause: Throwable? = null) : SpeechToTextException(cause)