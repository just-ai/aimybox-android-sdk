package com.justai.aimybox.speechkit.yandex.cloud

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.okhttp.OkHttpChannelBuilder
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

internal object PinnedChannelBuilder {
    fun build(
        host: String, port: Int,
        pinningConfig: PinningConfig? = null
    ): ManagedChannel {
        val channelBuilder: ManagedChannelBuilder<*> = ManagedChannelBuilder.forAddress(host, port)
            .maxInboundMessageSize(16 * 1024 * 1024)

        if (pinningConfig == null) return channelBuilder.build()
        val serverHostOverride = pinningConfig.serverHostOverride
        val certsKS = pinningConfig.certsKS
        val protocol = pinningConfig.protocol

        if (serverHostOverride != null) {
            // Force the hostname to match the cert the server uses.
            channelBuilder.overrideAuthority(serverHostOverride)
        }
        try {
            channelBuilder.useTransportSecurity()
            val socketFactory = getSslSocketFactory(certsKS, protocol)
            (channelBuilder as OkHttpChannelBuilder).sslSocketFactory(socketFactory)
        } catch (t: Throwable) {
            return channelBuilder.build()
        }
        return channelBuilder.build()
    }

    private fun getSslSocketFactory(certsKS: KeyStore, protocol: Protocol): SSLSocketFactory {
        // initialize trust manager factor from certs keystore
        val tmf: TrustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(certsKS)

        // initialize SSL context from trust manager factory
        val context: SSLContext = SSLContext.getInstance(protocol.stringValue)
        context.init(null, tmf.trustManagers, null)

        // return socket factory from the SSL context
        return context.socketFactory
    }
}

enum class Protocol(val stringValue: String) {
    TLS("TLS"),
    SSL("SSL")
}