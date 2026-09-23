package com.moblin.android.view.settings.ingests.rtmpserver

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.isValidPort
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRtmpServer
import com.moblin.android.various.settings.SettingsRtmpServerStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.utils.randomHumanString
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InfoBannerView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.reloadRtmpServer
import com.moblin.android.various.model.updateMicsListAsync

@Composable
fun RtmpServerSettingsView(
    model: Model = LocalModel.current,
    rtmpServer: SettingsRtmpServer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            RtmpServerSettingsForm(model = model, rtmpServer = rtmpServer)
        },
    ) {
        Text(localized("RTMP server"))
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(rtmpServer))
    }
}

@Composable
fun RtmpServerSettingsForm(
    model: Model = LocalModel.current,
    rtmpServer: SettingsRtmpServer,
) {
    Form(title = localized("RTMP server")) {
        Section {
            Text(
                localized(
                    "The RTMP server allows Moblin to receive video streams over the network. " +
                        "This allows the use of some drones and other cameras as sources.",
                ),
            )
        }
        Section {
            Toggle("Enabled", isOn = rtmpServer.enabled) { newValue ->
                rtmpServer.enabled = newValue
                model.reloadRtmpServer()
            }
        }
        if (rtmpServer.enabled) {
            InfoBannerView(text = localized("Disable the RTMP server to change its settings."))
        }
        Section(
            footer = localized("The TCP port the RTMP server listens for RTMP publishers on."),
        ) {
            TextEditNavigationView(
                title = localized("Port"),
                value = rtmpServer.port.toString(),
                onChange = { isValidPort(it) },
                onSubmit = { submitPort(model, rtmpServer, it) },
                keyboardType = KeyboardType.Number,
            )
        }
        Section(
            header = localized("Streams"),
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        localized(
                            "Each stream can receive video from one RTMP publisher, typically a drone.",
                        ),
                    )
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a stream"))
                }
            },
        ) {
            ForEach(
                rtmpServer.streams,
                id = { it.id },
                onDelete = if (!rtmpServer.enabled) {
                    { offsets -> deleteStream(model, rtmpServer, offsets.toList()) }
                } else {
                    null
                },
            ) { stream ->
                ContextMenuDeleteButton(
                    disabled = rtmpServer.enabled,
                    action = {
                        val index = rtmpServer.streams
                            .indexOfFirst { it.id == stream.id }
                        if (index >= 0) {
                            deleteStream(model, rtmpServer, listOf(index))
                        }
                    },
                ) {
                    RtmpServerStreamSettingsView(
                        status = model.statusOther,
                        rtmpServer = rtmpServer,
                        stream = stream,
                    )
                }
            }
            CreateButtonView {
                val stream = SettingsRtmpServerStream()
                stream.name = makeUniqueName(
                    SettingsRtmpServerStream.baseName,
                    rtmpServer.streams,
                )
                while (true) {
                    stream.streamKey = randomHumanString()
                    if (rtmpServer.streams.none { it.streamKey == stream.streamKey }) {
                        break
                    }
                }
                rtmpServer.streams.add(stream)
                model.updateMicsListAsync()
            }
        }
    }
}

private fun submitPort(model: Model, rtmpServer: SettingsRtmpServer, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port < 0 || port > 65535) {
        return
    }
    rtmpServer.port = port
    model.reloadRtmpServer()
}

private fun status(rtmpServer: SettingsRtmpServer): String {
    return if (rtmpServer.enabled) {
        rtmpServer.streams.size.toString()
    } else {
        "0"
    }
}

private fun deleteStream(model: Model, rtmpServer: SettingsRtmpServer, indexes: List<Int>) {
    for (index in indexes.sortedDescending()) {
        if (index in rtmpServer.streams.indices) {
            rtmpServer.streams.removeAt(index)
        }
    }
    model.reloadRtmpServer()
    model.updateMicsListAsync()
}
