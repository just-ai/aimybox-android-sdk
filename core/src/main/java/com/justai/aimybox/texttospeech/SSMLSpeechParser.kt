package com.justai.aimybox.texttospeech

import com.justai.aimybox.model.AudioSpeech
import com.justai.aimybox.model.Speech
import com.justai.aimybox.model.TextSpeech
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Simple SSML parser which can extract text and audio speeches from text input
 * */
class SSMLSpeechParser {
    companion object {
        private const val TAG_AUDIO = "audio"

        private const val ATTR_SRC = "src"
    }

    private val factory = XmlPullParserFactory.newInstance()

    /**
     * Split input to text and audio speeches.
     *
     * @return flow of extracted speeches
     * */
    fun extractSSML(text: String): Flow<Speech> {
        val parser = factory.newPullParser()
        return flow {
            text.wrapWithRootTag().reader().use { reader ->
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(reader)

                while (true) {

                    if (parser.eventType == XmlPullParser.END_DOCUMENT) break

                    when (parser.eventType) {
                        XmlPullParser.TEXT -> emit(TextSpeech(parser.text))
                        XmlPullParser.START_TAG -> handleTag(parser.name, parser)
                    }

                    parser.next()
                }
            }
        }
    }

    private suspend fun FlowCollector<Speech>.handleTag(tagName: String, parser: XmlPullParser) {
        when (tagName) {
            TAG_AUDIO -> {
                emit(AudioSpeech.Uri(parser.getAttributeValue(null, ATTR_SRC)))
            }
        }
    }

    // This prevents XmlPullParser from crash when text doesn't have a root tag
    private fun String.wrapWithRootTag() = "<utterance>$this</utterance>"
}