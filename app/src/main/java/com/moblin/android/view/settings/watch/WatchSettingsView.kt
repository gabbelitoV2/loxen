package com.moblin.android.view.settings.watch

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.moblinwatch.shared.WatchSettings
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.view.settings.watch.chat.WatchChatSettingsView
import com.moblin.android.view.settings.watch.display.WatchDisplaySettingsView

@Composable
fun WatchSettingsView(
    model: Model = LocalModel.current,
    watch: WatchSettings,
    onViaRemoteControlChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val viaRemoteControl by watch.viaRemoteControl.collectAsState()
    Form(title = "Apple Watch") {
        Section {
            NavigationLink(
                destination = {
                    WatchChatSettingsView(
                        model = model,
                        chat = com.moblin.android.view.settings.watch.chat.WatchSettingsChat().apply {
                            fontSize.value = watch.chat.fontSize.value
                            timestampEnabled.value = watch.chat.timestampEnabled.value
                            badges.value = watch.chat.badges.value
                            notificationOnMessage.value = watch.chat.notificationOnMessage.value
                            notificationRate.value = watch.chat.notificationRate.value
                        },
                    )
                },
            ) {
                Text("Chat")
            }
            NavigationLink(
                destination = {
                    WatchDisplaySettingsView(show = watch.show)
                },
            ) {
                Text("Display")
            }
        }
        Section(
            footer = "The watch acts as remote control assistant when enabled. Please note " +
                "that in this case, chat, skip current TTS and a few other features are not " +
                "supported.",
        ) {
            Toggle("Remote control assistant", isOn = viaRemoteControl) { value ->
                onViaRemoteControlChange(value)
            }
        }
    }
}
