package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.AutoSceneSwitcherProvider
import com.moblin.android.various.settings.SettingsAutoSceneSwitchers
import com.moblin.android.view.settings.scenes.autoswitchers.AutoSwitchersSelectView
import com.moblin.android.view.settings.scenes.autoswitchers.AutoSwitchersSettingsView
import com.moblin.android.view.utils.ShortcutSectionView

@Composable
fun QuickButtonAutoSceneSwitcherView(
    autoSceneSwitcher: AutoSceneSwitcherProvider,
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
) {
    Form(title = "Auto scene switcher") {
        Section {
            AutoSwitchersSelectView(
                autoSceneSwitcher = autoSceneSwitcher,
                autoSceneSwitchers = autoSceneSwitchers,
                pickerStyle = PickerStyle.inline,
            )
        }
        ShortcutSectionView {
            AutoSwitchersSettingsView(
                autoSceneSwitchers = autoSceneSwitchers,
                showSelector = false,
            )
        }
    }
}
