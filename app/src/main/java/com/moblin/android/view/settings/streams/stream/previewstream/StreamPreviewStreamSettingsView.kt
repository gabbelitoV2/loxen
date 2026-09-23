package com.moblin.android.view.settings.streams.stream.previewstream

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.settings.SettingsStreamPreviewStream
import com.moblin.android.various.settings.SettingsStreamResolution
import com.moblin.android.view.settings.ingests.rtspclient.UrlSettingsView
import com.moblin.android.view.settings.streams.stream.url.whipExamples
import com.moblin.android.view.utils.TextItemLocalizedView

private fun resolutions(): List<SettingsStreamResolution> =
    listOf(
        SettingsStreamResolution.r854x480,
        SettingsStreamResolution.r640x360,
        SettingsStreamResolution.r426x240,
    )

private fun videoBitrates(): List<Int> =
    listOf(2_000_000, 1_500_000, 1_000_000, 500_000, 250_000)

@Composable
fun StreamPreviewStreamSettingsView(
    previewStream: SettingsStreamPreviewStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Preview stream") {
        Section {
            Text(
                "A low-quality low-latency stream sent to a WHIP server. Can be used to " +
                    "preview the stream from another device.",
            )
        }
        Section {
            NavigationLink(
                destination = {
                    UrlSettingsView(
                        disabled = false,
                        url = previewStream.url,
                        onChangeUrl = { previewStream.url = it },
                        value = previewStream.url,
                        placeholder = "whip://your-server/live",
                        allowedSchemes = listOf("whip", "whips"),
                        examples = whipExamples,
                        onSubmitted = {},
                        onDismiss = {},
                    )
                },
            ) {
                TextItemLocalizedView(name = "URL", value = previewStream.url, sensitive = true)
            }
            Picker(
                title = "Resolution",
                selection = previewStream.resolution,
                options = resolutions(),
                text = { it.shortString() },
                onChange = { previewStream.resolution = it },
            )
            Picker(
                title = "Video bitrate",
                selection = previewStream.bitrate,
                options = videoBitrates(),
                text = { formatBytesPerSecond(speed = it.toLong()) },
                onChange = { previewStream.bitrate = it },
            )
        }
    }
}
