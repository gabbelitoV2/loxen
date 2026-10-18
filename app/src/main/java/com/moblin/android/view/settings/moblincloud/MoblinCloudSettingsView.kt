package com.moblin.android.view.settings.moblincloud

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.moblinCloudSendSongRequests
import com.moblin.android.various.model.moblinCloudSendWheelOfLuck
import com.moblin.android.various.model.reloadMoblinCloud
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsMoblinCloud
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID
import com.moblin.android.LocalModel

@Composable
fun MoblinCloudSettingsView(model: Model = LocalModel.current, database: Database, moblinCloud: SettingsMoblinCloud) {
    Form(title = localized("Moblin Cloud")) {
        Section {
            Text(
                localized(
                    "Let viewers interact with your stream from your page at https://moblin.app. " +
                        "Sign in at https://moblin.app/dashboard to get your Moblin Cloud key."
                )
            )
        }
        Section(footer = localized("Keep the Moblin Cloud key to yourself.")) {
            Toggle(
                "Enabled",
                isOn = moblinCloud.enabled,
                onChange = { enabled ->
                    moblinCloud.enabled = enabled
                    model.reloadMoblinCloud()
                }
            )
            TextEditNavigationView(
                title = localized("Moblin Cloud key"),
                value = moblinCloud.key,
                onSubmit = {
                    moblinCloud.key = it
                    model.reloadMoblinCloud()
                },
                sensitive = true
            )
        }
        val alertsWidgets = database.widgets.filter { it.type == SettingsWidgetType.alerts }
        val alertsOptions: List<UUID?> = listOf<UUID?>(null) + alertsWidgets.map { it.id }
        Section(footer = localized("Alerts widget that shows tips.")) {
            Picker(
                title = "Tips",
                selection = moblinCloud.alertsWidgetId,
                options = alertsOptions,
                text = { id ->
                    if (id == null) {
                        localized("-- None --")
                    } else {
                        database.widgets.firstOrNull { it.id == id }?.name ?: ""
                    }
                },
                onChange = { id ->
                    moblinCloud.alertsWidgetId = id
                }
            )
        }
        val wheelOfLuckWidgets = database.widgets.filter { it.type == SettingsWidgetType.wheelOfLuck }
        val wheelOfLuckOptions: List<UUID?> = listOf<UUID?>(null) + wheelOfLuckWidgets.map { it.id }
        Section(footer = localized("Wheel of luck widget viewers can spin.")) {
            Picker(
                title = "Wheel of luck",
                selection = moblinCloud.wheelOfLuckWidgetId,
                options = wheelOfLuckOptions,
                text = { id ->
                    if (id == null) {
                        localized("-- None --")
                    } else {
                        database.widgets.firstOrNull { it.id == id }?.name ?: ""
                    }
                },
                onChange = { id ->
                    moblinCloud.wheelOfLuckWidgetId = id
                    model.moblinCloudSendWheelOfLuck()
                }
            )
        }
        Section(
            footer = localized(
                "Let viewers add songs to your Apple Music queue. " +
                    "The queue is still shown when disabled."
            )
        ) {
            Toggle(
                "Song requests",
                isOn = moblinCloud.songRequests,
                onChange = { songRequests ->
                    moblinCloud.songRequests = songRequests
                    model.moblinCloudSendSongRequests()
                }
            )
        }
    }
}
