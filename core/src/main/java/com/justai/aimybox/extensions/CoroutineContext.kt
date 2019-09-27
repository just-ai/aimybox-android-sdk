package com.justai.aimybox.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.joinAll
import kotlin.coroutines.CoroutineContext

suspend fun CoroutineContext.cancelChildrenAndJoin() {
    cancelChildren()
    get(Job)?.children?.toList()?.joinAll()
}

val CoroutineScope.contextJob get() = requireNotNull(coroutineContext[Job])