package com.justai.aimybox.api

import com.justai.aimybox.core.AimyboxComponent
import com.justai.aimybox.core.AimyboxException
import com.justai.aimybox.core.ApiException
import com.justai.aimybox.core.ApiRequestTimeoutException
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
) : AimyboxComponent("API") {

    private val L = Logger("Aimybox-API")

    internal suspend fun send(request: Request): Response? {
        cancel() // Cancel previous request
        val timeout = delegate.getRequestTimeoutMs()

        return try {
            withContext(coroutineContext) {
                val response = async { delegate.send(request) }

                events.send(DialogApi.Event.RequestSent(request))

                withTimeout(timeout) {
                    response.await()
                }.also {
                    events.send(DialogApi.Event.ResponseReceived(it))
                }
            }
        } catch (e: TimeoutCancellationException) {
            val timeoutException = ApiRequestTimeoutException(request, timeout)
            L.e(timeoutException)
            exceptions.send(timeoutException)
        } catch (e: CancellationException) {
            L.w("Request $request was cancelled")
            events.send(DialogApi.Event.RequestCancelled(request))
        } catch (e: Throwable) {
            L.e("Error during request $request:", e)
            exceptions.send(ApiException(cause = e))
        } as? Response
    }

    internal suspend fun setDelegate(dialogApi: DialogApi) {
        if (delegate != dialogApi) {
            cancel()
            delegate.destroy()
            delegate = dialogApi
        }
    }
}
