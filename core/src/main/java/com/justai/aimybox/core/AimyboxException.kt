package com.justai.aimybox.core

import com.justai.aimybox.Aimybox
import com.justai.aimybox.model.Request
import java.io.IOException

/**
 * Base class for [Aimybox] exceptions. This class is sealed, to determine which component produced the exception,
 * it is recommended to use when construction:
 * ```
 *  fun handleException(exception: AimyboxException) {
 *      when (exception) {
 *          is SpeechToTextException -> { ... }
 *          is TextToSpeechException -> { ... }
 *          { ... }
 *      }
 *  }
 * ```
 *
 * @see [Aimybox.exceptions]
 * @see SpeechToTextException
 * @see TextToSpeechException
 * @see VoiceTriggerException
 * @see ApiException
 * */
sealed class AimyboxException(message: String? = null, cause: Throwable?) : IOException(message, cause)

/**
 *
 * Exceptions occurred during speech recognition.
 * @see [Aimybox.exceptions]
 * */
open class SpeechToTextException(message: String? = null, cause: Throwable? = null) : AimyboxException(message, cause)
/**
 * Exceptions occurred during speech synthesis.
 *
 * @see [Aimybox.exceptions]
 * */
open class TextToSpeechException(message: String? = null, cause: Throwable? = null) : AimyboxException(message, cause)

/**
 * Exceptions occurred in voice trigger component.
 *
 * @see [Aimybox.exceptions]
 * */
open class VoiceTriggerException(message: String? = null, cause: Throwable? = null) : AimyboxException(message, cause)
/**
 * Exceptions occurred during dialog api communication.
 *
 * @see [Aimybox.exceptions]
 * */
open class ApiException(message: String? = null, cause: Throwable? = null) : AimyboxException(message, cause)

class RecognitionTimeoutException : SpeechToTextException()

class ApiRequestTimeoutException(
    request: Request,
    timeout: Long
) : ApiException("Request timeout: $request. Server didn't respond within $timeout ms.")
