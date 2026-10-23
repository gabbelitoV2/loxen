package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.common.various.color
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsMoblinCloud
import com.moblin.android.videoeffects.alerts.AlertsEffectAlert
import com.moblin.android.view.utils.TextButtonView

@Composable
private fun MoblinCloudTipsView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    Form(title = "Message") {
        Section {
            Toggle(
                title = "Enabled",
                isOn = alert.enabled,
                onChange = { value ->
                    alert.enabled = value
                    model.updateAlertsSettings()
                },
            )
        }
        AlertMediaView(model = model, alert = alert)
        AlertPositionView(model = model, alert = alert)
        AlertColorsView(
            model = model,
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color(),
        )
        AlertFontView(
            model = model,
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight,
        )
        AlertTextToSpeechView(model = model, alert = alert)
        Section {
            TextButtonView("Test") {
                model.testAlert(
                    AlertsEffectAlert.MoblinCloudTip(
                        name = alertTestNames.random(),
                        message = "",
                        amount = (1..100).random().toString(),
                        currency = "USD",
                    ),
                )
            }
        }
    }
}

@Composable
fun WidgetAlertsMoblinCloudSettingsView(moblinCloud: SettingsWidgetAlertsMoblinCloud) {
    Form(title = "Moblin Cloud") {
        Section {
            NavigationLink(title = "Message") {
                MoblinCloudTipsView(alert = moblinCloud.tips)
            }
        }
    }
}
