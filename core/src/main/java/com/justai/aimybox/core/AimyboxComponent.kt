package com.justai.aimybox.core

import androidx.annotation.CallSuper
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.extensions.contextJob
import com.justai.aimybox.logging.Logger
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

abstract class AimyboxComponent(name: String) : CoroutineScope {

    override val coroutineContext = Dispatchers.IO + Job() + CoroutineName("Aimybox($name)")

    protected val L = Logger(name)

    val hasRunningJobs: Boolean
        get() = contextJob.children.any(Job::isActive)

    @CallSuper
    open suspend fun cancelRunningJob() {
       if (hasRunningJobs) contextJob.cancelChildrenAndJoin()
    }
}
