package com.moblin.android.view.settings.camera.videostabilization

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoStabilizationMode
import com.moblin.android.various.settings.videoStabilizationModes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoStabilizationSettingsView(
    model: Model,
    mode: SettingsVideoStabilizationMode,
    onModeChange: (SettingsVideoStabilizationMode) -> Unit = {},
) {
    var modeState by remember { mutableStateOf(mode) }
    var applied by remember { mutableStateOf(mode) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(modeState) {
        if (modeState != applied) {
            applied = modeState
            model.database.videoStabilizationMode = modeState
            model.reattachCamera()
            onModeChange(modeState)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = localized("Video stabilization"),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = modeState.toString(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .width(180.dp),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                videoStabilizationModes.forEach { modeOption ->
                    DropdownMenuItem(
                        text = {
                            Text(modeOption.toString())
                        },
                        onClick = {
                            modeState = modeOption
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
