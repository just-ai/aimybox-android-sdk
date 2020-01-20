package com.justai.aimybox.dialogapi.command

data class Command(
    val action: String,
    val patterns: Map<String, String>
)