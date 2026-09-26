package com.moblin.android.intents

import com.moblin.android.platform.appintents.AppIntent
import com.moblin.android.platform.appintents.Dependency
import com.moblin.android.platform.appintents.IntentDescription
import com.moblin.android.platform.appintents.IntentResult
import com.moblin.android.platform.appintents.LocalizedStringResource
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsQuickButtonType

class MuteIntent : AppIntent {
    companion object {
        val title: LocalizedStringResource = "Mute"
        val description: IntentDescription? = IntentDescription("Mutes audio.")
        val openAppWhenRun: Boolean = false
    }

    override suspend fun perform(): IntentResult {
        model.setMuted(value = true)
        model.setQuickButton(type = SettingsQuickButtonType.mute, isOn = true)
        return IntentResult.result()
    }

    private val model: Model by Dependency<Model>()
}
