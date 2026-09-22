package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.AutoSceneSwitcherProvider
import com.moblin.android.various.settings.SettingsAutoSceneSwitchers
import com.moblin.android.view.settings.scenes.autoswitchers.AutoSwitchersSelectView
import com.moblin.android.view.settings.scenes.autoswitchers.AutoSwitchersSettingsView
import com.moblin.android.view.utils.ShortcutSectionView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonAutoSceneSwitcherView(
    autoSceneSwitcher: AutoSceneSwitcherProvider,
    autoSceneSwitchers: SettingsAutoSceneSwitchers,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Auto scene switcher")) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Column {
                    AutoSwitchersSelectView(
                        autoSceneSwitcher = autoSceneSwitcher,
                        autoSceneSwitchers = autoSceneSwitchers,
                    )
                    ShortcutSectionView {
                        AutoSwitchersSettingsView(
                            autoSceneSwitchers = autoSceneSwitchers,
                            showSelector = false,
                        )
                    }
                }
            }
        }
    }
}
