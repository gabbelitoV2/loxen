package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsBitratePreset
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.settings.bitratepresets.BitratePresetsSettingsView

@Composable
private fun BitratePresetView(preset: SettingsBitratePreset) {
    Text(text = formatBytesPerSecond(speed = preset.bitrate.toLong()))
}

@Composable
fun QuickButtonBitrateView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = localized("Bitrate")) {
        if (stream !== fallbackStream) {
            Section {
                Picker(
                    title = "",
                    selection = stream.bitrate,
                    options = database.bitratePresets.map { it.bitrate },
                    text = { formatBytesPerSecond(speed = it.toLong()) },
                    onChange = { bitrate ->
                        stream.bitrate = bitrate
                        Unit
                    },
                )
            }
        }
        ShortcutSectionView {
            NavigationLink(
                destination = {
                    BitratePresetsSettingsView(database = model.database)
                },
            ) {
                Label("Bitrate presets", systemImage = "dot.radiowaves.left.and.right")
            }
        }
    }
}
