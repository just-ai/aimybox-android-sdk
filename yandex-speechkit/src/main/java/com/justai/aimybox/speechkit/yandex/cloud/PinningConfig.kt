package com.justai.aimybox.speechkit.yandex.cloud

import java.security.KeyStore

data class PinningConfig(
    val certsKS: KeyStore,
    val serverHostOverride: String? = null,
    val protocol: Protocol = Protocol.TLS
)
