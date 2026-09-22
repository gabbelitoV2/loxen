package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsBitratePreset
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun BitratePresetView(preset: SettingsBitratePreset) {
    Text(text = formatBytesPerSecond(speed = preset.bitrate.toLong()))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BitratePicker(
    bitratePresets: List<SettingsBitratePreset>,
    bitrate: Int,
    onBitrateChange: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = formatBytesPerSecond(speed = bitrate.toLong()),
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            bitratePresets.forEach { preset ->
                DropdownMenuItem(
                    text = { BitratePresetView(preset = preset) },
                    onClick = {
                        onBitrateChange(preset.bitrate)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonBitrateView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val bitrate by stream.bitrate.collectAsState()
    val bitratePresets by database.bitratePresets.collectAsState()
    LaunchedEffect(bitrate) {
        model.setBitrate(bitrate = bitrate)
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = localized("Bitrate")) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (stream !== fallbackStream) {
                item {
                    BitratePicker(
                        bitratePresets = bitratePresets,
                        bitrate = bitrate,
                        onBitrateChange = { stream.bitrate.value = it },
                    )
                }
            }
            item {
                ShortcutSectionView {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("BitratePresetsSettingsView") },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputAntenna,
                            contentDescription = null,
                        )
                        Text(text = localized("Bitrate presets"))
                    }
                }
            }
        }
    }
}
