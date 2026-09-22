package com.moblin.android.view.settings.scenes.disconnectprotection

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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsDisconnectProtection
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.view.settings.scenes.SceneNameView
import java.util.UUID

@Composable
fun DisconnectProtectionSettingsView(
    database: Database,
    disconnectProtection: SettingsDisconnectProtection,
    onNavigate: (String) -> Unit,
) {
    TextButton(onClick = { onNavigate("Disconnect protection") }) {
        Text(localized("Disconnect protection"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisconnectProtectionSettingsForm(
    database: Database,
    disconnectProtection: SettingsDisconnectProtection,
) {
    val scenes by database.scenes.collectAsState()
    val liveSceneId by disconnectProtection.liveSceneId.collectAsState()
    val fallbackSceneId by disconnectProtection.fallbackSceneId.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Disconnect protection")) })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                SceneSelectionPicker(
                    label = localized("Live scene"),
                    selection = liveSceneId,
                    scenes = scenes,
                    onSelectionChange = { disconnectProtection.liveSceneId.value = it },
                )
            }
            item {
                SceneSelectionPicker(
                    label = localized("Fallback scene"),
                    selection = fallbackSceneId,
                    scenes = scenes,
                    onSelectionChange = { disconnectProtection.fallbackSceneId.value = it },
                )
            }
            item {
                Text(
                    text = localized(
                        "Can be used when using Moblin as a server at home with stable internet connection.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneSelectionPicker(
    label: String,
    selection: UUID?,
    scenes: List<SettingsScene>,
    onSelectionChange: (UUID?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedScene = scenes.firstOrNull { it.id == selection }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedScene?.name ?: "-- None --",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(localized("-- None --")) },
                onClick = {
                    onSelectionChange(null)
                    expanded = false
                },
            )
            scenes.forEach { scene ->
                DropdownMenuItem(
                    text = { SceneNameView(scene = scene) },
                    onClick = {
                        onSelectionChange(scene.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
