package com.justai.aimybox.dialogapi.command

import androidx.annotation.RawRes

data class Command(
    @RawRes val id: Int,
    val patterns: List<String>
)