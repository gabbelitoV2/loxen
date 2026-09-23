package com.moblin.android.view.settings.bitratepresets

import androidx.compose.runtime.Composable
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.DeleteDisabled
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.move
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsBitratePreset
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import java.util.UUID

@Composable
fun BitratePresetsSettingsView(database: Database) {
    Form(title = "Bitrate presets") {
        Section(footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a preset")) }) {
            ForEach(
                database.bitratePresets,
                id = { it.id },
                onDelete = { offsets ->
                    database.bitratePresets.remove(atOffsets = offsets)
                },
                onMove = { froms, to ->
                    database.bitratePresets.move(fromOffsets = froms, toOffset = to)
                },
            ) { preset ->
                ContextMenuDeleteButton(
                    disabled = database.bitratePresets.size == 1,
                    action = {
                        database.bitratePresets.removeAll { it.id == preset.id }
                    },
                ) {
                    DeleteDisabled(database.bitratePresets.size == 1) {
                        BitratePresetsPresetSettingsView(preset = preset)
                    }
                }
            }
            CreateButtonView {
                database.bitratePresets = (database.bitratePresets + SettingsBitratePreset(
                    id = UUID.randomUUID(),
                    bitrate = 1_000_000,
                )).toMutableList()
            }
        }
    }
}
