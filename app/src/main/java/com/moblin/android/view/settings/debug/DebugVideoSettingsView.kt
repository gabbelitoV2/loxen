package com.moblin.android.view.settings.debug

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun onPixelFormatChange(model: Model, format: String) {
    model.database.debug.pixelFormat = format
    Unit
    Unit
    Unit
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugVideoSettingsView(
    model: Model = LocalModel.current,
    debug: SettingsDebug,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val allowVideoRangePixelFormat by debug.allowVideoRangePixelFormat.collectAsState()
    val nativeLowLightBoost by debug.nativeLowLightBoost.collectAsState()
    val videoBitrateChange by debug.videoBitrateChange.collectAsState()
    val pixelFormat = model.database.debug.pixelFormat

    LaunchedEffect(allowVideoRangePixelFormat) {
        model.setAllowVideoRangePixelFormat()
    }

    LaunchedEffect(nativeLowLightBoost) {
        model.setNativeLowLightBoost()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Video")) })
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("pixelFormat") },
            ) {
                TextItemLocalizedView(name = "Pixel format", value = pixelFormat)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localized("Allow video range pixel format"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = allowVideoRangePixelFormat,
                    onCheckedChange = { debug.allowVideoRangePixelFormat.value = it },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localized("Native low light boost"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = nativeLowLightBoost,
                    onCheckedChange = { debug.nativeLowLightBoost.value = it },
                )
            }
            Text(
                text = localized("Change camera and restart stream for these to work properly."),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = localized("Periodic video bitrate change"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = videoBitrateChange,
                    onCheckedChange = { debug.videoBitrateChange.value = it },
                )
            }
        }
    }
}
