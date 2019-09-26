package com.justai.aimybox.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

suspend inline fun <T> retry(times: Int, delay: Long = 0, block: (attempt: Int) -> T): T {
    check(times > 0)
    for (attempt in 1..times) {
        try {
            return block(attempt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (attempt == times) throw e
        }
        if (delay > 0) delay(delay)
    }
    throw IllegalStateException()
}