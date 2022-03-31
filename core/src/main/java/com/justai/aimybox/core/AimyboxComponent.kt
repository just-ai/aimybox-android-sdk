package com.justai.aimybox.core

import androidx.annotation.CallSuper
import com.justai.aimybox.extensions.cancelChildrenAndJoin
import com.justai.aimybox.extensions.contextJob
import com.justai.aimybox.logging.Logger
import kotlinx.coroutines.*

abstract class AimyboxComponent(name: String)  {
//
//    val coroutineContext = Dispatchers.Default + Job() + CoroutineName("Aimybox($name)")
//    val scope = CoroutineScope( coroutineContext)

    protected val logger = Logger(name)

//    val hasRunningJobs: Boolean
//        get() = scope.contextJob.children.any(Job::isActive)
//
//    @CallSuper
//    open suspend fun cancelRunningJob() {
//       if (hasRunningJobs) scope.contextJob.cancelChildrenAndJoin()
//    }
}
