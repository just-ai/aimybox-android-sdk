package com.justai.aimybox.speechkit.tinkoff

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.apache.commons.codec.binary.Base64
import java.util.*

interface TokenProvider {
    fun generateToken() : String
}

class TokenProviderImpl (
    private val apiKey: String,
    private val secretKey: String,
    private val endPoint: String,
    private val timeExpiration : Long = 10 * 60 * 1000
) : TokenProvider {

    override fun generateToken() : String {

        return JWT.create()
            .withKeyId(apiKey)
            .withClaim("aud", endPoint)
            .withExpiresAt(Date(System.currentTimeMillis()+ timeExpiration))
            .sign(Algorithm.HMAC256(Base64.decodeBase64(secretKey)))

    }
}