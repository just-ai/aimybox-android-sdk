package com.justai.aimybox.assistant.api

import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.CustomSkill
import com.justai.aimybox.dialogapi.dummy.DummyCustomSkill
import com.justai.aimybox.dialogapi.dummy.DummyRequest

class DummyDialogApi(
    private val dummyCustomSkill: DummyCustomSkill = DummyCustomSkill()
): DialogApi<DummyRequest, DummyResponse>() {


    override fun createRequest(query: String) = DummyRequest(query)

    override suspend fun send(request: DummyRequest) = DummyResponse(request.query)

    override val customSkills: LinkedHashSet<CustomSkill<DummyRequest, DummyResponse>>
        get() = linkedSetOf(dummyCustomSkill)
}