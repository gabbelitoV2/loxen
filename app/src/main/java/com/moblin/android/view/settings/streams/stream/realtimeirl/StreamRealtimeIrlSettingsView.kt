package com.moblin.android.view.settings.streams.stream.realtimeirl

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
fun StreamRealtimeIrlSettingsView(
    model: Model,
    stream: SettingsStream,
    onNavigate: (String) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = localized(
                "Send your location to https://rtirl.com, to let your viewers know where you are.",
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
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
