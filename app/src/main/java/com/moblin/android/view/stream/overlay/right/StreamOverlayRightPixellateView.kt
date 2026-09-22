package com.moblin.android.view.stream.overlay.right

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database

@Composable
fun StreamOverlayRightPixellateView(
    model: Model,
    database: Database,
) {
    val pixellateStrength by database.pixellateStrength.collectAsState()
    EffectSlider(
        title = "PIXELLATE STRENGTH",
        range = 0f..1f,
        value = pixellateStrength,
        onChange = { database.pixellateStrength.value = it },
    )
    LaunchedEffect(pixellateStrength) {
        model.setPixellateStrength(strength = pixellateStrength)
    }
}
