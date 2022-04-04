package com.justai.aimybox.api.aimybox

import com.github.salomonbrys.kotson.nullString
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.justai.aimybox.model.TextSpeech
import com.justai.aimybox.model.reply.AudioReply
import com.justai.aimybox.model.reply.Reply
import com.justai.aimybox.model.reply.TextReply
import com.justai.aimybox.model.reply.aimybox.UnknownAimyboxReply
import com.justai.aimybox.model.reply.asTextSpeech
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


data class StackReply(
    @SerializedName("content")
    @Expose
    val content: String,
    val textToSpeech: String?
) : TextReply(textToSpeech)


internal class AimyboxUtilsTest {

    private lateinit var replyTypes: Map<String, Class<out Reply>>
    private lateinit var jsonObject: JsonObject

    private val jsonData =
        "{ \"query\":\"Confirmed\", \"text\":\"\", \"question\":false," +
                " \"replies\":[ { \"type\":\"va_stack\", \"content\":\"Simplified\" } ]," +
                " \"data\":{ \"class\":\"/aaa/bbbb\", \"confidence\":0.1, \"emotion\":\"SUCCESS\"," +
                " \"newSession\":false, \"sessionId\":\"a1\" } }"

    @Before
    fun setUp() {
        replyTypes = mapOf("va_stack" to StackReply::class.java)
        jsonObject = JsonParser().parse(jsonData).asJsonObject

    }

    @Test
    fun `parse simple json`() {

        assertTrue(jsonObject.isJsonObject)

        val jsonArray = jsonObject.get("replies")?.takeIf(JsonElement::isJsonArray)?.asJsonArray

        assertNotNull(jsonArray)

        val replays = jsonArray.filterIsInstance(JsonObject::class.java).map { jsonObject ->
            val type = jsonObject["type"].nullString
            val replyClass = replyTypes[type] ?: UnknownAimyboxReply::class.java
            replyClass.constructors.first().newInstance(jsonObject["content"].asString, null)
        }

        val reply = replays[0] as StackReply

        assertNotNull(reply.text)
        assertTrue(reply.text.isEmpty())

        val textSpeech = reply.asTextSpeech()
        assertNotNull(textSpeech.text)
    }

    //Test is failed. Gson doesn't interpret Kotlin constructors properly.
    @Test
    fun `parse reply via gson`(){

        assertTrue(jsonObject.isJsonObject)

        val jsonArray = jsonObject.get("replies")?.takeIf(JsonElement::isJsonArray)?.asJsonArray

        assertNotNull(jsonArray)
        val gson = Gson()
        val stackModel = gson.fromJson(jsonArray[0], StackReply::class.java)

       assertNull(stackModel.text)

        val textSpeech = stackModel.asTextSpeech()

        assertNotNull(textSpeech.text)
        assertTrue(textSpeech.text.isEmpty())

    }


}