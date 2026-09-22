package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.SceneSelector
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.view.settings.scenes.scene.SceneWidgetSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.bingocard.WidgetBingoCardQuickButtonControlsView
import com.moblin.android.view.settings.scenes.widgets.widget.pomodorotimer.WidgetPomodoroTimerQuickButtonControlsView
import com.moblin.android.view.settings.scenes.widgets.widget.scoreboard.WidgetScoreboardQuickButtonControlsView
import com.moblin.android.view.settings.scenes.widgets.widget.text.WidgetTextQuickButtonControlsView
import com.moblin.android.view.settings.scenes.widgets.widget.wheelofluck.WidgetWheelOfLuckQuickButtonControlsView
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.view.utils.ScenesShortcutView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.LocalModel

@Composable
private fun WidgetView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    sceneWidget: SettingsSceneWidget,
) {
    Column {
        Row(
            modifier = Modifier.clickable(onClick = {
                TODO("NavigationLink to SceneWidgetSettingsView")
            }),
        ) {
            IconAndTextView(
                image = widget.image(),
                text = widget.name,
                longDivider = true,
            )
            Switch(
                checked = widget.enabled,
                onCheckedChange = { enabled ->
                    widget.enabled = enabled
                    model.reloadSpeechToText()
                    model.sceneUpdated(attachCamera = model.isCaptureDeviceWidget(widget = widget))
                },
            )
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
    Column {
        model.widgetsInCurrentScene(onlyEnabled = false).forEach { widget ->
            WidgetView(
                model = model,
                database = model.database,
                widget = widget.widget,
                sceneWidget = widget.sceneWidget,
            )
        }
        ShortcutSectionView {
            ScenesShortcutView(database = model.database)
        }
    }
}
