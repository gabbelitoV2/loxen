package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.SceneSelector
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.bingocard.WidgetBingoCardQuickButtonControlsView
import com.moblin.android.view.settings.scenes.widgets.widget.pomodorotimer.WidgetPomodoroTimerQuickButtonControlsView
import com.moblin.android.view.settings.scenes.widgets.widget.scoreboard.WidgetScoreboardQuickButtonControlsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.WidgetTextQuickButtonControlsView
import com.moblin.android.view.settings.scenes.widgets.widget.wheelofluck.WidgetWheelOfLuckQuickButtonControlsView
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.various.model.getBingoCardEffect
import com.moblin.android.various.model.isCaptureDeviceWidget
import com.moblin.android.various.model.reloadSpeechToText
import com.moblin.android.various.model.sceneUpdated
import com.moblin.android.various.model.sceneWidgetsInSelectedScene

@Composable
private fun WidgetView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
) {
    Column {
        NavigationLink(
            destination = {
                WidgetSettingsView(
                    model = model,
                    database = model.database,
                    widget = widget,
                )
            },
        ) {
            Toggle(
                isOn = widget.enabled,
                onChange = { enabled ->
                    widget.enabled = enabled
                    model.reloadSpeechToText()
                    model.sceneUpdated(attachCamera = model.isCaptureDeviceWidget(widget = widget))
                },
            ) {
                IconAndTextView(
                    image = widget.image(),
                    text = widget.name,
                    longDivider = true,
                )
            }
        }
        when (widget.type) {
            SettingsWidgetType.text ->
                WidgetTextQuickButtonControlsView(
                    model = model,
                    widget = widget,
                    text = widget.text,
                )
            SettingsWidgetType.wheelOfLuck ->
                WidgetWheelOfLuckQuickButtonControlsView(
                    model = model,
                    widget = widget,
                )
            SettingsWidgetType.bingoCard ->
                WidgetBingoCardQuickButtonControlsView(bingoCard = widget.bingoCard) {
                    model.getBingoCardEffect(id = widget.id)?.setSettings(settings = widget.bingoCard)
                }
            SettingsWidgetType.scoreboard ->
                WidgetScoreboardQuickButtonControlsView(
                    model = model,
                    widget = widget,
                    scoreboard = widget.scoreboard,
                )
            SettingsWidgetType.pomodoroTimer ->
                WidgetPomodoroTimerQuickButtonControlsView(pomodoroTimer = widget.pomodoroTimer)
            else -> {}
        }
    }
}

@Composable
fun QuickButtonSceneWidgetsView(
    model: Model = LocalModel.current,
    sceneSelector: SceneSelector,
) {
    val widgets = model.sceneWidgetsInSelectedScene()
    Form(title = "Scene widgets") {
        Section {
            if (widgets.isEmpty()) {
                Text(localized("No widgets in this scene"))
            } else {
                widgets.forEach {
                    WidgetView(model = model, widget = it.widget)
                }
            }
        }
    }
}
