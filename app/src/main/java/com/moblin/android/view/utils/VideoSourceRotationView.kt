package com.moblin.android.view.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoSourceRotationView(
    selectedRotation: Double,
    onSelectedRotationChange: (Double) -> Unit,
) {
    val rotations = listOf(0.0, 90.0, 180.0, 270.0)
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = "${selectedRotation.toInt()}°",
            onValueChange = {},
            readOnly = true,
            label = { Text("Rotation") },
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
            rotations.forEach { rotation ->
                DropdownMenuItem(
                    text = { Text("${rotation.toInt()}°") },
                    onClick = {
                        onSelectedRotationChange(rotation)
                        expanded = false
                    },
                )
            }
        }
    }
}
