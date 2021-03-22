package com.justai.aimybox

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

fun mockLog(){
    mockkStatic(Log::class)
    every { Log.isLoggable(any(), any()) } returns true
    every { Log.v(any(), any()) } answers { args.drop(1).forEach { println("[VERBOSE] $it") }; 0 }
    every { Log.d(any(), any()) } answers { args.drop(1).forEach { println("[DEBUG] $it") }; 0 }
    every { Log.i(any(), any()) } answers { args.drop(1).forEach { println("[INFO] $it") }; 0 }
    every { Log.w(any(), any<String>()) } answers { args.drop(1).forEach { println("[WARN] $it") }; 0 }
    every { Log.w(any(), any<Throwable>()) } answers { args.drop(1).forEach { println("[WARN] $it") }; 0 }
    every { Log.e(any(), any()) } answers { args.drop(1).forEach { println("[ERROR] $it") }; 0 }
    every { Log.e(any(), any(), any()) } answers { args.drop(1).forEach { println("[ERROR] $it") }; 0 }
    every { Log.getStackTraceString(any()) } returns ""
}