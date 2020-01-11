package com.justai.aimybox.dialogapi.command

import com.justai.aimybox.model.Request

data class CommandRequest(
    override val query: String
): Request