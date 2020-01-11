package com.justai.aimybox.dialogapi.command

import android.content.res.XmlResourceParser

object CommandParser {

    private const val TAG_COMMAND = "command"
    private const val TAG_PATTERN = "pattern"

    private const val ATTR_ID = "id"
    private const val ATTR_VALUE = "value"

    fun parseCommands(xml: XmlResourceParser): List<Command> {
        var event: Int
        var command: Command? = null
        val commands = ArrayList<Command>()

        while (xml.next().also { event = it } != XmlResourceParser.END_DOCUMENT) {
            when (event) {
                XmlResourceParser.START_TAG -> {
                    when (xml.name) {
                        TAG_COMMAND -> {
                            command = Command(xml.getAttributeResourceValue(null, ATTR_ID, 0), ArrayList())
                            commands.add(command)
                        }
                        TAG_PATTERN -> {
                            (command?.patterns as ArrayList<String>).add(
                                xml.getAttributeValue(null, ATTR_VALUE)
                            )
                        }
                    }
                }
            }
        }

        return commands
    }
}