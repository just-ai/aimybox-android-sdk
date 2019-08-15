package com.justai.aimybox.core

import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking

internal abstract class AimyboxComponent(name: String) : CoroutineScope {
    private  var job = Job()

    val hasRunningJobs: Boolean
        get() = job.children.any(Job::isActive)

    override val coroutineContext = Dispatchers.IO + job + CoroutineName("Aimybox Component $name")

    @CallSuper
    open fun cancel() = runBlocking {
        job.cancelChildren()
        job.children.toList().joinAll()
    }
}
