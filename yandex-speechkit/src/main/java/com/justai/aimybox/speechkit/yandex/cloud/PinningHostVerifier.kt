package com.justai.aimybox.speechkit.yandex.cloud

import io.grpc.okhttp.internal.OkHostnameVerifier
import okhttp3.CertificatePinner
import java.security.cert.Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

class PinningHostVerifier constructor( private val certificatePinner: CertificatePinner) : HostnameVerifier {

    private val baseHostVerifier = OkHostnameVerifier.INSTANCE

    override fun verify(hostname: String, session: SSLSession): Boolean {

        val sertificates : List<Certificate> = session.peerCertificates.toList()
        certificatePinner.check(hostname, sertificates)
        return baseHostVerifier.verify(hostname, session)
    }
}