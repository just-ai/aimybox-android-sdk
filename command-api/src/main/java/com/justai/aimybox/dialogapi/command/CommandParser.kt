package com.justai.aimybox.dialogapi.command

import android.content.res.XmlResourceParser

internal object CommandParser {

    private const val TAG_ACTION = "action"
    private const val TAG_INTENT = "intent"

    private const val ATTR_NAME = "name"
    private const val ATTR_REGEX = "regex"

    fun parseCommands(xml: XmlResourceParser): List<Command> {
        var event: Int
        var command: Command? = null
        val commands = ArrayList<Command>()

        while (xml.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
            when (event) {
                XmlResourceParser.START_TAG -> {
                    when (xml.name) {
                        TAG_ACTION -> {
                            command = Command(xml.getAttributeValue(null, ATTR_NAME), HashMap())
                            commands.add(command)
                        }
                        TAG_INTENT -> {
                            (command?.patterns as HashMap<String, String>).put(
                                xml.getAttributeValue(null, ATTR_REGEX),
                                xml.getAttributeValue(null, ATTR_NAME)
                            )
                        }
                    }
                }
            }
        }

        return commands
    }
}