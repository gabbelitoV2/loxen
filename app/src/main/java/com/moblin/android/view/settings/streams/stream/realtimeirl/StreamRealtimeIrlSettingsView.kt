package com.moblin.android.view.settings.streams.stream.realtimeirl

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.reloadLocation

@Composable
fun StreamRealtimeIrlSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun submitBaseUrl(value: String) {
        stream.realtimeIrlBaseUrl = value
        if (stream.enabled) {
            model.reloadLocation()
        }
    }

    fun submitPushKey(value: String) {
        stream.realtimeIrlPushKey = value
        if (stream.enabled) {
            model.reloadLocation()
        }
    }

    Form(title = "RealtimeIRL") {
        Section {
            FormRow {
                Text(
                    text = localized(
                        "Send your location to https://rtirl.com, to let your viewers know where you are.",
                    ),
                    style = formBodyStyle,
                )
            }
        }
        Section {
            TextEditNavigationView(
                title = localized("Base URL"),
                value = stream.realtimeIrlBaseUrl,
                onChange = { isValidHttpUrl(it) },
                onSubmit = { submitBaseUrl(it) },
                placeholder = SettingsStream.defaultRealtimeIrlBaseUrl,
                onNavigate = onNavigate,
            )
            TextEditNavigationView(
                title = localized("Push key"),
                value = stream.realtimeIrlPushKey,
                onSubmit = { submitPushKey(it) },
                sensitive = true,
                onNavigate = onNavigate,
            )
        }
    }
}
