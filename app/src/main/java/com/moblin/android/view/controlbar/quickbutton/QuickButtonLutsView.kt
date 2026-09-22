package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsColor
import com.moblin.android.various.settings.SettingsColorLut
import com.moblin.android.view.utils.ShortcutSectionView

@Composable
private fun LutView(model: Model, lut: SettingsColorLut) {
    val enabled by lut.enabled.collectAsState()
    var previousEnabled by remember { mutableStateOf(enabled) }
    LaunchedEffect(enabled) {
        if (enabled != previousEnabled) {
            previousEnabled = enabled
            model.sceneUpdated(updateRemoteScene = false)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = lut.name,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enabled,
            onCheckedChange = { lut.setEnabled(it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonLutsView(model: Model, color: SettingsColor, onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("LUTs") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            items(color.allLuts()) { lut ->
                LutView(model = model, lut = lut)
            }
            item {
                ShortcutSectionView {
                    Row(
                        modifier = Modifier.clickable { onNavigate("CameraSettingsLutsView") },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                        )
                        Text("LUTs")
                    }
                }
            }
        }
    }
}
