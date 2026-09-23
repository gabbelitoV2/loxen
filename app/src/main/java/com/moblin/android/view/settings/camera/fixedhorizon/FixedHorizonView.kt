package com.moblin.android.view.settings.camera.fixedhorizon

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.model.sceneUpdated

@Composable
fun FixedHorizonView(model: Model = LocalModel.current, database: Database) {
    Toggle(
        title = "Fixed horizon",
        isOn = database.fixedHorizon,
        onChange = { value ->
            database.fixedHorizon = value
            model.sceneUpdated()
        },
    )
}
