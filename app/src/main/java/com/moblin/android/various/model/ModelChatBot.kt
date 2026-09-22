package com.moblin.android.various.model

import com.moblin.android.integrations.openai.OpenAi
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.ChatBotAiArgument
import com.moblin.android.various.ChatBotCommand
import com.moblin.android.various.ChatBotFilterArgument
import com.moblin.android.various.ChatBotGimbalArgument
import com.moblin.android.various.ChatBotLocationArgument
import com.moblin.android.various.ChatBotLocationDataArgument
import com.moblin.android.various.ChatBotMacroArgument
import com.moblin.android.various.ChatBotMainArgument
import com.moblin.android.various.ChatBotMapArgument
import com.moblin.android.various.ChatBotMapZoomArgument
import com.moblin.android.various.ChatBotMessage
import com.moblin.android.various.ChatBotMusicArgument
import com.moblin.android.various.ChatBotObsArgument
import com.moblin.android.various.ChatBotOnOffArgument
import com.moblin.android.various.ChatBotReactionArgument
import com.moblin.android.various.ChatBotStreamArgument
import com.moblin.android.various.ChatBotTeslaArgument
import com.moblin.android.various.ChatBotTeslaMediaArgument
import com.moblin.android.various.ChatBotTeslaTrunkArgument
import com.moblin.android.various.ChatBotTwitchArgument
import com.moblin.android.various.ChatBotWidgetArgument
import com.moblin.android.various.ChatBotWidgetTimerArgument
import com.moblin.android.various.ChatBotWidgetWheelOfLuckArgument
import com.moblin.android.various.settings.SettingsChatBotPermissionsCommand
import com.moblin.android.various.settings.SettingsMacrosMacro
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsReaction
import com.moblin.android.various.settings.SettingsVideoEffectType
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.videoeffects.text.isGForceVariable
import com.moblin.android.videoeffects.text.isGeographyVariable
import com.moblin.android.videoeffects.text.isWeatherVariable
import com.moblin.android.videoeffects.text.loadTextFormat
import java.net.URI
import java.time.Duration
import java.time.Instant

private val punctuationRegex = Regex("\\p{P}")

private fun formatAiAnswer(user: String, question: String, answer: String): String {
    val questionWithPunctuation = if (question.lastOrNull()?.let {
            punctuationRegex.matches(it.toString())
        } == true
    ) {
        question
    } else {
        "$question,"
    }
    return localized("$user asked: $questionWithPunctuation Answer: $answer")
}

fun Model.executeChatBotMessage() {
    val message = chatBotMessages.removeFirstOrNull() ?: return
    handleChatBotMessage(message = message)
}

private fun Model.handleChatBotMessage(message: ChatBotMessage) {
    val command = ChatBotCommand(message = message, aliases = database.chat.aliases) ?: return
    val mainCommand = command.popFirstArgument<ChatBotMainArgument>() ?: return
    when (mainCommand) {
        ChatBotMainArgument.help -> handleChatBotMessageHelp(platform = message.platform)
        ChatBotMainArgument.tts -> handleChatBotMessageTts(command = command)
        ChatBotMainArgument.obs -> handleChatBotMessageObs(command = command)
        ChatBotMainArgument.map -> handleChatBotMessageMap(command = command)
        ChatBotMainArgument.location -> handleChatBotMessageLocation(command = command)
        ChatBotMainArgument.snapshot -> {
            if (command.peekFirst() == null) {
                handleChatBotMessageSnapshot(command = command)
            } else {
                handleChatBotMessageSnapshotWithMessage(command = command)
            }
        }
        ChatBotMainArgument.mute -> handleChatBotMessageMute(command = command)
        ChatBotMainArgument.unmute -> handleChatBotMessageUnmute(command = command)
        ChatBotMainArgument.alert -> handleChatBotMessageAlert(command = command)
        ChatBotMainArgument.fax -> handleChatBotMessageFax(command = command)
        ChatBotMainArgument.filter -> handleChatBotMessageFilter(command = command)
        ChatBotMainArgument.zoom -> handleChatBotMessageZoom(command = command)
        ChatBotMainArgument.say -> handleChatBotMessageTtsSay(command = command)
        ChatBotMainArgument.tesla -> handleChatBotMessageTesla(command = command)
        ChatBotMainArgument.reaction -> handleChatBotMessageReaction(command = command)
        ChatBotMainArgument.scene -> handleChatBotMessageScene(command = command)
        ChatBotMainArgument.stream -> handleChatBotMessageStream(command = command)
        ChatBotMainArgument.widget -> handleChatBotMessageWidget(command = command)
        ChatBotMainArgument.ai -> handleChatBotMessageAi(command = command)
        ChatBotMainArgument.twitch -> handleChatBotMessageTwitch(command = command)
        ChatBotMainArgument.gimbal -> handleChatBotMessageGimbal(command = command)
        ChatBotMainArgument.macro -> handleChatBotMessageMacro(command = command)
        ChatBotMainArgument.send -> handleChatBotMessageSend(command = command)
        ChatBotMainArgument.music -> handleChatBotMessageMusic(command = command)
        ChatBotMainArgument.custom -> handleChatBotMessageCustom(command = command)
        else -> Unit
    }
}

private fun Model.handleChatBotMessageTts(command: ChatBotCommand) {
    when (command.popFirstArgument<ChatBotOnOffArgument>()) {
        ChatBotOnOffArgument.on -> handleChatBotMessageTtsOn(command = command)
        ChatBotOnOffArgument.off -> handleChatBotMessageTtsOff(command = command)
        else -> Unit
    }
}

private fun Model.handleChatBotMessageObs(command: ChatBotCommand) {
    if (command.popFirstArgument<ChatBotObsArgument>() == ChatBotObsArgument.fix) {
        handleChatBotMessageObsFix(command = command)
    }
}

private fun Model.handleChatBotMessageMap(command: ChatBotCommand) {
    if (command.popFirstArgument<ChatBotMapArgument>() == ChatBotMapArgument.zoom) {
        if (command.popFirstArgument<ChatBotMapZoomArgument>()?.rawValue == "out") {
            handleChatBotMessageMapZoomOut(command = command)
        }
    }
}

private fun Model.handleChatBotMessageLocation(command: ChatBotCommand) {
    if (command.popFirstArgument<ChatBotLocationArgument>()?.rawValue != "data") {
        return
    }
    when (command.popFirstArgument<ChatBotLocationDataArgument>()) {
        ChatBotLocationDataArgument.reset -> handleChatBotMessageLocationDataReset(command = command)
        ChatBotLocationDataArgument.split -> handleChatBotMessageLocationDataSplit(command = command)
        else -> Unit
    }
}

private fun Model.handleChatBotMessageHelp(platform: Platform) {
    sendChatBotReply(
        message = localized("Moblin chat bot help: https://moblin.app/chat-bot/"),
        platform = platform
    )
}

private fun Model.handleChatBotMessageTtsOn(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.tts,
        command = command,
        onCompleted = {
            makeToast(
                title = localized("Chat bot"),
                subTitle = localized("Turning on chat text to speech")
            )
            database.chat.textToSpeechEnabled = true
        }
    )
}

private fun Model.handleChatBotMessageTtsOff(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.tts,
        command = command,
        onCompleted = {
            makeToast(
                title = localized("Chat bot"),
                subTitle = localized("Turning off chat text to speech")
            )
            database.chat.textToSpeechEnabled = false
            chatTextToSpeech.reset(running = true)
        }
    )
}

private fun Model.handleChatBotMessageTtsSay(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.tts,
        command = command,
        onCompleted = {
            val user = command.user() ?: "Unknown"
            chatTextToSpeech.say(
                messageId = null,
                user = user,
                userId = null,
                message = command.rest(),
                isRedemption = false
            )
        }
    )
}

private fun Model.handleChatBotMessageObsFix(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.fix,
        command = command,
        onCompleted = {
            if (obsWebSocket != null) {
                makeToast(
                    title = localized("Chat bot"),
                    subTitle = localized("Fixing OBS input")
                )
                obsFixStream()
            } else {
                makeErrorToast(
                    title = localized("Chat bot"),
                    subTitle = localized(
                        "Cannot fix OBS input. OBS remote control is not configured."
                    )
                )
            }
        }
    )
}

private fun Model.handleChatBotMessageMapZoomOut(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.map,
        command = command,
        onCompleted = {
            makeToast(
                title = localized("Chat bot"),
                subTitle = localized("Zooming out map")
            )
            for (mapEffect in mapEffects.values) {
                mapEffect.zoomOutTemporarily()
            }
        }
    )
}

private fun Model.handleChatBotMessageLocationDataReset(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.location,
        command = command,
        onCompleted = {
            resetLocationData()
        }
    )
}

private fun Model.handleChatBotMessageLocationDataSplit(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.location,
        command = command,
        onCompleted = {
            resetSplitLocationData()
        }
    )
}

private fun Model.handleChatBotMessageSnapshot(command: ChatBotCommand) {
    val permissions = database.chat.botCommandPermissions.snapshot
    executeIfUserAllowedToUseChatBot(
        permissions = permissions,
        command = command,
        onCompleted = {
            val user = command.user()
            if (user != null) {
                if (permissions.sendChatMessages) {
                    sendChatBotReply(
                        message = formatSnapshotTakenSuccessfully(user = user),
                        platform = command.message.platform
                    )
                }
                takeSnapshot(isChatBot = true, message = formatSnapshotTakenBy(user = user))
            } else {
                takeSnapshot(isChatBot = true)
            }
        },
        onNotAllowed = {
            val user = command.user()
            if (permissions.sendChatMessages && user != null) {
                sendChatBotReply(
                    message = formatSnapshotTakenNotAllowed(user = user),
                    platform = command.message.platform
                )
            }
        }
    )
}

private fun Model.handleChatBotMessageSnapshotWithMessage(command: ChatBotCommand) {
    val permissions = database.chat.botCommandPermissions.snapshot
    executeIfUserAllowedToUseChatBot(
        permissions = permissions,
        command = command,
        onCompleted = {
            val user = command.user()
            if (permissions.sendChatMessages && user != null) {
                sendChatBotReply(
                    message = formatSnapshotTakenSuccessfully(user = user),
                    platform = command.message.platform
                )
            }
            takeSnapshotWithCountdown(
                isChatBot = true,
                message = command.rest(),
                user = command.user()
            )
        },
        onNotAllowed = {
            val user = command.user()
            if (permissions.sendChatMessages && user != null) {
                sendChatBotReply(
                    message = formatSnapshotTakenNotAllowed(user = user),
                    platform = command.message.platform
                )
            }
        }
    )
}

private fun Model.handleChatBotMessageMute(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.audio,
        command = command,
        onCompleted = {
            if (!audio.muted.value) {
                makeToast(
                    title = localized("Chat bot"),
                    subTitle = localized("Muting audio")
                )
                setMuted(value = true)
                setQuickButton(type = SettingsQuickButtonType.mute, isOn = true)
            }
        }
    )
}

private fun Model.handleChatBotMessageUnmute(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.audio,
        command = command,
        onCompleted = {
            if (audio.muted.value) {
                makeToast(
                    title = localized("Chat bot"),
                    subTitle = localized("Unmuting audio")
                )
                setMuted(value = false)
                setQuickButton(type = SettingsQuickButtonType.mute, isOn = false)
            }
        }
    )
}

private fun Model.handleChatBotMessageAi(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.ai,
        command = command,
        onCompleted = {
            when (command.popFirstArgument<ChatBotAiArgument>()) {
                ChatBotAiArgument.ask -> handleChatBotMessageAiAsk(command = command)
                else -> Unit
            }
        }
    )
}

private fun Model.handleChatBotMessageAiAsk(command: ChatBotCommand) {
    val question = command.rest()
    val ai = database.chat.botCommandAi
    val baseUrl = runCatching { URI(ai.baseUrl) }.getOrNull() ?: return
    val platform = command.message.platform
    val user = command.user() ?: localized("Unknown")
    OpenAi(baseUrl = baseUrl, apiKey = ai.apiKey)
        .ask(question, model = ai.model, role = ai.personality) { result ->
            result.fold(
                onSuccess = { answer ->
                    sendChatBotReply(
                        message = formatAiAnswer(user = user, question = question, answer = answer),
                        platform = platform
                    )
                },
                onFailure = { error ->
                    sendChatBotReply(
                        message = localized(
                            "$user, sorry, I could not answer your question (${error.message}). 😢"
                        ),
                        platform = platform
                    )
                }
            )
        }
}

private fun Model.handleChatBotMessageTwitch(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.twitch,
        command = command,
        onCompleted = {
            when (command.popFirstArgument<ChatBotTwitchArgument>()) {
                ChatBotTwitchArgument.raid -> handleChatBotMessageTwitchRaid(command = command)
                else -> Unit
            }
        }
    )
}

private fun Model.handleChatBotMessageTwitchRaid(command: ChatBotCommand) {
    val channelName = command.rest()
    searchTwitchChannel(stream = stream.value, channelName = channelName) { channel ->
        if (channel == null) {
            makeErrorToast(
                title = localized("Raid failed"),
                subTitle = localized("Channel $channelName not found")
            )
            return@searchTwitchChannel
        }
        startRaidTwitchChannel(channelId = channel.id) { result ->
            if (result != null) {
                makeErrorToast(title = localized("Failed to raid $channelName"))
            }
        }
    }
}

private fun Model.handleChatBotMessageGimbal(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.gimbal,
        command = command,
        onCompleted = {
            when (command.popFirstArgument<ChatBotGimbalArgument>()) {
                ChatBotGimbalArgument.preset -> handleChatBotMessageGimbalPreset(command = command)
                else -> Unit
            }
        }
    )
}

private fun Model.handleChatBotMessageGimbalPreset(command: ChatBotCommand) {
    val presetName = command.popFirstLowerCased() ?: return
    val preset = database.gimbal.presets.firstOrNull {
        it.name.lowercase() == presetName
    } ?: return
    moveToGimbalPreset(id = preset.id)
}

private fun Model.handleChatBotMessageMacro(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.macro,
        command = command,
        onCompleted = {
            val subcommand = command.popFirstArgument<ChatBotMacroArgument>()
            if (subcommand != null) {
                val macroName = command.popFirstLowerCased()
                if (macroName != null) {
                    val macro = database.macros.macros.firstOrNull {
                        it.name.lowercase() == macroName
                    }
                    if (macro != null) {
                        when (subcommand) {
                            ChatBotMacroArgument.run -> handleChatBotMessageMacroRun(macro = macro)
                            ChatBotMacroArgument.cancel -> handleChatBotMessageMacroCancel(macro = macro)
                            else -> Unit
                        }
                    }
                }
            }
        }
    )
}

private fun Model.handleChatBotMessageSend(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.send,
        command = command,
        onCompleted = {
            sendChatBotReply(
                message = command.rest(),
                platform = command.message.platform
            )
        }
    )
}

fun Model.isChatBotCustomCommandsWeatherNeeded(): Boolean {
    return database.chat.botEnabled && database.chat.customCommands.any { it.needsWeather }
}

fun Model.isChatBotCustomCommandsGeographyNeeded(): Boolean {
    return database.chat.botEnabled && database.chat.customCommands.any { it.needsGeography }
}

fun Model.isChatBotCustomCommandsGForceNeeded(): Boolean {
    return database.chat.botEnabled && database.chat.customCommands.any { it.needsGForce }
}

fun Model.chatBotCustomCommandsTextChanged() {
    for (customCommand in database.chat.customCommands) {
        val parts = loadTextFormat(format = customCommand.formatString)
        customCommand.needsWeather = parts.isWeatherVariable()
        customCommand.needsGeography = parts.isGeographyVariable()
        customCommand.needsGForce = parts.isGForceVariable()
    }
    startWeatherManager()
    startGeographyManager()
    startGForceManager()
}

private fun Model.handleChatBotMessageCustom(command: ChatBotCommand) {
    val name = command.rest().lowercase()
    val customCommand = database.chat.customCommands.firstOrNull {
        it.name.lowercase() == name
    } ?: return
    executeIfUserAllowedToUseChatBot(
        permissions = customCommand.permissions,
        command = command,
        onCompleted = {
            sendChatBotReply(
                message = formatPlainText(formatString = customCommand.formatString),
                platform = command.message.platform
            )
        }
    )
}

private fun Model.handleChatBotMessageMusic(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.music,
        command = command,
        onCompleted = {
            when (command.popFirstArgument<ChatBotMusicArgument>()) {
                ChatBotMusicArgument.play -> handleChatBotMessageMusicPlay()
                ChatBotMusicArgument.pause -> handleChatBotMessageMusicPause()
                ChatBotMusicArgument.add -> handleChatBotMessageMusicAdd(command = command)
                ChatBotMusicArgument.next -> handleChatBotMessageMusicNext(command = command)
                ChatBotMusicArgument.previous -> handleChatBotMessageMusicPrevious(command = command)
                ChatBotMusicArgument.status -> handleChatBotMessageMusicStatus(command = command)
                else -> Unit
            }
        }
    )
}

private fun Model.handleChatBotMessageMusicPlay() {
    playMusic()
}

private fun Model.handleChatBotMessageMusicPause() {
    pauseMusic()
}

private fun Model.handleChatBotMessageMusicAdd(command: ChatBotCommand) {
    val title = command.rest()
    if (title.isEmpty()) {
        return
    }
    val platform = command.message.platform
    val user = command.user() ?: localized("Unknown")
    addMusic(title = title) { result ->
        val resultDescription = result.toString()
        if (resultDescription.contains("SongNotFound")) {
            sendChatBotReply(
                message = localized("$title requested by $user not found."),
                platform = platform
            )
        } else {
            val song = resultDescription.removePrefix("Added(song=").removeSuffix(")")
            sendChatBotReply(
                message = localized("$song added to the queue by $user."),
                platform = platform
            )
        }
    }
}

private fun Model.handleChatBotMessageMusicNext(command: ChatBotCommand) {
    nextMusic(count = command.popFirstInt(1..100) ?: 1)
}

private fun Model.handleChatBotMessageMusicPrevious(command: ChatBotCommand) {
    previousMusic(count = command.popFirstInt(1..100) ?: 1)
}

private fun Model.handleChatBotMessageMusicStatus(command: ChatBotCommand) {
    statusMusic { status ->
        val songs = mutableListOf<String>()
        var numberOfSongsNotShown = 0
        val currentSongIndex = status.currentSongIndex
        if (currentSongIndex != null) {
            val lastSongToShowIndex = currentSongIndex + 4
            status.songs.forEachIndexed { index, song ->
                if (index >= currentSongIndex && index <= lastSongToShowIndex) {
                    var title = song.title.take(50).trim()
                    if (title != song.title) {
                        title += "…"
                    }
                    if (index == currentSongIndex) {
                        if (status.playing) {
                            songs.add("▶️ $title")
                        } else {
                            songs.add("⏸️ $title")
                        }
                    } else {
                        songs.add(title)
                    }
                } else if (index > lastSongToShowIndex) {
                    numberOfSongsNotShown += 1
                }
            }
            if (numberOfSongsNotShown > 0) {
                songs.add("$numberOfSongsNotShown more")
            }
        } else {
            songs.add("Current song not found")
        }
        sendChatBotReply(
            message = songs.joinToString(" | "),
            platform = command.message.platform
        )
    }
}

private fun Model.handleChatBotMessageMacroRun(macro: SettingsMacrosMacro) {
    startMacro(macro = macro)
}

private fun Model.handleChatBotMessageMacroCancel(macro: SettingsMacrosMacro) {
    stopMacro(macro = macro)
}

private fun Model.handleChatBotMessageReaction(command: ChatBotCommand) {
    val reactionArgument = command.popFirstArgument<ChatBotReactionArgument>() ?: return
    val reaction = SettingsReaction.fromRawValue(reactionArgument.rawValue) ?: return
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.reaction,
        command = command,
        onCompleted = {
            triggerReaction(reaction = reaction)
        }
    )
}

fun Model.triggerReaction(reaction: SettingsReaction) {
    val systemReaction = reaction.toSystem()
    if (systemReaction != null) {
        triggerAppleReaction(reaction = systemReaction)
    } else {
        when (reaction.rawValue) {
            "glasses" -> triggerGlasses()
            "sparkle" -> triggerSparkle()
            else -> Unit
        }
    }
}

private fun Model.triggerAppleReaction(reaction: Any) {
    TODO("no Android counterpart for AVCaptureReactionType")
}

private fun Model.handleChatBotMessageScene(command: ChatBotCommand) {
    val sceneName = command.popFirst() ?: return
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.scene,
        command = command,
        onCompleted = {
            selectSceneByName(name = sceneName)
        }
    )
}

private fun Model.handleChatBotMessageStream(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.stream,
        command = command,
        onCompleted = {
            when (command.popFirstArgument<ChatBotStreamArgument>()) {
                ChatBotStreamArgument.start -> handleChatBotMessageStreamStart()
                ChatBotStreamArgument.stop -> handleChatBotMessageStreamStop()
                ChatBotStreamArgument.title -> handleChatBotMessageStreamTitle(command = command)
                ChatBotStreamArgument.category -> handleChatBotMessageStreamCategory(command = command)
                else -> Unit
            }
        }
    )
}

private fun Model.handleChatBotMessageStreamStart() {
    startStream()
}

private fun Model.handleChatBotMessageStreamStop() {
    stopStream()
}

private fun Model.handleChatBotMessageStreamTitle(command: ChatBotCommand) {
    setTwitchStreamTitle(stream = stream.value, title = command.rest())
}

private fun Model.handleChatBotMessageStreamCategory(command: ChatBotCommand) {
    fetchTwitchGameId(stream = stream.value, name = command.rest()) { gameId ->
        if (gameId != null) {
            setTwitchStreamCategory(stream = stream.value, categoryId = gameId)
        }
    }
}

private fun Model.handleChatBotMessageWidget(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.widget,
        command = command,
        onCompleted = {
            val name = command.popFirst()
            if (name != null) {
                val widget = findWidget(name = name)
                if (widget != null) {
                    when (command.popFirstArgument<ChatBotWidgetArgument>()) {
                        ChatBotWidgetArgument.enable -> handleChatBotMessageWidgetEnable(widget = widget)
                        ChatBotWidgetArgument.disable -> handleChatBotMessageWidgetDisable(widget = widget)
                        ChatBotWidgetArgument.timer -> handleChatBotMessageWidgetTimer(
                            command = command,
                            widget = widget
                        )
                        ChatBotWidgetArgument.wheelOfLuck -> handleChatBotMessageWidgetWheelOfLuck(
                            command = command,
                            widget = widget
                        )
                        else -> Unit
                    }
                }
            }
        }
    )
}

private fun Model.handleChatBotMessageWidgetEnable(widget: SettingsWidget) {
    widget.enabled = true
    reloadSpeechToText()
    sceneUpdated(attachCamera = isCaptureDeviceWidget(widget = widget))
}

private fun Model.handleChatBotMessageWidgetDisable(widget: SettingsWidget) {
    widget.enabled = false
    reloadSpeechToText()
    sceneUpdated(attachCamera = isCaptureDeviceWidget(widget = widget))
}

private fun Model.handleChatBotMessageWidgetTimer(command: ChatBotCommand, widget: SettingsWidget) {
    val effects = getTextEffects(id = widget.id)
    if (effects.isEmpty()) {
        return
    }
    if (widget.text.timers.isEmpty()) {
        return
    }
    val number = command.popFirstInt(1..widget.text.timers.size) ?: return
    val index = number - 1
    val timer = widget.text.timers[index]
    when (command.popFirstArgument<ChatBotWidgetTimerArgument>()) {
        ChatBotWidgetTimerArgument.add -> {
            val delta = command.popFirstDouble(-3600.0..3600.0) ?: return
            timer.add(delta = delta)
            val endTime = (timer.textEffectEndTime() as? Long) ?: 0L
            for (effect in effects) {
                effect.setEndTime(index = index, endTime = endTime)
            }
        }
        else -> Unit
    }
}

private fun Model.handleChatBotMessageWidgetWheelOfLuck(
    command: ChatBotCommand,
    widget: SettingsWidget
) {
    val effect = getWheelOfLuckEffect(id = widget.id) ?: return
    when (command.popFirstArgument<ChatBotWidgetWheelOfLuckArgument>()) {
        ChatBotWidgetWheelOfLuckArgument.spin -> effect.spin()
        ChatBotWidgetWheelOfLuckArgument.options -> {
            val options = command.popAll()
            widget.wheelOfLuck.optionsFromText(text = options.joinToString("\n"))
            getWheelOfLuckEffect(id = widget.id)?.setSettings(settings = widget.wheelOfLuck)
        }
        else -> Unit
    }
}

private fun Model.handleChatBotMessageAlert(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.alert,
        command = command,
        onCompleted = {
            val alert = command.popFirst()
            if (alert != null) {
                playAlert(alert = TODO("no Android counterpart for Alert"))
            }
        }
    )
}

private fun Model.handleChatBotMessageFax(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.fax,
        command = command,
        onCompleted = {
            val urlString = command.peekFirst()
            val url = urlString?.let { value -> runCatching { URI(value) }.getOrNull() }
            if (url != null) {
                faxReceiver.add(url = url.toString())
            }
        }
    )
}

private fun Model.handleChatBotMessageFilter(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.filter,
        command = command,
        onCompleted = {
            val filter = command.popFirstArgument<ChatBotFilterArgument>()
            if (filter != null) {
                val state = command.popFirstArgument<ChatBotOnOffArgument>()
                if (state != null) {
                    val on = state == ChatBotOnOffArgument.on
                    when (filter) {
                        ChatBotFilterArgument.movie -> setFilterQuickButton(
                            type = SettingsQuickButtonType.movie,
                            on = on
                        )
                        ChatBotFilterArgument.grayscale -> setFilterQuickButton(
                            type = SettingsQuickButtonType.grayScale,
                            on = on
                        )
                        ChatBotFilterArgument.sepia -> setFilterQuickButton(
                            type = SettingsQuickButtonType.sepia,
                            on = on
                        )
                        ChatBotFilterArgument.triple -> setFilterQuickButton(
                            type = SettingsQuickButtonType.triple,
                            on = on
                        )
                        ChatBotFilterArgument.twin -> setFilterQuickButton(
                            type = SettingsQuickButtonType.twin,
                            on = on
                        )
                        ChatBotFilterArgument.pixellate -> setPixellateQuickButton(on = on)
                        ChatBotFilterArgument.fourThree -> setFilterQuickButton(
                            type = SettingsQuickButtonType.fourThree,
                            on = on
                        )
                        ChatBotFilterArgument.whirlpool -> setWhirlpoolQuickButton(on = on)
                        ChatBotFilterArgument.pinch -> setPinchQuickButton(on = on)
                        else -> Unit
                    }
                }
            }
        }
    )
}

private fun Model.handleChatBotMessageZoom(command: ChatBotCommand) {
    val permissions = database.chat.botCommandPermissions.zoom
    executeIfUserAllowedToUseChatBot(
        permissions = permissions,
        command = command,
        onCompleted = {
            val x = command.rest().toFloatOrNull()
            if (x == null || !x.isFinite()) {
                if (permissions.sendChatMessages) {
                    sendChatBotReply(
                        message = localized("Sorry, zoom x must be a number."),
                        platform = command.message.platform
                    )
                }
            } else {
                setZoomX(x = x, rate = database.zoom.speed)
            }
        }
    )
}

private fun Model.handleChatBotMessageTesla(command: ChatBotCommand) {
    executeIfUserAllowedToUseChatBot(
        permissions = database.chat.botCommandPermissions.tesla,
        command = command,
        onCompleted = {
            when (command.popFirstArgument<ChatBotTeslaArgument>()) {
                ChatBotTeslaArgument.trunk -> handleChatBotMessageTeslaTrunk(command = command)
                ChatBotTeslaArgument.media -> handleChatBotMessageTeslaMedia(command = command)
                else -> Unit
            }
        }
    )
}

private fun Model.handleChatBotMessageTeslaTrunk(command: ChatBotCommand) {
    when (command.popFirstArgument<ChatBotTeslaTrunkArgument>()?.rawValue) {
        "open" -> tesla.vehicle?.openTrunk()
        "close" -> tesla.vehicle?.closeTrunk()
        else -> Unit
    }
}

private fun Model.handleChatBotMessageTeslaMedia(command: ChatBotCommand) {
    when (command.popFirstArgument<ChatBotTeslaMediaArgument>()) {
        ChatBotTeslaMediaArgument.next -> tesla.vehicle?.mediaNextTrack()
        ChatBotTeslaMediaArgument.previous -> tesla.vehicle?.mediaPreviousTrack()
        ChatBotTeslaMediaArgument.togglePlayback -> tesla.vehicle?.mediaTogglePlayback()
        else -> Unit
    }
}

private fun Model.sendChatBotReply(message: String, platform: Platform) {
    when (platform) {
        Platform.twitch -> sendTwitchChatMessage(message = message) { _ -> }
        Platform.kick -> sendKickChatMessage(message = message)
        else -> Unit
    }
}

private fun Model.executeIfUserAllowedToUseChatBot(
    permissions: SettingsChatBotPermissionsCommand,
    command: ChatBotCommand,
    onCompleted: () -> Unit,
    onNotAllowed: (() -> Unit)? = null
) {
    val now = Instant.now()
    if (isChannelOwner(command = command)) {
        permissions.latestExecutionTime = now.toEpochMilli()
        onCompleted()
        return
    }
    if (command.message.isModerator && permissions.moderatorsEnabled) {
        permissions.latestExecutionTime = now.toEpochMilli()
        onCompleted()
        return
    }
    val onCompletedChecked = {
        val cooldown = permissions.cooldown
        val latestExecutionTime = permissions.latestExecutionTime
        var onCooldown = false
        if (cooldown != null && latestExecutionTime != null) {
            val elapsed = Duration.between(Instant.ofEpochMilli(latestExecutionTime), now)
            val timeLeftOfCooldown = Duration.ofSeconds(cooldown.toLong()) - elapsed
            if (!timeLeftOfCooldown.isNegative) {
                onCooldown = true
                if (permissions.sendChatMessages) {
                    val user = command.user()
                    if (user != null) {
                        sendChatBotReply(
                            message = localized(
                                "$user Sorry, but this chat bot command is on cooldown for " +
                                    "${timeLeftOfCooldown.seconds} seconds. 😢"
                            ),
                            platform = command.message.platform
                        )
                    }
                }
            }
        }
        if (!onCooldown) {
            permissions.latestExecutionTime = now.toEpochMilli()
            onCompleted()
        }
    }
    var onNotAllowedLocal = onNotAllowed
    if (permissions.sendChatMessages && onNotAllowedLocal == null) {
        onNotAllowedLocal = {
            if (permissions.sendChatMessages) {
                val user = command.user()
                if (user != null) {
                    sendChatBotReply(
                        message = localized(
                            "$user Sorry, you are not allowed to use this chat bot command 😢"
                        ),
                        platform = command.message.platform
                    )
                }
            }
        }
    }
    if (command.message.isSubscriber && permissions.subscribersEnabled) {
        if (command.message.platform == Platform.twitch) {
            if (permissions.minimumSubscriberTier > 1) {
                val userId = command.message.userId
                if (userId != null) {
                    createTwitchApi(stream = stream.value).getBroadcasterSubscriptions(
                        broadcasterId = stream.value.twitchChannelId,
                        userId = userId
                    ) { data ->
                        val tier = data?.tierAsNumber()
                        if (tier != null && tier >= permissions.minimumSubscriberTier) {
                            onCompletedChecked()
                            return@getBroadcasterSubscriptions
                        }
                        executeIfUserAllowedToUseChatBotAfterSubscribeCheck(
                            permissions = permissions,
                            onCompleted = onCompletedChecked,
                            onNotAllowed = onNotAllowedLocal
                        )
                    }
                    return
                }
            } else {
                onCompletedChecked()
                return
            }
        } else {
            onCompletedChecked()
            return
        }
    }
    executeIfUserAllowedToUseChatBotAfterSubscribeCheck(
        permissions = permissions,
        onCompleted = onCompletedChecked,
        onNotAllowed = onNotAllowedLocal
    )
}

private fun Model.isChannelOwner(command: ChatBotCommand): Boolean {
    val user = command.user() ?: return false
    return when (command.message.platform) {
        Platform.twitch -> user.lowercase() == stream.value.twitchChannelName.lowercase()
        Platform.kick -> user.lowercase() == stream.value.kickChannelName.lowercase()
        Platform.youTube -> command.message.isOwner
        else -> false
    }
}

private fun Model.executeIfUserAllowedToUseChatBotAfterSubscribeCheck(
    permissions: SettingsChatBotPermissionsCommand,
    onCompleted: () -> Unit,
    onNotAllowed: (() -> Unit)?
) {
    if (permissions.othersEnabled) {
        onCompleted()
        return
    }
    onNotAllowed?.invoke()
}
