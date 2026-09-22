package com.moblin.android.view.settings.streams.stream.soop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel

private fun submitChannelName(model: Model, stream: SettingsStream, value: String) {
    stream.soopChannelName = value
    if (stream.enabled) {
        model.soopChannelNameUpdated()
    }
}

private fun submitStreamId(model: Model, stream: SettingsStream, value: String) {
    stream.soopStreamId = value
    if (stream.enabled) {
        model.soopStreamIdUpdated()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamSoopSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("SOOP") })
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            TextEditNavigationView(
                title = localized("Channel name"),
                value = stream.soopChannelName,
                onSubmit = { value -> submitChannelName(model, stream, value) },
                capitalize = true,
            )
            TextEditNavigationView(
                title = localized("Video id"),
                value = stream.soopStreamId,
                onSubmit = { value -> submitStreamId(model, stream, value) },
            )
            Column(horizontalAlignment = Alignment.Start) {
                Text("Very experimental and very secret!")
                Text("")
                Text(
                    "Find your channel name (myChannelName) and video id (myVideoId) in your stream's URL.",
                )
                Text("Example URL: https://play.sooplive.co.kr/myChannelName/myVideoId")
            }
        }
    }
}
