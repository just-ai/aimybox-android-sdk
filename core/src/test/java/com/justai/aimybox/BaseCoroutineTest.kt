package com.justai.aimybox

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlin.test.fail

abstract class BaseCoroutineTest : CoroutineScope {

    init {
        mockLog()
    }

    final override val coroutineContext =
        newSingleThreadContext("Test") + CoroutineExceptionHandler { coroutineContext, throwable ->
            when (throwable) {
                is CancellationException -> println("Coroutine in $coroutineContext is cancelled")
                else -> fail(throwable.toString())
            }
        }

    fun runInTestContext(block: suspend CoroutineScope.() -> Unit) = runBlocking(coroutineContext, block)
}