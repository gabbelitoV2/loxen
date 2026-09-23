package com.moblin.android.view.settings.streams.stream.rist

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
fun StreamRistSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    val isLive by model.isLive.collectAsState()
    Form(title = "RIST") {
        Section {
            Toggle(
                title = "Adaptive bitrate",
                isOn = binding(
                    get = { stream.rist.adaptiveBitrateEnabled },
                    set = { value ->
                        stream.rist.adaptiveBitrateEnabled = value
                        model.reloadStreamIfEnabled(stream)
                    },
                ),
                enabled = !(stream.enabled && isLive),
            )
            Toggle(
                title = "Bonding",
                isOn = binding(
                    get = { stream.rist.bonding },
                    set = { value ->
                        stream.rist.bonding = value
                        model.reloadStreamIfEnabled(stream)
                    },
                ),
                enabled = !(stream.enabled && isLive),
            )
        }
    }
}
