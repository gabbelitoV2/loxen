package com.moblin.android.view.controlbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.view.utils.CloseToolbar

@Composable
private fun FlameStateView(
    color: Color,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .background(color = Color.Black, shape = RoundedCornerShape(5.dp))
                .padding(4.dp),
        )
        Text(text)
    }
}

@Composable
private fun BulletView(
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        Text("• ")
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalStateSheetView(
    presenting: Boolean,
    onPresentingChange: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized("Thermal state")) },
                navigationIcon = {
                    CloseToolbar(
                        presenting = presenting,
                        onPresentingChange = onPresentingChange,
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    FlameStateView(
                        color = Color.White,
                        text = localized("Your device is cold and should function normally."),
                    )
                    FlameStateView(
                        color = Color.Yellow,
                        text = localized("Your device is warm, but should function normally."),
                    )
                    FlameStateView(
                        color = Color.Red,
                        text = localized("Your device is hot and may overheat."),
                    )
                }
            }
            item {
                Text(
                    text = localized("Mitigating overheating"),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    BulletView(text = localized("Use single lens or low energy cameras"))
                    BulletView(text = localized("Lower FPS"))
                    BulletView(text = localized("Lower resolution"))
                    BulletView(text = localized("Lower bitrate"))
                    BulletView(text = localized("No widgets, LUTs or other image effects"))
                    BulletView(text = localized("No direct sunlight"))
                    BulletView(text = localized("More air flow"))
                    BulletView(text = localized("Keep the battery fully charged"))
                    BulletView(text = localized("No wireless charging or fast charging"))
                    BulletView(text = localized("Use a phone cooler"))
                    BulletView(text = localized("Turn off cellular"))
                    BulletView(text = localized("Turn off busy chats"))
                    BulletView(text = localized("And a lot more..."))
                }
            }
        }
    }
}
