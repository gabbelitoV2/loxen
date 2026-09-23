package com.moblin.android.view.settings.streams.stream.rtmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.model.reloadStreamIfEnabled

@Composable
fun StreamRtmpSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    Form(title = "RTMP") {
        Section {
            Toggle(
                title = "Adaptive bitrate",
                isOn = binding(
                    get = { stream.rtmp.adaptiveBitrateEnabled },
                    set = { value ->
                        stream.rtmp.adaptiveBitrateEnabled = value
                        model.reloadStreamIfEnabled(stream = stream)
                    },
                ),
                enabled = !(stream.enabled && isLive),
            )
        }
    }
}
