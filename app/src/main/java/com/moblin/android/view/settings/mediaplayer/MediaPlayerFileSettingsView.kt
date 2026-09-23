package com.moblin.android.view.settings.mediaplayer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayerFile
import com.moblin.android.various.utils.createThumbnail
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.localized
import com.moblin.android.various.model.updateMediaPlayerSettings

private fun submitName(
    model: Model,
    player: SettingsMediaPlayer,
    file: SettingsMediaPlayerFile,
    value: String,
) {
    file.name = value.trim()
    model.updateMediaPlayerSettings(player.id, player)
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

    NavigationLink(
        destination = {
            Form(title = "File") {
                Section {
                    TextEditNavigationView(
                        title = localized("Name"),
                        value = file.name,
                        onSubmit = { value -> submitName(model, player, file, value) },
                        capitalize = true,
                    )
                }
            }
        },
    ) {
        DraggableItemPrefixView()
        val currentImage = image
        if (currentImage != null) {
            Image(
                bitmap = currentImage.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(90.dp),
            )
        } else {
            SystemImage("photo", fontSize = 22.sp)
        }
        Text(file.name)
    }
}
