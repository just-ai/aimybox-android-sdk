package com.justai.aimybox.dialogapi.command

import android.content.Context
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.CustomSkill

class CommandDialogApi(
    context: Context,
    commandResources: List<Int>,
    override val customSkills: LinkedHashSet<CustomSkill<CommandRequest, CommandResponse>>
): DialogApi<CommandRequest, CommandResponse>() {

    private val commands: Map<String, Int>

    init {
        val map = HashMap<String, Int>()

        commandResources.forEach { xml ->
            CommandParser
                .parseCommands(context.resources.getXml(xml))
                .forEach { cmd -> cmd.patterns.forEach { pattern ->
                    map[pattern] = cmd.id
                } }
        }

        commands = map.toSortedMap(reverseOrder())
    }

    override fun createRequest(query: String) = CommandRequest(query)

    override suspend fun send(request: CommandRequest): CommandResponse {
        return commands
            .filterKeys { pattern -> request.query.matches(pattern.toRegex()) }
            .values.firstOrNull()
            .let { CommandResponse(request.query, it ?: 0) }
    }
}