package com.justai.aimybox.logging

import android.os.Build
import android.util.Log
import com.justai.aimybox.BuildConfig

class Logger(
    tag: String = "",
    private val debug: Boolean = BuildConfig.DEBUG,
    private val messageFormatter: (any: Any?) -> String = DEFAULT_FORMAT
) {

    companion object {
        internal val DEFAULT_FORMAT = { any: Any? -> "[${Thread.currentThread().name}] $any" }
    }

    private val formattedTag = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N)
        tag.take(14)
    else tag

    private val tag = "Aimybox" + if (formattedTag.isNotBlank()) "($formattedTag)" else ""

    // Verbose
    fun v(throwable: Throwable) = v("", throwable)

    fun v(any: Any?) {
        if (Log.isLoggable(tag, Log.VERBOSE))
            Log.v(tag, messageFormatter(any))
    }

    fun v(any: Any?, throwable: Throwable) {
        if (Log.isLoggable(tag, Log.VERBOSE))
            Log.v(tag, messageFormatter(any), throwable)
    }

    // Debug
    fun d(throwable: Throwable) {
        if (debug) d("", throwable)
    }

    fun d(any: Any?) {
        if (debug) Log.d(tag, messageFormatter(any))
    }

    fun d(any: Any?, throwable: Throwable) {
        if (debug) Log.d(tag, messageFormatter(any), throwable)
    }

    // Info
    fun i(throwable: Throwable) = i("", throwable)

    fun i(any: Any?) {
        if (Log.isLoggable(tag, Log.INFO))
            Log.i(tag, messageFormatter(any))
    }

    fun i(any: Any?, throwable: Throwable) {
        if (Log.isLoggable(tag, Log.INFO))
            Log.i(tag, messageFormatter(any), throwable)
    }

    // Warning
    fun w(throwable: Throwable) = w("", throwable)

    fun w(any: Any?) {
        if (Log.isLoggable(tag, Log.WARN))
            Log.w(tag, messageFormatter(any))
    }

    fun w(any: Any?, throwable: Throwable) {
        if (Log.isLoggable(tag, Log.WARN))
            Log.w(tag, messageFormatter(any), throwable)
    }

    // Error
    fun e(throwable: Throwable) = e("", throwable)

    fun e(any: Any?) {
        if (Log.isLoggable(tag, Log.ERROR))
            Log.e(tag, messageFormatter(any))
    }

    fun e(any: Any?, throwable: Throwable) {
        if (Log.isLoggable(tag, Log.ERROR))
            Log.e(tag, messageFormatter(any), throwable)
    }

    // Assert
    fun assert(condition: Boolean, e: Throwable? = null, lazyMessage: () -> Any? = { "" }) {
        if (condition) return
        if (debug) {
            throw AssertionError(messageFormatter(lazyMessage()), e)
        } else {
            Log.e(tag, messageFormatter(lazyMessage()), e)
        }
    }

    //Ping
    @Deprecated("Only for debugging.")
    fun ping() {
        getCallSite()?.apply {
            d("*PING* ($className.$methodName:$lineNumber)")
        }
    }

    private fun getCallSite() = try {
        Thread.currentThread().stackTrace.firstOrNull {

            !it.isNativeMethod
                    && it.className != Thread::class.java.name
                    && it.className != Logger::class.java.name
        }
    } catch (e: Throwable) {
        e(e)
        null
    }
}