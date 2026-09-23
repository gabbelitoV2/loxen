package com.moblin.android.view.settings.streams.stream.obsremotecontrol

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.common.various.cleanUrl
import com.moblin.android.common.various.isValidWebSocketUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.obsWebSocketPasswordUpdated
import com.moblin.android.various.model.obsWebSocketUrlUpdated

private fun submitWebSocketUrl(model: Model, stream: SettingsStream, value: String) {
    val url = cleanUrl(value)
    val message = isValidWebSocketUrl(url)
    if (message != null) {
        model.makeErrorToast(message)
        return
    }
    stream.obsWebSocketUrl = url
    if (stream.enabled) {
        model.obsWebSocketUrlUpdated()
    }
}

private fun submitWebSocketPassword(model: Model, stream: SettingsStream, value: String) {
    stream.obsWebSocketPassword = value
    if (stream.enabled) {
        model.obsWebSocketPasswordUpdated()
    }
}

private fun submitSourceName(stream: SettingsStream, value: String) {
    stream.obsSourceName = value
}

private fun submitBrbScene(stream: SettingsStream, value: String) {
    stream.obsBrbScene = value
}

private fun submitMainScene(stream: SettingsStream, value: String) {
    stream.obsMainScene = value
}

@Composable
fun StreamObsRemoteControlSettingsInnerView(model: Model = LocalModel.current, stream: SettingsStream) {
    val showAllSettings = model.database.showAllSettings
    Section(header = localized("WebSocket")) {
        TextEditNavigationView(
            title = localized("URL"),
            value = stream.obsWebSocketUrl,
            onChange = ::isValidWebSocketUrl,
            onSubmit = { submitWebSocketUrl(model, stream, it) },
            footers = listOf(localized("For example ws://232.32.45.332:4567."))
        )
        TextEditNavigationView(
            title = localized("Password"),
            value = stream.obsWebSocketPassword,
            onSubmit = { submitWebSocketPassword(model, stream, it) },
            sensitive = true
        )
    }
    Section(
        footer = localized(
            "Moblin will periodically try to switch to the BRB scene if the stream is " +
                "likely broken, and back to the main scene once everything seems to work again."
        )
    ) {
        TextEditNavigationView(
            title = localized("Main scene"),
            value = stream.obsMainScene,
            onSubmit = { submitMainScene(stream, it) },
            capitalize = true
        )
        TextEditNavigationView(
            title = localized("BRB scene"),
            value = stream.obsBrbScene,
            onSubmit = { submitBrbScene(stream, it) },
            capitalize = true
        )
        Toggle(
            localized("Streaming directly to OBS"),
            isOn = stream.streamingDirectlyToObs,
            onChange = { value -> stream.streamingDirectlyToObs = value }
        )
    }
    if (showAllSettings) {
        Section(
            footer = localized(
                "Moblin will switch to the BRB scene configured above when the current scene's " +
                    "SRT(LA) or RTMP video source is disconnected. Typically enable when using Moblin " +
                    "as SRT(LA) server at home, streaming to OBS on the same computer."
            )
        ) {
            Toggle(
                localized("BRB scene when video source is broken"),
                isOn = stream.obsBrbSceneVideoSourceBroken,
                enabled = stream.obsBrbScene.isNotEmpty(),
                onChange = { value -> stream.obsBrbSceneVideoSourceBroken = value }
            )
        }
    }
    Section(
        footer = localized("The name of the Source in OBS that receives the stream from Moblin.")
    ) {
        TextEditNavigationView(
            title = localized("Source name"),
            value = stream.obsSourceName,
            onSubmit = { submitSourceName(stream, it) },
            capitalize = true
        )
    }
    if (showAllSettings) {
        Section {
            Toggle(
                localized("Auto start streaming when going live"),
                isOn = stream.obsAutoStartStream,
                onChange = { value -> stream.obsAutoStartStream = value }
            )
            Toggle(
                localized("Auto stop streaming when ending stream"),
                isOn = stream.obsAutoStopStream,
                onChange = { value -> stream.obsAutoStopStream = value }
            )
        }
        Section {
            Toggle(
                localized("Auto start recording when going live"),
                isOn = stream.obsAutoStartRecording,
                onChange = { value -> stream.obsAutoStartRecording = value }
            )
            Toggle(
                localized("Auto stop recording when ending stream"),
                isOn = stream.obsAutoStopRecording,
                onChange = { value -> stream.obsAutoStopRecording = value }
            )
        }
    }
}

@Composable
fun StreamObsRemoteControlSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    Form(title = localized("OBS remote control")) {
        StreamObsRemoteControlSettingsInnerView(model = model, stream = stream)
    }
}
