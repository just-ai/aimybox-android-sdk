package com.justai.aimybox.dialogapi.dialogflow

import android.content.Context
import androidx.annotation.RawRes
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2.*
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.model.Request
import com.justai.aimybox.model.Response
import com.justai.aimybox.dialogapi.dialogflow.converter.SimpleResponseConverter.convert
import com.justai.aimybox.dialogapi.dialogflow.converter.ImageResponseConverter.convert
import com.justai.aimybox.dialogapi.dialogflow.converter.SuggestionsResponseConverter.convert
import com.justai.aimybox.dialogapi.dialogflow.converter.BasicCardResponseConverter.convert
import com.justai.aimybox.model.reply.TextReply
import java.util.*

/**
 * Dialogflow dialog api implementation
 *
 * @param context application context
 * @param serviceAccountRes your Dialogflow agent's service account JSON resource
 * @param language the language code of your Dialogflow agent
 */
class DialogflowDialogApi(
    context: Context,
    @RawRes serviceAccountRes: Int,
    private val language: String
): DialogApi {

    private val sessionSettings: SessionsSettings
    private val sessionId = UUID.randomUUID().toString()
    private val session: SessionName

    init {
        val credentials = ServiceAccountCredentials
            .fromStream(context.resources.openRawResource(serviceAccountRes))

        session = SessionName.of(credentials.projectId, sessionId)
        sessionSettings = SessionsSettings.newBuilder()
            .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
            .build()
    }

    override suspend fun send(request: Request): Response {
        val client = SessionsClient.create(sessionSettings)
        val res = try {
            val textInput = TextInput.newBuilder().setText(request.query).setLanguageCode(language)
            val queryInput = QueryInput.newBuilder().setText(textInput).build()
            client.detectIntent(session, queryInput)
        } finally {
            client.close()
        }

        return parseResponse(res)
    }

    private fun parseResponse(res: DetectIntentResponse): Response {
        val qr = res.queryResult
        val replies = qr.fulfillmentMessagesList.mapNotNull { msg ->
            when {
                msg.hasBasicCard() -> convert(msg.basicCard)
                msg.hasSimpleResponses() -> convert(msg.simpleResponses.simpleResponsesList)
                msg.hasImage() -> listOf(convert(msg.image))
                msg.hasSuggestions() -> listOf(convert(msg.suggestions.suggestionsList))
                else -> null
            }
        }
            .flatten()
            .takeIf { it.isNotEmpty() }
            ?: listOf(TextReply(qr.fulfillmentText, null, null))

        return Response(
            query = qr.queryText,
            action = qr.action,
            intent = qr.intent.displayName,
            question = !qr.diagnosticInfo.fieldsMap.containsKey("end_conversation"),
            replies = replies,
            data = qr.parameters)
    }
}