package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.DeepLinkCreatorQuickButton
import com.moblin.android.various.settings.DeepLinkCreatorQuickButtons
import com.moblin.android.view.controlbar.controlBarPages
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.localized

@Composable
private fun DeepLinkCreatorQuickButtonSettingsView(
    model: Model = LocalModel.current,
    button: DeepLinkCreatorQuickButton,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DraggableItemPrefixView()
        val quickButton = model.getQuickButton(button.type)
        if (quickButton != null) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Toggle(
                    isOn = button.enabled,
                    onChange = { button.enabled = it },
                ) {
                    IconAndTextView(
                        image = quickButton.imageOff,
                        text = quickButton.name,
                        longDivider = true,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Spacer(Modifier.weight(1f))
                    Picker(
                        title = "Page",
                        selection = button.page,
                        options = (1..controlBarPages).toList(),
                        onChange = { button.page = it },
                    )
                }
            }
        } else {
            Text(localized("Unknown"))
        }
    }
}

@Composable
fun DeepLinkCreatorQuickButtonsSettingsView(
    quickButtons: DeepLinkCreatorQuickButtons,
    model: Model = LocalModel.current,
) {
    Form(title = "Quick buttons") {
        Section(header = "Appearance") {
            Toggle(
                title = "Scroll",
                isOn = quickButtons.enableScroll,
            ) {
                quickButtons.enableScroll = it
            }
            Toggle(
                title = "Two columns",
                isOn = quickButtons.twoColumns,
            ) {
                quickButtons.twoColumns = it
            }
            Toggle(
                title = "Show name",
                isOn = quickButtons.showName,
            ) {
                quickButtons.showName = it
            }
        }
        Section {
            quickButtons.buttons.forEach { button ->
                DeepLinkCreatorQuickButtonSettingsView(model = model, button = button)
            }
        }
    }
}

private fun moveButtons(
    buttons: MutableList<DeepLinkCreatorQuickButton>,
    froms: List<Int>,
    toOffset: Int,
) {
    val moving = froms.map { buttons[it] }
    val target = toOffset - froms.count { it < toOffset }
    val remaining = buttons.filterIndexed { index, _ -> index !in froms }.toMutableList()
    remaining.addAll(target, moving)
    buttons.clear()
    buttons.addAll(remaining)
}
