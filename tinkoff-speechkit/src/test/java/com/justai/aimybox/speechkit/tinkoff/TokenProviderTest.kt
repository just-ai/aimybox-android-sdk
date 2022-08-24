package com.justai.aimybox.speechkit.tinkoff

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*

import org.junit.Test

class TokenProviderTest {

    @Test
    fun generateToken() = runBlocking {
        val tokenGenerator = TokenProviderImpl(
            "fffffff",
            "ssdsdsdsds",
        "dddd/dddf//fff",
        )
        val token = tokenGenerator.generateToken()
        assertFalse(token.isEmpty())
    }
}