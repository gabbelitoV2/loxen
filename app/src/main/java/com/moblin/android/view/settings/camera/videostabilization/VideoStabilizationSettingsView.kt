package com.moblin.android.view.settings.camera.videostabilization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoStabilizationMode
import com.moblin.android.various.settings.videoStabilizationModes
import com.moblin.android.LocalModel

@Composable
fun VideoStabilizationSettingsView(
    model: Model = LocalModel.current,
    mode: SettingsVideoStabilizationMode,
    onModeChange: (SettingsVideoStabilizationMode) -> Unit = {},
) {
    var modeState by remember { mutableStateOf(mode) }

    Picker(
        title = localized("Video stabilization"),
        selection = modeState,
        options = videoStabilizationModes,
        text = { it.toString() },
        onChange = {
            modeState = it
            model.database.videoStabilizationMode = it
            model.attachCamera()
            onModeChange(it)
        },
    )
}
