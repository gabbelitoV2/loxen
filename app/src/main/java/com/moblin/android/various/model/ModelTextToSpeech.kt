package com.moblin.android.various.model

import com.moblin.android.various.ChatPost
import com.moblin.android.various.settings.SettingsQuickButtonType

fun Model.toggleTextToSpeechPaused() {
    if (getQuickButton(type = SettingsQuickButtonType.pauseTts)?.isOn?.value == false) {
        chatTextToSpeech.pause()
    } else {
        chatTextToSpeech.play()
    }
    toggleQuickButton(type = SettingsQuickButtonType.pauseTts)
}

fun Model.isTextToSpeechEnabledForMessage(post: ChatPost): Boolean {
    if (!database.chat.textToSpeechEnabled) {
        return false
    }
    if (!post.live) {
        return false
    }
    if (post.filter?.textToSpeech == false) {
        return false
    }
    if (!isTextToSpeechOutputAllowed()) {
        return false
    }
    if (database.chat.textToSpeechSubscribersOnly) {
        if (!post.isSubscriber) {
            return false
        }
    }
    if (post.bits != null) {
        return false
    }
    if (isAlertMessage(post = post) && isTextToSpeechEnabledForAnyAlertWidget()) {
        return false
    }
    return post.user != null
}

fun Model.isTextToSpeechOutputAllowed(): Boolean {
    return !database.chat.textToSpeechBluetoothSpeakerOnly || isBluetoothAudioOutput
}

fun Model.stopTextToSpeechIfOutputNotAllowed() {
    if (!database.chat.textToSpeechEnabled || isTextToSpeechOutputAllowed()) {
        return
    }
    chatTextToSpeech.reset(running = true)
}

private fun Model.isTextToSpeechEnabledForAnyAlertWidget(): Boolean {
    for (alertEffect in enabledAlertsEffects) {
        val settings = alertEffect.getSettings()
        if (settings.twitch.follows.isTextToSpeechEnabled()) {
            return true
        }
        if (settings.twitch.subscriptions.isTextToSpeechEnabled()) {
            return true
        }
        if (settings.twitch.raids.isTextToSpeechEnabled()) {
            return true
        }
        if (settings.twitch.cheers.isTextToSpeechEnabled()) {
            return true
        }
    }
    return false
}

fun Model.setTextToSpeechStreamerMentions() {
    val streamerMentions = mutableListOf<String>()
    if (isTwitchChatConfigured()) {
        streamerMentions.add("@${stream.value.twitchChannelName}")
    }
    if (isKickPusherConfigured()) {
        streamerMentions.add("@${stream.value.kickChannelName}")
    }
    if (isSoopChatConfigured()) {
        streamerMentions.add("@${stream.value.soopChannelName}")
    }
    chatTextToSpeech.setStreamerMentions(streamerMentions)
}

fun Model.previewTextToSpeech(username: String, message: String) {
    chatTextToSpeech.sayPreview(username, message)
}
