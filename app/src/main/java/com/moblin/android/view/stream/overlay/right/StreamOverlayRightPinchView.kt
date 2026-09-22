package com.moblin.android.view.stream.overlay.right

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.LocalModel

@Composable
fun StreamOverlayRightPinchView(
    model: Model = LocalModel.current,
    database: Database,
) {
    val pinchScale = database.pinchScale

    EffectSlider(
        title = "PINCH SCALE",
        range = 0.5f..1.0f,
        value = pinchScale,
        onValueChange = { newValue ->
            database.pinchScale = newValue
            model.setPinchScale(scale = newValue)
        }
    )
}
