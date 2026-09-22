package com.moblin.android.view.settings.mediaplayer

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayerFile
import com.moblin.android.various.utils.createThumbnail
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun submitName(
    model: Model,
    player: SettingsMediaPlayer,
    file: SettingsMediaPlayerFile,
    value: String,
) {
    file.name = value.trim()
    TODO("model.updateMediaPlayerSettings")
}

@Composable
fun MediaPlayerFileSettingsView(
    model: Model = LocalModel.current,
    player: SettingsMediaPlayer,
    file: SettingsMediaPlayerFile,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var image by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        createThumbnail(model.mediaStorage.makePath(file.id).absolutePath) { thumbnail ->
            image = thumbnail
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onNavigate("File") },
    ) {
        DraggableItemPrefixView()
        val currentImage = image
        if (currentImage != null) {
            androidx.compose.foundation.Image(
                bitmap = currentImage.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.width(90.dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Icon(Icons.Default.Image, contentDescription = null)
        }
        Text(file.name)
    }
}
