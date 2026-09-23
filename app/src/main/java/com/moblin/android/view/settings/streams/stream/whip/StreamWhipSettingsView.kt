package com.moblin.android.view.settings.streams.stream.whip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsHttpHeader
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamWhip
import com.moblin.android.various.settings.SettingsStreamWhipHttpTransport
import com.moblin.android.view.utils.RemoteControlAssistantShortcutView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.reloadStreamIfEnabled

private fun getBearerToken(headers: List<SettingsHttpHeader>): String {
    val authorization = headers.firstOrNull { it.name == "Authorization" } ?: return ""
    val match = Regex("^Bearer (.*)$").find(authorization.value) ?: return ""
    return match.groupValues[1]
}

private fun setBearerToken(
    model: Model,
    stream: SettingsStream,
    whip: SettingsStreamWhip,
    token: String,
) {
    val value = "Bearer $token"
    val index = whip.headers.indexOfFirst { it.name == "Authorization" }
    if (index != -1) {
        whip.headers[index].value = value
    } else {
        whip.headers.add(SettingsHttpHeader(name = "Authorization", value = value))
    }
    model.reloadStreamIfEnabled(stream = stream)
}

@Composable
fun StreamWhipSettingsView(model: Model = LocalModel.current, stream: SettingsStream, whip: SettingsStreamWhip) {
    val isLive by model.isLive.collectAsState()
    val disabled = stream.enabled && isLive
    Form(title = "WHIP") {
        Section {
            TextEditNavigationView(
                title = localized("Bearer token"),
                value = getBearerToken(whip.headers),
                onSubmit = { token -> setBearerToken(model, stream, whip, token) },
                sensitive = true,
            )
        }
        Section(
            footerContent = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        localized(
                            "Select ${SettingsStreamWhipHttpTransport.standard.toString()} to use " +
                                "standard WHIP.",
                        ),
                    )
                    Text("")
                    Text(
                        localized(
                            "Select ${SettingsStreamWhipHttpTransport.remoteControl.toString()} to exchange " +
                                "connection establishment information via the remote control. Configure this " +
                                "device as remote control assistant, and the device you are streaming to as " +
                                "remote control streamer.",
                        ),
                    )
                }
            },
        ) {
            Picker(
                title = localized("HTTP transport"),
                selection = whip.httpTransport,
                options = SettingsStreamWhipHttpTransport.entries,
                enabled = !disabled,
                onChange = { transport ->
                    whip.httpTransport = transport
                    model.reloadStreamIfEnabled(stream = stream)
                },
            )
        }
        if (whip.httpTransport == SettingsStreamWhipHttpTransport.remoteControl) {
            ShortcutSectionView {
                RemoteControlAssistantShortcutView(model = model)
            }
        }
    }
}
