package com.moblin.android.view.settings.camera.zoom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsZoomPreset
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemView

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
        var showX by remember { mutableStateOf(false) }
        Row(modifier = Modifier.clickable { showX = true }) {
            DraggableItemPrefixView()
            TextItemView(name = preset.name, value = preset.x.toString())
        }
        if (showX) {
            Dialog(onDismissRequest = { showX = false }) {
                TextEditView(
                    title = localized("X"),
                    value = preset.x.toString(),
                    footers = listOf(
                        localized("Allowed range is ${formatX(minX)} - ${formatX(maxX)}."),
                    ),
                    keyboardType = KeyboardType.Text,
                    onSubmit = { submitX(it) },
                )
            }
        }
    }
}
