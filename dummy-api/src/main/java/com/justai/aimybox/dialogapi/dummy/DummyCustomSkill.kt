package com.justai.aimybox.dialogapi.dummy

import android.annotation.SuppressLint
import com.justai.aimybox.Aimybox
import com.justai.aimybox.assistant.api.DummyResponse
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.model.Response
import com.justai.aimybox.model.TextSpeech

open class DummyCustomSkill: CustomSkill<DummyRequest, DummyResponse> {

    override fun canHandle(response: DummyResponse) = true

    @SuppressLint("MissingPermission")
    override suspend fun onResponse(
        response: DummyResponse,
        aimybox: Aimybox,
        defaultHandler: suspend (Response) -> Unit
    ) {
        aimybox.speak(TextSpeech(response.query ?: ""))
    }
}