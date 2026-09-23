package com.moblin.android.view.settings.camera.mirrorfrontcamera

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database

@Composable
fun MirrorFrontCameraOnStreamView(model: Model = LocalModel.current, database: Database) {
    Toggle(
        title = "Mirror front camera on stream",
        isOn = binding(
            get = { database.mirrorFrontCameraOnStream },
            set = {
                database.mirrorFrontCameraOnStream = it
                model.attachCamera()
            },
        ),
    )
}
