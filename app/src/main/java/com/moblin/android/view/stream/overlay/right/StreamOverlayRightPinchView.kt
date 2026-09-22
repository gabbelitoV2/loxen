package com.moblin.android.view.stream.overlay.right

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database

@Composable
fun StreamOverlayRightPinchView(
    model: Model,
    database: Database,
) {
    val pinchScale by database.pinchScale.collectAsState()

    EffectSlider(
        title = "PINCH SCALE",
        range = 0.5..1.0,
        value = pinchScale,
        onChange = { newValue ->
            database.setPinchScale(newValue)
            model.setPinchScale(scale = newValue)
        }
    )
}
