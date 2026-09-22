package com.moblin.android.view.settings.streams.stream.rtmp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.LocalModel

@Composable
fun StreamRtmpSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(text = "Adaptive bitrate")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = stream.rtmp.adaptiveBitrateEnabled,
                onCheckedChange = { value ->
                    stream.rtmp.adaptiveBitrateEnabled = value
                    model.reloadStreamIfEnabled(stream)
                },
                enabled = !(stream.enabled && isLive),
            )
        }
    }
}
