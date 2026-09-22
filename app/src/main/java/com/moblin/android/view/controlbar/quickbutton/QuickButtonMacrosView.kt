package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.settings.SettingsMacros
import com.moblin.android.various.settings.SettingsMacrosMacro
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun MacroView(model: Model = LocalModel.current, macro: SettingsMacrosMacro) {
    val name = macro.name
    val running = macro.running
    val finished = macro.finished
    val closePanelOnRun = macro.closePanelOnRun
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name)
        Spacer(Modifier.weight(1f))
        if (running) {
            TextButton(
                onClick = {
                    TODO("stopMacro")
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
            ) {
                Text("Cancel")
            }
        } else if (finished) {
            Text("Finished", color = Color.Green)
        } else {
            TextButton(
                onClick = {
                    TODO("startMacro")
                    if (closePanelOnRun) {
                        model.toggleShowingPanel(type = null, panel = ShowingPanel.none)
                    }
                },
            ) {
                if (closePanelOnRun) {
                    Text("Run and close")
                } else {
                    Text("Run")
                }
            }
        }
    }
}

@Composable
fun QuickButtonMacrosView(
    model: Model = LocalModel.current,
    macros: SettingsMacros,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val macrosList = macros.macros
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(macrosList) { macro ->
            MacroView(model = model, macro = macro)
        }
        item {
            Column {
                ShortcutSectionView {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigate("MacrosSettingsView")
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.List, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Macros")
                    }
                }
            }
        }
    }
}
