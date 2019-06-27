package com.justai.aimybox.core

import com.justai.aimybox.Aimybox

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
sealed class AimyboxException(cause: Throwable?) : RuntimeException(cause)

/**
 *
 * Exceptions occurred during speech recognition.
 * @see [Aimybox.exceptions]
 * */
open class SpeechToTextException(cause: Throwable? = null) : AimyboxException(cause)
/**
 * Exceptions occurred during speech synthesis.
 *
 * @see [Aimybox.exceptions]
 * */
open class TextToSpeechException(cause: Throwable? = null) : AimyboxException(cause)

/**
 * Exceptions occurred in voice trigger component.
 *
 * @see [Aimybox.exceptions]
 * */
open class VoiceTriggerException(cause: Throwable? = null) : AimyboxException(cause)
/**
 * Exceptions occurred during dialog api communication.
 *
 * @see [Aimybox.exceptions]
 * */
open class ApiException(cause: Throwable? = null) : AimyboxException(cause)

class RecognitionTimeoutException : SpeechToTextException()
