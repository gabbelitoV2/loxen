package com.moblin.android.view.settings.talkback

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getMicById
import com.moblin.android.various.model.unknownSad
import com.moblin.android.various.model.updateTalkback
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsTalkback
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.IngestsShortcutView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.LocalModel

private fun onChange(micId: String, model: Model, talkback: SettingsTalkback) {
    talkback.micId.value = micId
    model.updateTalkback()
}

@Composable
fun TalkbackSettingsView(
    model: Model = LocalModel.current,
    mics: SettingsMics,
    talkback: SettingsTalkback,
) {
    val enabled by talkback.enabled.collectAsState()
    val micId by talkback.micId.collectAsState()

    Form(title = "Talkback") {
        Section {
            Text("Play audio from an Ingest in your speakers.")
        }
        Section {
            Toggle(
                title = "Enabled",
                isOn = enabled,
                onChange = { value ->
                    talkback.enabled.value = value
                    model.updateTalkback()
                },
            )
        }
        Section {
            NavigationLink(
                destination = {
                    val micList by mics.mics.collectAsState()
                    InlinePickerView(
                        title = "Mic",
                        onChange = { id ->
                            onChange(id, model, talkback)
                        },
                        items = micList
                            .filter { it.isNetwork() }
                            .map { InlinePickerItem(id = it.id, text = it.name) },
                        initialSelectedId = micId,
                    )
                },
            ) {
                Text("Mic")
                Spacer(modifier = Modifier.weight(1f))
                GrayTextView(text = model.getMicById(micId)?.name ?: unknownSad)
            }
        }
        ShortcutSectionView {
            IngestsShortcutView(model = model)
        }
    }
}
