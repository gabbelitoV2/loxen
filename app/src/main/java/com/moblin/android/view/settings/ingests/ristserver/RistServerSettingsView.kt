package com.moblin.android.view.settings.ingests.ristserver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.moblin.android.various.settings.SettingsRistServer
import com.moblin.android.various.settings.SettingsRistServerStream
import com.moblin.android.various.utils.Identifiable
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InfoBannerView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID
import com.moblin.android.various.model.reloadRistServer
import com.moblin.android.various.model.ristServerEnabled
import com.moblin.android.various.model.updateRistVideoSourcesAndMics

@Composable
fun RistServerSettingsView(
    model: Model = LocalModel.current,
    ristServer: SettingsRistServer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            RistServerSettingsDetailView(model = model, ristServer = ristServer)
        },
    ) {
        Text(localized("RIST server"))
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(ristServer))
    }
}

@Composable
fun RistServerSettingsDetailView(
    model: Model = LocalModel.current,
    ristServer: SettingsRistServer,
) {
    Form(title = localized("RIST server")) {
        Section {
            Text(
                localized(
                    "The RIST server allows Moblin to receive video streams over the network.",
                ),
            )
        }
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = ristServer.enabled,
                onChange = { value ->
                    ristServer.enabled = value
                    model.reloadRistServer()
                },
            )
        }
        if (ristServer.enabled) {
            InfoBannerView(text = localized("Disable the RIST server to change its settings."))
        }
        Section(
            footer = localized("The UDP port the RIST server listens for RIST publishers on."),
        ) {
            Box(modifier = Modifier.alpha(if (ristServer.enabled) 0.5f else 1f)) {
                TextEditNavigationView(
                    title = localized("Port"),
                    value = ristServer.port.toString(),
                    onChange = { value -> isValidPort(value) },
                    onSubmit = { value -> submitPort(model, ristServer, value) },
                    keyboardType = KeyboardType.Number,
                )
            }
        }
        Section(
            header = localized("Streams"),
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("Each stream can receive video from one RIST publisher."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a stream"))
                }
            },
        ) {
            ForEach(
                ristServer.streams,
                id = { it.id },
                onDelete = if (!ristServer.enabled) {
                    { offsets -> deleteStream(model, ristServer, offsets) }
                } else {
                    null
                },
            ) { stream ->
                ContextMenuDeleteButton(
                    disabled = ristServer.enabled,
                    action = {
                        makeOffsets(
                            ristServer.streams.map {
                                IdentifiedStream(it.id)
                            },
                            stream.id,
                        )?.let { offset ->
                            deleteStream(model, ristServer, setOf(offset))
                        }
                    },
                ) {
                    RistServerStreamSettingsView(
                        status = model.statusOther,
                        ristServer = ristServer,
                        stream = stream,
                    )
                }
            }
            Box(modifier = Modifier.alpha(if (model.ristServerEnabled()) 0.5f else 1f)) {
                CreateButtonView(
                    action = {
                        val stream = SettingsRistServerStream()
                        stream.name = makeUniqueName(
                            SettingsRistServerStream.baseName,
                            ristServer.streams,
                        )
                        stream.virtualDestinationPort =
                            ristServer.makeUniqueVirtualDestinationPort()
                        ristServer.streams.add(stream)
                        model.updateRistVideoSourcesAndMics()
                    },
                )
            }
        }
    }
}

private fun submitPort(model: Model, ristServer: SettingsRistServer, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port !in 0..65535) {
        return
    }
    ristServer.port = port
    model.reloadRistServer()
}

private fun status(ristServer: SettingsRistServer): String {
    return if (ristServer.enabled) {
        ristServer.streams.size.toString()
    } else {
        "0"
    }
}

private fun deleteStream(model: Model, ristServer: SettingsRistServer, indexes: Set<Int>) {
    for (index in indexes.sortedDescending()) {
        if (index in ristServer.streams.indices) {
            ristServer.streams.removeAt(index)
        }
    }
    model.reloadRistServer()
    model.updateRistVideoSourcesAndMics()
}

private data class IdentifiedStream(override val id: UUID) : Identifiable<UUID>
