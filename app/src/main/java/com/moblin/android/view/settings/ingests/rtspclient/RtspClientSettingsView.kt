package com.moblin.android.view.settings.ingests.rtspclient

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRtspClient
import com.moblin.android.various.settings.SettingsRtspClientStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.various.model.reloadRtspClient
import com.moblin.android.various.model.updateRtspVideoSources

private fun status(numberOfEnabledStreams: Int): String {
    return numberOfEnabledStreams.toString()
}

private fun deleteStream(model: Model, rtspClient: SettingsRtspClient, indexes: Set<Int>) {
    rtspClient.streams = rtspClient.streams.filterIndexed { index, _ ->
        !indexes.contains(index)
    }.toMutableList()
    model.reloadRtspClient()
    model.updateRtspVideoSources()
}

@Composable
fun RtspClientSettingsView(
    model: Model = LocalModel.current,
    rtspClient: SettingsRtspClient,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            RtspClientSettingsViewDestination(
                model = model,
                rtspClient = rtspClient,
                onNavigate = onNavigate,
            )
        },
    ) {
        Text(localized("RTSP client"))
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(rtspClient.streams.count { it.enabled }))
    }
}

@Composable
fun RtspClientSettingsViewDestination(
    model: Model = LocalModel.current,
    rtspClient: SettingsRtspClient,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "RTSP client") {
        Section(
            header = "Streams",
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a stream"))
            },
        ) {
            ForEach(
                rtspClient.streams,
                id = { it.id },
                onDelete = { offsets ->
                    deleteStream(model, rtspClient, offsets)
                },
            ) { stream ->
                ContextMenuDeleteButton(
                    action = {
                        val offset = rtspClient.streams
                            .indexOfFirst { it.id == stream.id }
                        if (offset >= 0) {
                            deleteStream(model, rtspClient, setOf(offset))
                        }
                    },
                ) {
                    RtspClientStreamSettingsView(
                        model = model,
                        rtspClient = rtspClient,
                        stream = stream,
                    )
                }
            }
            CreateButtonView {
                val stream = SettingsRtspClientStream()
                stream.name = makeUniqueName(
                    SettingsRtspClientStream.baseName,
                    rtspClient.streams,
                )
                rtspClient.streams = (rtspClient.streams + stream).toMutableList()
                model.updateRtspVideoSources()
            }
        }
    }
}
