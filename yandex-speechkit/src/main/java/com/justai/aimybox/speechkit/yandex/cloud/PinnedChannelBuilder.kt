package com.justai.aimybox.speechkit.yandex.cloud

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import okhttp3.CertificatePinner
import okhttp3.HttpUrl
import java.security.KeyStore
import java.security.cert.Certificate
import java.security.cert.CertificateExpiredException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
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

        var pinningHost = host
        if (serverHostOverride.isNotEmpty()) {
            // Force the hostname to match the cert the server uses.
            channelBuilder.overrideAuthority(serverHostOverride)
            pinningHost = serverHostOverride
        }
        try {
            channelBuilder.useTransportSecurity()

            if (certsKS != null) {
                val socketFactory = getSslSocketFactory(certsKS, protocol)
                (channelBuilder as OkHttpChannelBuilder).sslSocketFactory(socketFactory)
            } else {
                if (pinningConfig.pin.isNotEmpty()) {
                    val certificatePinner: CertificatePinner = CertificatePinner.Builder()
                        .add(pinningHost, pinningConfig.pin)
                        .build()
                    val pinningHostVerifier = PinningHostVerifier(certificatePinner)
                    (channelBuilder as OkHttpChannelBuilder).apply {
                        hostnameVerifier(pinningHostVerifier)
                    }
                } else {
                    GlobalScope.launch(Main) {
                        val deferred =
                            getRemoteCertificate(pinningHost)
                        val certificate = deferred.await()
                        certificate ?: throw CertificateExpiredException()
                        val ks = KeyStore.getInstance(KeyStore.getDefaultType())
                        ks.load(null, null)
                        ks.setCertificateEntry("s0", certificate)
                        val socketFactory = getSslSocketFactory(ks, protocol)
                        (channelBuilder as OkHttpChannelBuilder).sslSocketFactory(socketFactory)
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
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


    private fun validateCertificates(certificate: X509Certificate): Boolean =
        try {
            certificate.checkValidity()
            true
        } catch (e: Throwable) {
            false
        }

    //TODO put it to separate block. These is very special code. Try don't use it.
    private fun getRemoteCertificate(aURL: String): Deferred<Certificate?> =
        GlobalScope.async(IO) {
            val destinationURL = HttpUrl.Builder()
                .scheme("https")
                .host(aURL)
                .build()
            val conn = destinationURL.toUrl().openConnection() as HttpsURLConnection
            conn.connect()
            conn.serverCertificates.find { cert ->
                if (cert is X509Certificate && validateCertificates(cert)) {
                    cert.issuerDN.name.contains("OU=Yandex Certification Authority")
                } else {
                    false
                }
            }
        }
}

enum class Protocol(val stringValue: String) {
    TLS("TLS"),
    SSL("SSL")
}