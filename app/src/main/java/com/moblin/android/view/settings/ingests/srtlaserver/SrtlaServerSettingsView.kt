package com.moblin.android.view.settings.ingests.srtlaserver

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
import com.moblin.android.various.settings.SettingsSrtlaServer
import com.moblin.android.various.settings.SettingsSrtlaServerStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.various.utils.randomHumanString
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.InfoBannerView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.getSrtlaStream
import com.moblin.android.various.model.reloadSrtlaServer
import com.moblin.android.various.model.srtlaServerEnabled
import com.moblin.android.various.model.updateSrtlaVideoSourcesAndMics

private fun submitSrtPort(srtlaServer: SettingsSrtlaServer, model: Model, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port !in 0..65535) {
        return
    }
    srtlaServer.srtPort = port
    model.reloadSrtlaServer()
}

private fun submitSrtlaPort(srtlaServer: SettingsSrtlaServer, model: Model, value: String) {
    val port = value.toIntOrNull() ?: return
    if (port !in 0..65535) {
        return
    }
    srtlaServer.srtlaPort = port
    model.reloadSrtlaServer()
}

private fun status(srtlaServer: SettingsSrtlaServer): String {
    return if (srtlaServer.enabled) {
        srtlaServer.streams.count().toString()
    } else {
        "0"
    }
}

private fun deleteStream(srtlaServer: SettingsSrtlaServer, model: Model, indexes: List<Int>) {
    indexes.sortedDescending().forEach { index ->
        if (index in srtlaServer.streams.indices) {
            srtlaServer.streams.removeAt(index)
        }
    }
    model.reloadSrtlaServer()
    model.updateSrtlaVideoSourcesAndMics()
}

@Composable
fun SrtlaServerSettingsView(
    model: Model = LocalModel.current,
    srtlaServer: SettingsSrtlaServer,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(destination = { SrtlaServerSettingsForm(model = model, srtlaServer = srtlaServer) }) {
        Text(localized("SRT(LA) server"))
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(srtlaServer))
    }
}

@Composable
fun SrtlaServerSettingsForm(
    model: Model = LocalModel.current,
    srtlaServer: SettingsSrtlaServer,
) {
    Form(title = localized("SRT(LA) server")) {
        Section {
            Toggle(localized("Enabled"), isOn = srtlaServer.enabled) { enabled ->
                srtlaServer.enabled = enabled
                model.reloadSrtlaServer()
            }
        }
        if (srtlaServer.enabled) {
            InfoBannerView(text = localized("Disable the SRT(LA) server to change its settings."))
        }
        Section(
            footer = localized("The UDP port the SRT(LA) server listens for SRT publishers on."),
        ) {
            TextEditNavigationView(
                title = localized("SRT port"),
                value = srtlaServer.srtPort.toString(),
                onChange = { value -> isValidPort(value) },
                onSubmit = { value -> submitSrtPort(srtlaServer, model, value) },
                keyboardType = KeyboardType.Number,
            )
        }
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("The UDP port the SRT(LA) server listens for SRTLA publishers on."))
                    Text("")
                    Text("The UDP port ${srtlaServer.srtlaSrtPort()} will also be used.")
                }
            },
        ) {
            TextEditNavigationView(
                title = localized("SRTLA port"),
                value = srtlaServer.srtlaPort.toString(),
                onChange = { value -> isValidPort(value) },
                onSubmit = { value -> submitSrtlaPort(srtlaServer, model, value) },
                keyboardType = KeyboardType.Number,
            )
        }
        Section(
            header = localized("Streams"),
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("Each stream can receive video from one SRT(LA) publisher."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a stream"))
                }
            },
        ) {
            ForEach(
                srtlaServer.streams,
                id = { it.id },
                onDelete = if (!model.srtlaServerEnabled()) {
                    { offsets -> deleteStream(srtlaServer, model, offsets.toList()) }
                } else {
                    null
                },
            ) { stream ->
                ContextMenuDeleteButton(
                    disabled = model.srtlaServerEnabled(),
                    action = {
                        val index = srtlaServer.streams.indexOfFirst { it.id == stream.id }
                        if (index != -1) {
                            deleteStream(srtlaServer, model, listOf(index))
                        }
                    },
                ) {
                    SrtlaServerStreamSettingsView(
                        status = model.statusOther,
                        srtlaServer = srtlaServer,
                        stream = stream,
                    )
                }
            }
            CreateButtonView {
                val stream = SettingsSrtlaServerStream()
                stream.name = makeUniqueName(
                    name = SettingsSrtlaServerStream.baseName,
                    existingNames = srtlaServer.streams,
                )
                while (true) {
                    stream.streamId = randomHumanString()
                    if (model.getSrtlaStream(stream.streamId) == null) {
                        break
                    }
                }
                srtlaServer.streams.add(stream)
                model.updateSrtlaVideoSourcesAndMics()
            }
        }
    }
}
