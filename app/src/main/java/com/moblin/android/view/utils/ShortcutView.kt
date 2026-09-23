package com.moblin.android.view.utils

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.ingests.IngestsSettingsView
import com.moblin.android.view.settings.remotecontrol.RemoteControlSettingsWebView
import com.moblin.android.view.settings.remotecontrol.RemoteControlStreamersView
import com.moblin.android.view.settings.scenes.ScenesSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetSettingsView
import com.moblin.android.view.settings.streams.stream.StreamPlatformsSettingsView

@Composable
fun ShortcutSectionView(content: @Composable () -> Unit) {
    Section(header = "Shortcut") {
        content()
    }
}

@Composable
fun WidgetShortcutView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            WidgetSettingsView(model = model, database = database, widget = widget)
        },
    ) {
        Text("Widget")
    }
}

@Composable
fun ScenesShortcutView(
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            ScenesSettingsView(database = database)
        },
    ) {
        Label("Scenes", systemImage = "photo.on.rectangle")
    }
}

@Composable
fun StreamingPlatformsShortcutView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = "Streaming platforms") {
                StreamPlatformsSettingsView(model = model, stream = stream)
            }
        },
    ) {
        Label("Streaming platforms", systemImage = "dot.radiowaves.left.and.right")
    }
}

@Composable
fun RemoteControlWebShortcutView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = "Web") {
                RemoteControlSettingsWebView(
                    model = model,
                    web = model.database.remoteControl.web,
                )
            }
        },
    ) {
        Label("Remote control", systemImage = "appletvremote.gen1")
    }
}

@Composable
fun RemoteControlAssistantShortcutView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = "Remote control assistant") {
                RemoteControlStreamersView(
                    model = model,
                    remoteControlSettings = model.database.remoteControl,
                )
            }
        },
    ) {
        Label("Remote control assistant", systemImage = "appletvremote.gen1")
    }
}

@Composable
fun IngestsShortcutView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            IngestsSettingsView(model = model, database = model.database)
        },
    ) {
        Label("Ingests", systemImage = "server.rack")
    }
}
