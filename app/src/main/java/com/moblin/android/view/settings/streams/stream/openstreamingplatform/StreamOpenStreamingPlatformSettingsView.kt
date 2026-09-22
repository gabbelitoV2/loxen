package com.moblin.android.view.settings.streams.stream.openstreamingplatform

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextEditNavigationView

fun submitUrl(model: Model, stream: SettingsStream, value: String) {
    if (isValidWebSocketUrl(value) != null) {
        return
    }
    stream.openStreamingPlatformUrl = value
    if (stream.enabled) {
        model.openStreamingPlatformUrlUpdated()
    }
}

fun submitRoom(model: Model, stream: SettingsStream, value: String) {
    stream.openStreamingPlatformChannelId = value
    if (stream.enabled) {
        model.openStreamingPlatformRoomUpdated()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamOpenStreamingPlatformSettingsView(model: Model, stream: SettingsStream) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Open Streaming Platform") })
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                TextEditNavigationView(
                    title = localized("URL"),
                    value = stream.openStreamingPlatformUrl,
                    onChange = { value -> isValidWebSocketUrl(value) },
                    onSubmit = { value -> submitUrl(model, stream, value) },
                    placeholder = "ws://foo.org:5443/ws"
                )
            }
            item {
                TextEditNavigationView(
                    title = localized("Channel id"),
                    value = stream.openStreamingPlatformChannelId,
                    onSubmit = { value -> submitRoom(model, stream, value) },
                    placeholder = "4e9f02fc-cee9-4d1c-b4b5-99b9496375c8"
                )
            }
        }
    }
}
