package com.justai.aimybox

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic

fun mockLog(){
    mockkStatic(Log::class)
    every { Log.v(any(), any()) } returns 0
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
    every { Log.e(any(), any(), any()) } returns 0
    every { Log.getStackTraceString(any()) } returns ""
}