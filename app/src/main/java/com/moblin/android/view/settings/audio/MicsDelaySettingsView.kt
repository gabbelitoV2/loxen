package com.moblin.android.view.settings.audio

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatTwoDecimals
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsMicsMic
import com.moblin.android.LocalModel

@Composable
private fun MicDelayView(model: Model = LocalModel.current, mic: SettingsMicsMic) {
    val delay by mic.delay.collectAsState()
    var initialized by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.Start) {
        Text(mic.name, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = delay.toFloat(),
                onValueChange = { mic._delay.value = it.toDouble() },
                modifier = Modifier.weight(1f),
                valueRange = -0.5f..0.5f,
                steps = 99,
            )
            Text(
                "${formatTwoDecimals(delay)} s",
                modifier = Modifier.width(60.dp),
            )
        }
    }
    LaunchedEffect(delay) {
        if (initialized) {
            Unit
        } else {
            initialized = true
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicsDelaySettingsView(model: Model = LocalModel.current, mics: SettingsMics) {
    val micsList by mics.mics.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Delays") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(micsList) { mic ->
                MicDelayView(model = model, mic = mic)
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("A positive delay makes the audio later, a negative delay earlier.")
                    Text("")
                    Text("Use to synchronize audio and video.")
                }
            }
        }
    }
}
