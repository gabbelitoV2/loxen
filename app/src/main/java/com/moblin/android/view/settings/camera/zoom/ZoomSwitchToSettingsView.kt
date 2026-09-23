package com.moblin.android.view.settings.camera.zoom

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsZoomSwitchTo
import com.moblin.android.view.utils.TextEditView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.various.model.getMinMaxZoomX

private fun x(defaultZoom: SettingsZoomSwitchTo): Float {
    return defaultZoom.x
}

private fun formatX(x: Float): String {
    return formatOneDecimal(x)
}

@Composable
fun ZoomSwitchToSettingsView(
    model: Model = LocalModel.current,
    name: String,
    position: AVCaptureDevice.Position,
    defaultZoom: SettingsZoomSwitchTo,
) {
    NavigationLink(
        destination = {
            TextEditView(
                title = localized("To $name camera"),
                value = formatX(x(defaultZoom)),
                keyboardType = KeyboardType.Number,
                onSubmit = { value ->
                    val newX = value.toFloatOrNull()
                    if (newX != null) {
                        val (minX, maxX) = model.getMinMaxZoomX(position)
                        if (newX < minX || newX > maxX) {
                            model.makeErrorToast(localized("X must be $minX - $maxX"))
                        } else {
                            defaultZoom.x = newX
                        }
                    }
                },
            )
        },
    ) {
        Toggle(
            isOn = defaultZoom.enabled,
            onChange = { defaultZoom.enabled = it },
        ) {
            TextItemLocalizedView(name = "To $name camera", value = formatX(x(defaultZoom)))
        }
    }
}
