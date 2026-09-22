package com.moblin.android.view.settings.camera.zoom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsZoomSwitchTo
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalModel

private fun x(defaultZoom: SettingsZoomSwitchTo): Float {
    return defaultZoom.x
}

private fun formatX(x: Float): String {
    return formatOneDecimal(x)
}

private fun getMinMaxZoomX(position: Int): Pair<Float, Float> {
    TODO("model.getMinMaxZoomX(position)")
}

@Composable
fun ZoomSwitchToSettingsView(
    model: Model = LocalModel.current,
    name: String,
    position: Int,
    defaultZoom: SettingsZoomSwitchTo,
) {
    var editing by remember { mutableStateOf(false) }
    if (editing) {
        TextEditView(
            title = localized("To $name camera"),
            value = formatX(x(defaultZoom)),
            keyboardType = KeyboardType.Number,
            onChange = { value ->
                val newX = value.toFloatOrNull()
                if (newX != null) {
                    val (minX, maxX) = getMinMaxZoomX(position)
                    if (newX < minX || newX > maxX) {
                        model.makeErrorToast(localized("X must be $minX - $maxX"))
                    } else {
                        defaultZoom.x = newX
                    }
                }
                null
            },
            onSubmit = { editing = false },
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { editing = true },
        ) {
            TextItemLocalizedView(name = "To $name camera", value = formatX(x(defaultZoom)))
            Switch(
                checked = defaultZoom.enabled,
                onCheckedChange = { defaultZoom.enabled = it },
            )
        }
    }
}
