package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.startMacro
import com.moblin.android.various.model.stopMacro
import com.moblin.android.various.settings.SettingsMacros
import com.moblin.android.various.settings.SettingsMacrosMacro
import com.moblin.android.view.settings.macros.MacrosSettingsView
import com.moblin.android.view.utils.ShortcutSectionView

@Composable
private fun MacroView(model: Model = LocalModel.current, macro: SettingsMacrosMacro) {
    val name = macro.name
    val running = macro.running
    val finished = macro.finished
    val closePanelOnRun = macro.closePanelOnRun
    val palette = formPalette()
    FormRow {
        Text(name, style = formBodyStyle)
        Spacer(Modifier.weight(1f))
        if (running) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Text(
                "Cancel",
                modifier = Modifier
                    .alpha(if (pressed) 0.2f else 1f)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        model.stopMacro(macro = macro)
                    },
                color = palette.red,
                style = formBodyStyle,
            )
        } else if (finished) {
            Text("Finished", color = palette.green, style = formBodyStyle)
        } else {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Text(
                if (closePanelOnRun) "Run and close" else "Run",
                modifier = Modifier
                    .alpha(if (pressed) 0.2f else 1f)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        model.startMacro(macro = macro)
                        if (closePanelOnRun) {
                            model.toggleShowingPanel(type = null, panel = ShowingPanel.none)
                        }
                    },
                color = palette.accent,
                style = formBodyStyle,
            )
        }
    }
}

@Composable
fun QuickButtonMacrosView(
    model: Model = LocalModel.current,
    macros: SettingsMacros,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Macros") {
        Section {
            macros.macros.forEach { macro ->
                MacroView(model = model, macro = macro)
            }
        }
        ShortcutSectionView {
            NavigationLink(
                destination = {
                    MacrosSettingsView(
                        model = model,
                        database = model.database,
                        macros = macros,
                    )
                },
            ) {
                Label("Macros", systemImage = "increase.indent")
            }
        }
    }
}
