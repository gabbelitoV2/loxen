package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectType
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.utils.CloseToolbarButtonView
import com.moblin.android.view.utils.TextButtonView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetEffectWizardSettingsView(
    model: Model,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
) {
    val effectType by effect.type.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create effect wizard") },
                actions = {
                    CloseToolbarButtonView {
                        onChangePresentingCreateWizard(false)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            item {
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = effectType.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        for (type in SettingsVideoEffectType.entries) {
                            DropdownMenuItem(
                                text = { Text(type.toString()) },
                                onClick = {
                                    effect.type.value = type
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                Column {
                    TextButtonView("Create") {
                        create(
                            model = model,
                            widget = widget,
                            effect = effect,
                            onChangePresentingCreateWizard = onChangePresentingCreateWizard,
                        )
                    }
                }
            }
        }
    }
}

private fun create(
    model: Model,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
) {
    onChangePresentingCreateWizard(false)
    widget.effects.add(effect)
    model.resetSelectedScene(changeScene = false)
}
