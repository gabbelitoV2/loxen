package com.moblin.android.intents

import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsQuickButtonType

class UnmuteIntent(private val model: Model) {
    suspend fun perform() {
        model.setMuted(value = false)
        model.setQuickButton(type = SettingsQuickButtonType.mute, isOn = false)
    }

    companion object {
        val title: String = "Unmute"
        val description: String? = "Unmutes audio."
        val openAppWhenRun: Boolean = false
    }
}
