package com.moblin.android.view.stream.overlay.right

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import kotlin.math.PI
import com.moblin.android.LocalModel

@Composable
fun StreamOverlayRightWhirlpoolView(model: Model = LocalModel.current, database: Database) {
    val databaseAngle = database.whirlpoolAngle
    var angle by remember { mutableStateOf(databaseAngle) }
    LaunchedEffect(databaseAngle) {
        angle = databaseAngle
    }
    EffectSlider(
        title = "WHIRLPOOL ANGLE",
        range = (PI / 2).toFloat()..(PI * 2).toFloat(),
        value = angle,
        onValueChange = {
            angle = it
            model.setWhirlpoolAngle(it)
        },
    )
}
