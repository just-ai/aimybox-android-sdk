package com.justai.aimybox.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

suspend inline fun <T> retry(attempts: Int, delay: Long = 0, block: (attempt: Int) -> T): T {
    check(attempts > 0)
    for (attempt in 1..attempts) {
        try {
            return block(attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (attempt == attempts) throw e
        }
        if (delay > 0) delay(delay)
    }
    throw IllegalStateException()
}