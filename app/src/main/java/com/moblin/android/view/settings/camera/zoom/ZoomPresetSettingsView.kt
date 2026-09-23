package com.moblin.android.view.settings.camera.zoom

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
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

class ZoomPresetSettingsView(
    private val model: Model,
    private val preset: SettingsZoomPreset,
    private val minX: Float,
    private val maxX: Float,
) {
    fun submitX(x: String) {
        val x = x.toFloatOrNull() ?: return
        if (x < minX || x > maxX) {
            model.makeErrorToast(title = localized("X must be $minX - $maxX"))
            return
        }
        preset.x = x
        preset.name = "${formatOneDecimal(x)}x".replace(".0", "")
        model.frontZoomPresetSettingUpdated()
        model.backZoomPresetSettingsUpdated()
    }

    private fun formatX(x: Float): String {
        return formatOneDecimal(x)
    }

    @Composable
    fun body() {
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
            DraggableItemPrefixView()
            TextItemView(name = preset.name, value = preset.x.toString())
        }
    }
}
