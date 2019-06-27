package com.justai.aimybox.extensions

val Any?.className: String
    get() = this?.let { it::class.java.simpleName } ?: "null"