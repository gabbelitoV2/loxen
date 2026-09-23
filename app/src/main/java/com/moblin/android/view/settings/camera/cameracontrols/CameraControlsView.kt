package com.moblin.android.view.settings.camera.cameracontrols

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.model.setCameraControlsEnabled

@Composable
fun CameraControlsView(model: Model = LocalModel.current, database: Database) {
    Toggle(
        title = "Camera controls",
        isOn = database.cameraControlsEnabled,
        onChange = {
            database.cameraControlsEnabled = it
            model.setCameraControlsEnabled()
        },
    )
}
