package com.justai.aimybox.extensions

import io.grpc.Metadata

fun Metadata.put(key: String, value: String) {
    val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
    put(metaKey, value)
}