package com.moblin.android.view.settings.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.*
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.updateMicDelay
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsMicsMic
import kotlin.math.roundToInt
import com.moblin.android.localized

@Composable
private fun MicDelayView(model: Model = LocalModel.current, mic: SettingsMicsMic) {
    val delay by mic.delay.collectAsState()
    Column(horizontalAlignment = Alignment.Start) {
        Text(mic.name, maxLines = 1)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FormSlider(
                value = delay.toFloat(),
                onValueChange = { value ->
                    mic._delay.value = (value * 100.0f).roundToInt() / 100.0
                    model.updateMicDelay()
                },
                modifier = Modifier.weight(1f),
                valueRange = -0.5f..0.5f,
            )
            Text(
                "${formatTwoDecimals(delay)} s",
                modifier = Modifier.width(60.dp),
            )
        }
    }
}

@Composable
fun MicsDelaySettingsView(model: Model = LocalModel.current, mics: SettingsMics) {
    val micsList by mics.mics.collectAsState()
    Form(title = "Delays") {
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("A positive delay makes the audio later, a negative delay earlier."))
                    Text("")
                    Text(localized("Use to synchronize audio and video."))
                }
            },
        ) {
            micsList.forEach { mic ->
                MicDelayView(model = model, mic = mic)
            }
        }
    }
}
