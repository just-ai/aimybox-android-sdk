package com.justai.aimybox.dialogapi.command

import android.content.Context
import com.justai.aimybox.api.DialogApi
import com.justai.aimybox.core.CustomSkill

class CommandDialogApi(
    context: Context,
    commandResources: List<Int>,
    override val customSkills: LinkedHashSet<CustomSkill<CommandRequest, CommandResponse>>
): DialogApi<CommandRequest, CommandResponse>() {

    private val commands = HashMap<Regex, Command>()

    init {
        commandResources.forEach { xml ->
            CommandParser
                .parseCommands(context.resources.getXml(xml))
                .forEach { cmd -> cmd.patterns.keys.forEach { pattern ->
                    commands[pattern.toRegex()] = cmd
                } }
        }
    }

    override fun createRequest(query: String) = CommandRequest(query)

    override suspend fun send(request: CommandRequest): CommandResponse {
        val regex = commands
            .filterKeys { request.query.matches(it) }
            .keys.firstOrNull()

        val cmd = commands[regex]
        return CommandResponse(request.query, cmd?.action, cmd?.patterns?.get(regex?.pattern))
    }
}