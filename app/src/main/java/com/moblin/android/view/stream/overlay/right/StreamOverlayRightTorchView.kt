package com.moblin.android.view.stream.overlay.right

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.LocalModel

@Composable
fun StreamOverlayRightTorchView(model: Model = LocalModel.current, database: Database) {
    val torchLevel = database.torchLevel
    EffectSlider(
        title = "TORCH BRIGHTNESS",
        range = 0.01f..1f,
        value = torchLevel,
        onValueChange = { newValue ->
            database.torchLevel = newValue
            model.setTorchLevel(level = newValue)
        }
    )
}
