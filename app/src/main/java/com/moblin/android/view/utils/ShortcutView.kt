package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun ShortcutSectionView(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Shortcut",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        )
        content()
    }
}

@Composable
fun WidgetShortcutView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Text(
        text = "Widget",
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("WidgetSettings") }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    )
}

@Composable
fun ScenesShortcutView(
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("ScenesSettings") }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(imageVector = Icons.Default.List, contentDescription = null)
        Text(
            text = "Scenes",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun StreamingPlatformsShortcutView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("StreamPlatformsSettings") }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(imageVector = Icons.Default.Share, contentDescription = null)
        Text(
            text = "Streaming platforms",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun RemoteControlWebShortcutView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("RemoteControlSettingsWeb") }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(
            text = "Remote control",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun RemoteControlAssistantShortcutView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("RemoteControlStreamers") }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
        Text(
            text = "Remote control assistant",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
fun IngestsShortcutView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("IngestsSettings") }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(imageVector = Icons.Default.Build, contentDescription = null)
        Text(
            text = "Ingests",
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
