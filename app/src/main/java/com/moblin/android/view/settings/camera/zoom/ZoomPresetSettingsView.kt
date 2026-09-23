package com.moblin.android.view.settings.camera.zoom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsZoomPreset
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemView
import com.moblin.android.various.model.backZoomPresetSettingsUpdated
import com.moblin.android.various.model.frontZoomPresetSettingUpdated

@Composable
fun ZoomPresetSettingsView(
    model: Model = LocalModel.current,
    preset: SettingsZoomPreset,
    minX: Float,
    maxX: Float,
) {
    fun submitX(value: String) {
        val x = value.toFloatOrNull() ?: return
        if (x < minX || x > maxX) {
            model.makeErrorToast(title = localized("X must be $minX - $maxX"))
            return
        }
        preset.x = x
        preset.name = "${formatOneDecimal(x)}x".replace(".0", "")
        model.frontZoomPresetSettingUpdated()
        model.backZoomPresetSettingsUpdated()
    }

    fun formatX(x: Float): String {
        return formatOneDecimal(x)
    }

    NavigationLink(
        destination = {
            TextEditView(
                title = localized("X"),
                value = preset.x.toString(),
                footers = listOf(
                    localized("Allowed range is ${formatX(minX)} - ${formatX(maxX)}."),
                ),
                keyboardType = KeyboardType.Decimal,
                onSubmit = { submitX(it) },
            )
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableItemPrefixView()
            TextItemView(name = preset.name, value = preset.x.toString())
        }
    }
}
