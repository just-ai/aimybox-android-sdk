package com.justai.aimybox.logging

import android.util.Log
import com.justai.aimybox.BuildConfig

class Logger(private val tag: String) {

    private fun format(text: String) = "[${Thread.currentThread().name}] $text"

    fun w(text: String) {
        Log.w(tag, format(text))
    }

    fun w(text: String, throwable: Throwable) {
        Log.w(tag, format(text), throwable)
    }

    fun e(text: String) {
        Log.e(tag, format(text))
    }

    fun e(text: String, throwable: Throwable) {
        Log.e(tag, format(text), throwable)
    }

    fun e(throwable: Throwable) = e("", throwable)

    fun d(text: String) {
        Log.d(tag, format(text))
    }

    fun i(text: String) {
        Log.i(tag, format(text))
    }

    fun a(text: String, e: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            throw AssertionError(text, e)
        } else {
            Log.e(tag, text, e)
        }
    }

}