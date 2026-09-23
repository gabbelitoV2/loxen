package com.moblin.android.view.settings.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.various.model.reloadStream
import com.moblin.android.various.model.sceneUpdated
import com.moblin.android.various.model.setPixelFormat

private fun onPixelFormatChange(model: Model, format: String) {
    model.database.debug.pixelFormat = format
    model.setPixelFormat()
    model.reloadStream()
    model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
}

@Composable
fun DebugVideoSettingsView(
    model: Model = LocalModel.current,
    debug: SettingsDebug,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val pixelFormat = model.database.debug.pixelFormat
    val allowVideoRangePixelFormat by debug.allowVideoRangePixelFormat.collectAsState()
    val nativeLowLightBoost by debug.nativeLowLightBoost.collectAsState()
    val videoBitrateChange by debug.videoBitrateChange.collectAsState()

    Form(title = localized("Video")) {
        Section(
            footer = localized("Change camera and restart stream for these to work properly."),
        ) {
            FormRow(onClick = { onNavigate("pixelFormat") }) {
                TextItemLocalizedView(name = "Pixel format", value = pixelFormat)
            }
            Toggle(
                title = localized("Allow video range pixel format"),
                isOn = binding(
                    get = { allowVideoRangePixelFormat },
                    set = {
                        debug.allowVideoRangePixelFormat.value = it
                        model.setAllowVideoRangePixelFormat()
                    },
                ),
            )
            Toggle(
                title = localized("Native low light boost"),
                isOn = binding(
                    get = { nativeLowLightBoost },
                    set = {
                        debug.nativeLowLightBoost.value = it
                        model.setNativeLowLightBoost()
                    },
                ),
            )
        }
        Section {
            Toggle(
                title = localized("Periodic video bitrate change"),
                isOn = binding(
                    get = { videoBitrateChange },
                    set = { debug.videoBitrateChange.value = it },
                ),
            )
        }
    }
}
