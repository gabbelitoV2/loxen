package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.settings.SettingsControllerThumbStickFunction
import com.moblin.android.various.settings.color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControllerThumbStickView(
    function: SettingsControllerThumbStickFunction,
    onFunctionChange: (SettingsControllerThumbStickFunction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(localized("Function"))
        Spacer(Modifier.weight(1f))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = function.toString(),
                onValueChange = {},
                readOnly = true,
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
                SettingsControllerThumbStickFunction.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.toString()) },
                        onClick = {
                            onFunctionChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun GameControllersControllerThumbStickSettingsView(
    image: String,
    name: String,
    function: SettingsControllerThumbStickFunction,
    onFunctionChange: (SettingsControllerThumbStickFunction) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Thumb stick") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = TODO("SF Symbol name to Compose ImageVector mapping for \"$image\""),
            contentDescription = null,
        )
        Text(localized(name))
        Spacer(Modifier.weight(1f))
        Text(
            text = function.toString(),
            color = function.color(),
        )
    }
}
