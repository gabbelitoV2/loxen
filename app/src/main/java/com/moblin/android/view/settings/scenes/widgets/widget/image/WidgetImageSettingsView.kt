package com.moblin.android.view.settings.scenes.widgets.widget.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
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
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.LocalModel

@Composable
fun WidgetImagePickerView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    image: Bitmap?,
    onImageChange: (Bitmap?) -> Unit,
    sizeScale: Double,
) {
    var presentingPicker by remember { mutableStateOf(false) }
    var selectedImageItem by remember { mutableStateOf<Uri?>(null) }

    fun loadImage() {
        val data = model.imageStorage.tryRead(widget.id)
        onImageChange(data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) })
    }

    LaunchedEffect(presentingPicker) {
        if (presentingPicker) {
            Unit
        }
    }

    LaunchedEffect(selectedImageItem) {
        if (selectedImageItem != null) {
            Unit
        }
    }

    LaunchedEffect(Unit) {
        model.checkPhotoLibraryAuthorization()
        if (image == null) {
            loadImage()
        }
    }

    Column {
        Button(onClick = { presentingPicker = true }) {
            if (image != null) {
                HCenter {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size((1920 / sizeScale).dp, (1080 / sizeScale).dp)
                    )
                }
            } else {
                HCenter {
                    Text("Select image")
                }
            }
        }
    }
}

@Composable
fun WidgetImageSettingsView(model: Model = LocalModel.current, widget: SettingsWidget) {
    var image by remember { mutableStateOf<Bitmap?>(null) }

    Column {
        WidgetImagePickerView(
            model = model,
            widget = widget,
            image = image,
            onImageChange = { image = it },
            sizeScale = 6.0
        )
        WidgetEffectsView(model = model, widget = widget)
    }
}
