package com.moblin.android.view.settings.camera.cameracontrols

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.LocalModel

@Composable
fun CameraControlsView(model: Model = LocalModel.current, database: Database) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Camera controls",
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = database.cameraControlsEnabled,
            onCheckedChange = {
                database.cameraControlsEnabled = it
                Unit
            },
        )
    }
}
