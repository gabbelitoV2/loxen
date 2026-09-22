package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.DeepLinkCreatorQuickButton
import com.moblin.android.various.settings.DeepLinkCreatorQuickButtons
import com.moblin.android.view.controlbar.controlBarPages
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.IconAndTextView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeepLinkCreatorQuickButtonSettingsView(
    model: Model,
    button: DeepLinkCreatorQuickButton,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DraggableItemPrefixView()
        val quickButton = model.getQuickButton(button.type)
        if (quickButton != null) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = button.enabled,
                        onCheckedChange = { button.enabled = it },
                    )
                    IconAndTextView(
                        image = quickButton.imageOff,
                        text = quickButton.name,
                        longDivider = true,
                    )
                }
                Row {
                    Spacer(Modifier.weight(1f))
                    var pageExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = pageExpanded,
                        onExpandedChange = { pageExpanded = it },
                        modifier = Modifier.width(120.dp),
                    ) {
                        OutlinedTextField(
                            value = button.page.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Page") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = pageExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = pageExpanded,
                            onDismissRequest = { pageExpanded = false },
                        ) {
                            for (page in 1..controlBarPages) {
                                DropdownMenuItem(
                                    text = { Text(page.toString()) },
                                    onClick = {
                                        button.page = page
                                        pageExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text("Unknown")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkCreatorQuickButtonsSettingsView(
    quickButtons: DeepLinkCreatorQuickButtons,
    model: Model,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Quick buttons") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Scroll", modifier = Modifier.weight(1f))
                Switch(
                    checked = quickButtons.enableScroll,
                    onCheckedChange = { quickButtons.enableScroll = it },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Two columns", modifier = Modifier.weight(1f))
                Switch(
                    checked = quickButtons.twoColumns,
                    onCheckedChange = { quickButtons.twoColumns = it },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Show name", modifier = Modifier.weight(1f))
                Switch(
                    checked = quickButtons.showName,
                    onCheckedChange = { quickButtons.showName = it },
                )
            }
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
