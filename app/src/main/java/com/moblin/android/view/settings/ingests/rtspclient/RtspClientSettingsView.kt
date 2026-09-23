package com.moblin.android.view.settings.ingests.rtspclient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsRtspClient
import com.moblin.android.various.settings.SettingsRtspClientStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.various.model.reloadRtspClient

private fun status(numberOfEnabledStreams: Int): String {
    return numberOfEnabledStreams.toString()
}

private fun deleteStream(model: Model, rtspClient: SettingsRtspClient, indexes: Set<Int>) {
    rtspClient.streams = rtspClient.streams.filterIndexed { index, _ ->
        !indexes.contains(index)
    }.toMutableList()
    model.reloadRtspClient()
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
            rtspClient.streams.forEach { stream ->
                key(stream.id) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            RtspClientStreamSettingsView(
                                rtspClient = rtspClient,
                                stream = stream,
                            )
                        }
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                menuExpanded = true
                            },
                        ) {
                            SystemImage(name = "ellipsis", fontSize = 17.sp)
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(localized("Delete")) },
                                    onClick = {
                                        menuExpanded = false
                                        val offset = rtspClient.streams
                                            .indexOfFirst { it.id == stream.id }
                                        if (offset >= 0) {
                                            deleteStream(model, rtspClient, setOf(offset))
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            CreateButtonView {
                val stream = SettingsRtspClientStream()
                stream.name = makeUniqueName(
                    SettingsRtspClientStream.baseName,
                    rtspClient.streams,
                )
                rtspClient.streams = (rtspClient.streams + stream).toMutableList()
            }
        }
    }
}
