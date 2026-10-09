package com.moblin.android.view.settings.debug

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.LocalModel

@Composable
fun DebugVideoSettingsView(
    model: Model = LocalModel.current,
    debug: SettingsDebug,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val nativeLowLightBoost by debug.nativeLowLightBoost.collectAsState()
    val externalCameraVideoRange by debug.externalCameraVideoRange.collectAsState()
    val videoBitrateChange by debug.videoBitrateChange.collectAsState()
    val photosImageQuality by debug.photosImageQuality.collectAsState()

    Form(title = localized("Video")) {
        Section(
            footer = localized("Change camera and restart stream for these to work properly."),
        ) {
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
            Toggle(
                title = localized("External camera video range"),
                isOn = binding(
                    get = { externalCameraVideoRange },
                    set = {
                        debug.externalCameraVideoRange.value = it
                        model.setExternalCameraVideoRange()
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
        Section(
            footer = localized("Compression quality of snapshots and photo shoot photos saved to Photos."),
        ) {
            Picker(
                title = localized("Photos image quality"),
                selection = photosImageQuality,
                options = listOf(0.9, 0.95, 1.0),
                text = { it.toString() },
                onChange = {
                    debug.photosImageQuality.value = it
                    model.setPhotosImageQuality()
                },
            )
        }
    }
}
