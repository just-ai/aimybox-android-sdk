package com.justai.aimybox.dialogapi.dialogflow

import com.google.cloud.dialogflow.v2.QueryParameters
import com.justai.aimybox.model.Request

data class DialogflowRequest(
    override val query: String,
    val parameters: QueryParameters
) : Request