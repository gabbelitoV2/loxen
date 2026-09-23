package com.moblin.android.view.settings.display.quickbuttons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsQuickButton
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.view.controlbar.controlBarPages
import com.moblin.android.view.utils.IconAndTextView

@Composable
private fun AppearanceSettingsView(database: Database, quickButtons: SettingsQuickButtons) {
    val enableScroll by quickButtons.enableScroll.collectAsState()
    val twoColumns by quickButtons.twoColumns.collectAsState()
    val bigButtons by quickButtons.bigButtons.collectAsState()
    val showName by quickButtons.showName.collectAsState()
    Section(
        header = "Appearance",
        footer = "Names are not shown in portrait mode.",
    ) {
        if (database.showAllSettings) {
            Toggle(
                "Scroll",
                isOn = enableScroll,
                onChange = { quickButtons.enableScroll.value = it },
            )
            Toggle(
                "Two columns",
                isOn = twoColumns,
                onChange = { quickButtons.twoColumns.value = it },
            )
        }
        Toggle(
            "Big buttons",
            isOn = bigButtons,
            onChange = { quickButtons.bigButtons.value = it },
        )
        Toggle(
            "Show name",
            isOn = showName,
            onChange = { quickButtons.showName.value = it },
        )
    }
}

@Composable
private fun ButtonSettingsView(model: Model = LocalModel.current, button: SettingsQuickButton) {
    val enabled by button.enabled.collectAsState()
    val isOn by button.isOn.collectAsState()
    NavigationLink(
        destination = {
            val orientation = model.orientation
            QuickButtonsButtonSettingsView(
                model = model,
                orientation = orientation,
                quickButtonsSettings = model.database.quickButtonsGeneral,
                button = button,
                showAll = false,
            )
            DisposableEffect(Unit) {
                model.quickButtons.selectedButtonType.value = button.type
                model.quickButtons.page = button.page.value
                model.quickButtons.activePage.value = button.page.value
                onDispose {
                    if (model.showingPanel.value != ShowingPanel.quickButtonSettings) {
                        model.quickButtons.selectedButtonType.value = null
                    }
                }
            }
        },
    ) {
        Toggle(
            isOn = enabled,
            onChange = {
                button.enabled.value = it
                model.updateQuickButtonPairs()
            },
            enabled = !(isOn && enabled),
        ) {
            IconAndTextView(
                image = button.imageOff,
                text = button.name,
                longDivider = true,
            )
        }
    }
}

@Composable
private fun ButtonsSettingsView(model: Model = LocalModel.current, database: Database) {
    val quickButtons = database.quickButtons
    for (page in 1..controlBarPages) {
        Section(header = "Page $page") {
            for (button in quickButtons.reversed().filter { it.page.value == page }) {
                ButtonSettingsView(
                    model = model,
                    button = button,
                )
            }
        }
    }
}

@Composable
fun QuickButtonsSettingsView(
    model: Model = LocalModel.current,
    showAll: Boolean,
    onNavigate: (String) -> Unit = {},
) {
    Form(title = "Quick buttons") {
        AppearanceSettingsView(
            database = model.database,
            quickButtons = model.database.quickButtonsGeneral,
        )
        if (showAll) {
            ButtonsSettingsView(
                model = model,
                database = model.database,
            )
        }
    }
}
