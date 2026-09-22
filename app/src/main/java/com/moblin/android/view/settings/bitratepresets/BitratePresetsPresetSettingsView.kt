package com.moblin.android.view.settings.bitratepresets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.bitrateFromMbps
import com.moblin.android.common.various.bitrateToMbps
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsBitratePreset
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemView

data class BitratePresetsPresetSettingsView(
    val preset: SettingsBitratePreset,
) {
    fun submit(bitrate: String) {
        val value = bitrate.toFloatOrNull() ?: return
        preset.bitrate = bitrateFromMbps(value.coerceIn(0.05f, 50f).toDouble())
    }

    @Composable
    fun Body() {
        var editing by remember { mutableStateOf(false) }
        Row(modifier = Modifier.clickable { editing = true }) {
            DraggableItemPrefixView()
            TextItemView(
                name = formatBytesPerSecond(preset.bitrate.toLong()),
                value = bitrateToMbps(preset.bitrate).toString(),
            )
        }
        if (editing) {
            TextEditView(
                title = localized("Bitrate"),
                value = bitrateToMbps(preset.bitrate).toString(),
                keyboardType = KeyboardType.Decimal,
            ) {
                submit(it)
                editing = false
            }
        }
    }
}
