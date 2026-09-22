package com.moblin.android.view.stream.overlay.right

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database

@Composable
fun StreamOverlayRightTorchView(model: Model, database: Database) {
    val torchLevel by database.torchLevel.collectAsState()
    EffectSlider(
        title = "TORCH BRIGHTNESS",
        range = 0.01f..1f,
        value = torchLevel,
        onValueChange = { newValue ->
            database.torchLevel.value = newValue
            model.setTorchLevel(level = newValue)
        }
    )
}
