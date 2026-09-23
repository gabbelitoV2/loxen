package com.moblin.android.view.settings.bitratepresets

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.bitrateFromMbps
import com.moblin.android.common.various.bitrateToMbps
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.various.settings.SettingsBitratePreset
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemView

data class BitratePresetsPresetSettingsView(
    val preset: SettingsBitratePreset,
) {
    fun submit(bitrate: String) {
        val value = bitrate.toFloatOrNull() ?: return
        preset.bitrate = bitrateFromMbps(value.coerceIn(0.05f, 50f)).toInt()
    }

    @Composable
    fun Body() {
        NavigationLink(destination = {
            TextEditView(
                title = localized("Bitrate"),
                value = bitrateToMbps(preset.bitrate.toUInt()).toString(),
                keyboardType = KeyboardType.Decimal,
                onSubmit = { submit(it) },
            )
        }) {
            DraggableItemPrefixView()
            TextItemView(
                name = formatBytesPerSecond(preset.bitrate.toLong()),
                value = bitrateToMbps(preset.bitrate.toUInt()).toString(),
            )
        }
    }
}
