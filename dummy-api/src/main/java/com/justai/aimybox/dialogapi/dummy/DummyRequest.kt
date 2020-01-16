package com.justai.aimybox.dialogapi.dummy

import com.justai.aimybox.model.Request

data class DummyRequest(
    override val query: String
): Request