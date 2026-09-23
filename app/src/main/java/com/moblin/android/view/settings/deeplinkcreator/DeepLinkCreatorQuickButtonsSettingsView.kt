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
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.move
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
        val enabled = binding({ button.enabled }) { button.enabled = it }
        val page = binding({ button.page }) { button.page = it }
        val quickButton = model.getQuickButton(button.type)
        if (quickButton != null) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Toggle(
                    isOn = enabled.value,
                    onChange = { enabled.value = it },
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
                        selection = page.value,
                        options = (1..controlBarPages).toList(),
                        onChange = { page.value = it },
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
                isOn = binding({ quickButtons.enableScroll }) { quickButtons.enableScroll = it },
            )
            Toggle(
                title = "Two columns",
                isOn = binding({ quickButtons.twoColumns }) { quickButtons.twoColumns = it },
            )
            Toggle(
                title = "Show name",
                isOn = binding({ quickButtons.showName }) { quickButtons.showName = it },
            )
        }
        Section {
            ForEach(
                quickButtons.buttons,
                id = { it.id },
                onMove = { froms, to ->
                    quickButtons.buttons.move(fromOffsets = froms, toOffset = to)
                },
            ) { button ->
                DeepLinkCreatorQuickButtonSettingsView(model = model, button = button)
            }
        }
    }
}
