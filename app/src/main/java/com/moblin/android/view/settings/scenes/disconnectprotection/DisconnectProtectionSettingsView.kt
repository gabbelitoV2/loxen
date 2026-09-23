package com.moblin.android.view.settings.scenes.disconnectprotection

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsDisconnectProtection
import java.util.UUID

@Composable
fun DisconnectProtectionSettingsView(
    database: Database,
    disconnectProtection: SettingsDisconnectProtection,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            DisconnectProtectionSettingsForm(
                database = database,
                disconnectProtection = disconnectProtection,
            )
        },
    ) {
        Text(localized("Disconnect protection"))
    }
}

@Composable
fun DisconnectProtectionSettingsForm(
    database: Database,
    disconnectProtection: SettingsDisconnectProtection,
) {
    val scenes = database.scenes
    val options: List<UUID?> = listOf<UUID?>(null) + scenes.map { it.id }
    Form(title = localized("Disconnect protection")) {
        Section(
            footer = localized(
                "Can be used when using Moblin as a server at home with stable internet connection.",
            ),
        ) {
            Picker(
                title = localized("Live scene"),
                selection = disconnectProtection.liveSceneId,
                options = options,
                text = { sceneId -> scenes.firstOrNull { it.id == sceneId }?.name ?: "-- None --" },
                onChange = { disconnectProtection.liveSceneId = it },
            )
            Picker(
                title = localized("Fallback scene"),
                selection = disconnectProtection.fallbackSceneId,
                options = options,
                text = { sceneId -> scenes.firstOrNull { it.id == sceneId }?.name ?: "-- None --" },
                onChange = { disconnectProtection.fallbackSceneId = it },
            )
        }
    }
}
