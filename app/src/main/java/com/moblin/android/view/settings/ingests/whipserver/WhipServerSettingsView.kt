package com.moblin.android.view.settings.ingests.whipserver

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InfoBannerView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWhipServer
import com.moblin.android.various.settings.SettingsWhipServerStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.utils.randomHumanString
import com.moblin.android.common.various.isValidPort
import com.moblin.android.various.model.getWhipStream
import com.moblin.android.various.model.reloadWhipServer
import com.moblin.android.various.model.updateWhipVideoSourcesAndMics

@Composable
fun WhipServerSettingsView(
    model: Model = LocalModel.current,
    whipServer: SettingsWhipServer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = localized("WHIP server")) {
                Section {
                    Text(
                        localized(
                            "The WHIP server allows Moblin to receive video streams over the network.",
                        ),
                    )
                }
                Section {
                    Toggle(
                        title = localized("Enabled"),
                        isOn = whipServer.enabled,
                        onChange = { value ->
                            whipServer.enabled = value
                            model.reloadWhipServer()
                        },
                    )
                }
                if (whipServer.enabled) {
                    InfoBannerView(
                        text = localized("Disable the WHIP server to change its settings."),
                    )
                }
                Section(
                    footer = localized(
                        "The TCP port the WHIP server listens for WHIP streams on.",
                    ),
                ) {
                    TextEditNavigationView(
                        title = localized("Port"),
                        value = whipServer.port.toString(),
                        onChange = { isValidPort(it) },
                        onSubmit = { submitPort(model, whipServer, it) },
                        keyboardType = KeyboardType.Number,
                        onNavigate = onNavigate,
                    )
                }
                Section(
                    header = localized("Streams"),
                    footerContent = { SwipeLeftToDeleteHelpView(kind = localized("a stream")) },
                ) {
                    ForEach(
                        whipServer.streams,
                        id = { it.id },
                        onDelete = if (!whipServer.enabled) {
                            { offsets -> deleteStream(model, whipServer, offsets) }
                        } else {
                            null
                        },
                    ) { stream ->
                        ContextMenuDeleteButton(
                            disabled = whipServer.enabled,
                            action = {
                                val offsets = streamIndex(whipServer.streams, stream.id)
                                if (offsets != null) {
                                    deleteStream(model, whipServer, setOf(offsets))
                                }
                            },
                        ) {
                            WhipServerStreamSettingsView(
                                status = model.statusOther,
                                whipServer = whipServer,
                                stream = stream,
                            )
                        }
                    }
                    CreateButtonView {
                        if (whipServer.enabled) {
                            return@CreateButtonView
                        }
                        val stream = SettingsWhipServerStream()
                        stream.name = makeUniqueName(
                            name = SettingsWhipServerStream.baseName,
                            existingNames = whipServer.streams,
                        )
                        while (true) {
                            stream.streamKey = randomHumanString()
                            if (model.getWhipStream(streamKey = stream.streamKey) == null) {
                                break
                            }
                        }
                        whipServer.streams.add(stream)
                        model.updateWhipVideoSourcesAndMics()
                    }
                }
            }
        },
    ) {
        Text(localized("WHIP server"))
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(whipServer))
    }
}

private fun streamIndex(streams: List<SettingsWhipServerStream>, id: java.util.UUID): Int? {
    val index = streams.indexOfFirst { it.id == id }
    return if (index == -1) null else index
}

private fun status(whipServer: SettingsWhipServer): String {
    return if (whipServer.enabled) {
        whipServer.streams.size.toString()
    } else {
        "0"
    }
}

private fun submitPort(model: Model, whipServer: SettingsWhipServer, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port < 0 || port > 65535) {
        return
    }
    whipServer.port = port
    model.reloadWhipServer()
}

private fun deleteStream(model: Model, whipServer: SettingsWhipServer, indexes: IndexSet) {
    whipServer.streams.remove(atOffsets = indexes)
    model.reloadWhipServer()
    model.updateWhipVideoSourcesAndMics()
}
