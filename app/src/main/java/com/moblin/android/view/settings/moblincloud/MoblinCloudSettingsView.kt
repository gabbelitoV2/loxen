package com.moblin.android.view.settings.moblincloud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.isValidHttpUrl
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.AssetImage
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.uikit.UIApplication
import com.moblin.android.platform.uikit.open
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.moblinCloudApiUrl
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AssetImage(
                    name = "MoblinCloudLogo",
                    modifier = Modifier.height(110.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    localized("Moblin Cloud"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    localized("Let viewers interact with your stream from your own page."),
                    fontSize = 15.sp,
                    color = formPalette().secondaryLabel,
                    textAlign = TextAlign.Center
                )
            }
        }
        Section(header = localized("Get started")) {
            Label(
                title = localized("Sign in at cloud.moblin.app and set up your page."),
                systemImage = "1.circle.fill"
            )
            Label(
                title = localized(
                    "Scan the QR code in the dashboard's Moblin section, or paste your Moblin Cloud key below."
                ),
                systemImage = "2.circle.fill"
            )
            Label(
                title = localized(
                    "Pick the alerts widget that shows messages and the wheel of luck widget that viewers spin."
                ),
                systemImage = "3.circle.fill"
            )
        }
        Section {
            Button(title = localized("Read the guide"), systemImage = "book") {
                UIApplication.shared.open("https://moblin.app/docs/#guides/moblin-cloud")
            }
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
        Section(
            footer = localized(
                "Alerts widget that shows messages on stream. Messages always show in chat. " +
                    "Create one in Settings → Scenes and add it to the scenes you stream with."
            )
        ) {
            Picker(
                title = "Message",
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
        Section(
            footer = localized(
                "Wheel of luck widget that viewers spin. Viewers can only spin while one is picked. " +
                    "Create one in Settings → Scenes and add it to the scenes you stream with."
            )
        ) {
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
            footer = localized("Moblin Cloud API server. Default is $moblinCloudApiUrl.")
        ) {
            TextEditNavigationView(
                title = localized("URL"),
                value = moblinCloud.url,
                onChange = { isValidHttpUrl(it) },
                onSubmit = {
                    moblinCloud.url = it
                    model.reloadMoblinCloud()
                }
            )
        }
    }
}
