package com.justai.aimybox.api

import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.ApiException
import com.justai.aimybox.logging.Logger
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class DialogApiComponent(
    private var delegate: DialogApi,
    private val events: SendChannel<DialogApi.Event>,
    private val exceptions: SendChannel<AimyboxException>
): AimyboxComponent("API") {

    private val L = Logger("Aimybox-API")

    internal suspend fun send(request: Request): Response? {
        cancel() // Cancel previous request
        return withContext(coroutineContext) {
            val response = try {
                val deferred = sendAsync(request)
                events.send(DialogApi.Event.Send(request))
                withTimeout(delegate.getRequestTimeoutMs()) {
                    deferred.await()
                }
            } catch (e: TimeoutCancellationException) {
                L.e("Request timeout: $request. Server didn't respond within ${delegate.getRequestTimeoutMs()} ms.")
            } catch (e: CancellationException) {
                L.w("Request $request was cancelled")
            } catch (e: Throwable) {
                L.e("Error during request $request:", e)
                exceptions.send(ApiException(e))
            } as? Response

            events.send(DialogApi.Event.Receive(response))

            response
        }
    }

    private fun sendAsync(request: Request) = async { delegate.send(request) }

    internal fun setDelegate(dialogApi: DialogApi) {
        if (delegate != dialogApi) {
            cancel()
            delegate.destroy()
            delegate = dialogApi
        }
    }
}
