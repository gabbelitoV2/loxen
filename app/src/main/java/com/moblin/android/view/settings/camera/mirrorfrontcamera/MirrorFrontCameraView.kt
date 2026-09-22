package com.moblin.android.view.settings.camera.mirrorfrontcamera

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.LocalModel

@Composable
fun MirrorFrontCameraOnStreamView(model: Model = LocalModel.current, database: Database) {
    val mirrorFrontCameraOnStream by database.mirrorFrontCameraOnStream.collectAsState()
    var previousMirrorFrontCameraOnStream by remember {
        mutableStateOf(mirrorFrontCameraOnStream)
    }
    LaunchedEffect(mirrorFrontCameraOnStream) {
        if (mirrorFrontCameraOnStream != previousMirrorFrontCameraOnStream) {
            previousMirrorFrontCameraOnStream = mirrorFrontCameraOnStream
            model.reattachCamera()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Mirror front camera on stream",
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = mirrorFrontCameraOnStream,
            onCheckedChange = { database.mirrorFrontCameraOnStream.value = it },
        )
    }
}
