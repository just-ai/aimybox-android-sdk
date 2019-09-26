package com.justai.aimybox.core

import androidx.annotation.CallSuper
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.extensions.contextJob
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

internal abstract class AimyboxComponent(name: String) : CoroutineScope {

    override val coroutineContext = Dispatchers.IO + Job() + CoroutineName("Aimybox($name)")

    val hasRunningJobs: Boolean
        get() = contextJob.children.any(Job::isActive)

    @CallSuper
    open suspend fun cancel() {
       if (hasRunningJobs) contextJob.cancelChildrenAndJoin()
    }
}
