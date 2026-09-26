package com.moblin.android.intents

import com.moblin.android.localized
import com.moblin.android.platform.appintents.AppIntent
import com.moblin.android.platform.appintents.Dependency
import com.moblin.android.platform.appintents.IntentDescription
import com.moblin.android.platform.appintents.IntentResult
import com.moblin.android.platform.appintents.LocalizedStringResource
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsQuickButtonType

class UnmuteIntent : AppIntent {
    companion object {
        val title: LocalizedStringResource = localized("Unmute")
        val description: IntentDescription? = IntentDescription(localized("Unmutes audio."))
        val openAppWhenRun: Boolean = false
    }

    override suspend fun perform(): IntentResult {
        model.setMuted(value = false)
        model.setQuickButton(type = SettingsQuickButtonType.mute, isOn = false)
        return IntentResult.result()
    }

    private val model: Model by Dependency<Model>()
}
