package com.moblin.android.view.settings.bitratepresets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsBitratePreset
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitratePresetsSettingsView(database: Database) {
    val bitratePresets = database.bitratePresets
    val palette = formPalette()
    Form(title = "Bitrate presets") {
        Section(footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a preset")) }) {
            bitratePresets.forEach { preset ->
                key(preset.id) {
                    val deleteDisabled = bitratePresets.size == 1
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart && !deleteDisabled) {
                                database.bitratePresets =
                                    database.bitratePresets.filterNot { it.id == preset.id }.toMutableList()
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = !deleteDisabled,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(palette.red)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                )
                            }
                        },
                    ) {
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
