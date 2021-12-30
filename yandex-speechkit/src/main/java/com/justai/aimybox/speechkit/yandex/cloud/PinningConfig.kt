package com.justai.aimybox.speechkit.yandex.cloud

import java.security.KeyStore

data class PinningConfig(
    val certsKS: KeyStore? = null,
    val serverHostOverride: String = "",
    val protocol: Protocol = Protocol.TLS,
    val pin: String = ""
)
