package com.moblin.android.view.settings.audio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsMicsMic
import kotlin.math.roundToInt
import com.moblin.android.LocalModel
import com.moblin.android.various.model.updateAudioGain

@Composable
private fun MicGainView(model: Model = LocalModel.current, mic: SettingsMicsMic) {
    val gainDb by mic.gainDb.collectAsState()
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            mic.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormSlider(
                value = gainDb,
                onValueChange = { value ->
                    mic._gainDb.value = value.roundToInt().toFloat().coerceIn(-24.0f, 24.0f)
                    model.updateAudioGain()
                },
                modifier = Modifier.weight(1f),
                valueRange = -24.0f..24.0f,
            )
            Text(
                "${formatOneDecimal(gainDb)} dB",
                modifier = Modifier.width(65.dp),
            )
        }
    }
}

@Composable
fun MicsGainSettingsView(model: Model = LocalModel.current, mics: SettingsMics) {
    val micsList by mics.mics.collectAsState()
    Form(title = "Gains") {
        Section(footerContent = {
            Text("Added to the output gain when the mic is selected.")
        }) {
            ForEach(micsList, id = { it.id }) { mic ->
                MicGainView(model = model, mic = mic)
            }
        }
    }
}
