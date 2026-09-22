package com.moblin.android.view.settings.watch.display

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.moblinwatch.shared.WatchSettingsShow
import com.moblin.android.LocalOnNavigate

private const val WatchLocalOverlaysSettingsRoute = "WatchLocalOverlaysSettingsView"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchDisplaySettingsView(
    show: WatchSettingsShow,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display") },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues),
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Local overlays") },
                    modifier = Modifier.clickable {
                        onNavigate(WatchLocalOverlaysSettingsRoute)
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
