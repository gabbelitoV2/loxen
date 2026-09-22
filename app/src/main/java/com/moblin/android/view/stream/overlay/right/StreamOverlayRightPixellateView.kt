package com.moblin.android.view.stream.overlay.right

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.LocalModel

@Composable
fun StreamOverlayRightPixellateView(
    model: Model = LocalModel.current,
    database: Database,
) {
    val pixellateStrength = database.pixellateStrength
    EffectSlider(
        title = "PIXELLATE STRENGTH",
        range = 0f..1f,
        value = pixellateStrength,
        onValueChange = { database.pixellateStrength = it },
    )
    LaunchedEffect(pixellateStrength) {
        model.setPixellateStrength(strength = pixellateStrength)
    }
}
