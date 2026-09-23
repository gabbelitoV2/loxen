package com.moblin.android.view.settings.streams.stream.soop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.soopChannelNameUpdated
import com.moblin.android.various.model.soopStreamIdUpdated

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

@Composable
fun StreamSoopSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    Form(title = "SOOP") {
        Section(footerContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(localized("Very experimental and very secret!"))
                Text("")
                Text(
                    localized(
                        "Find your channel name (myChannelName) and video id (myVideoId) in your stream's URL.",
                    ),
                )
                Text(localized("Example URL: https://play.sooplive.co.kr/myChannelName/myVideoId"))
            }
        }) {
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
        }
    }
}
