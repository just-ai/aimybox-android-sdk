package com.justai.aimybox.api.aimybox

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.JsonObject
import com.justai.aimybox.logging.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Deprecated("Use only for minApi < 21.", ReplaceWith("RetrofitHttpWorker"))
internal class LegacyHttpWorker(private val apiUrl: String, private val timeout: Int = DEFAULT_TIMEOUT) :
    BaseHttpWorker() {

    companion object {
        private const val POST = "POST"
        private const val DEFAULT_TIMEOUT = 10_000
    }

    private val L = Logger("Aimybox-LegacyHttpWorker")

    override suspend fun requestAsync(request: AimyboxRequest) =
        suspendCancellableCoroutine<JsonObject> { continuation ->
            L.i("\nHTTP POST >>> $apiUrl\n$request\nEND POST")

            lateinit var connection: HttpURLConnection

            try {
                connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = POST
                    connectTimeout = timeout
                    readTimeout = timeout
                    doInput = true
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }

                BufferedWriter(OutputStreamWriter(connection.outputStream, "UTF-8")).apply {
                    write(gsonInstance.toJson(request))
                    close()
                }

                val responseCode = connection.responseCode

                if (responseCode in 300 until 400) {
                    L.e("\nHTTP POST <<< $apiUrl\nCODE $responseCode\nEND POST")
                    continuation.resumeWithException(RuntimeException("LegacyHttpWorker does not support redirects"))
                } else {
                    val reader = BufferedReader(
                        InputStreamReader(
                            if (responseCode >= 400) {
                                connection.errorStream
                            } else {
                                connection.inputStream
                            }
                        )
                    )

                    val result = generateSequence { reader.readLine() }.joinToString("\n")

                    if (responseCode < 400) {
                        continuation.resume(gsonInstance.fromJson(result))
                    } else {
                        continuation.resumeWithException(RuntimeException(result))
                    }
                    L.i("\nHTTP POST <<< $apiUrl\nCODE $responseCode\n$result\nEND POST")
                }
            } catch (e: Throwable) {
                continuation.resumeWithException(RuntimeException(e))
            } finally {
                connection.disconnect()
            }
        }
}