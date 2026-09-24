package com.moblin.android.view.settings.scenes.widgets.widget.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.various.model.getImageEffect

@Composable
fun WidgetImagePickerView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    image: Bitmap?,
    onImageChange: (Bitmap?) -> Unit,
    sizeScale: Double,
) {
    val context = LocalContext.current
    var presentingPicker by remember { mutableStateOf(false) }
    var selectedImageItem by remember { mutableStateOf<Uri?>(null) }

    fun loadImage() {
        val data = model.imageStorage.tryRead(widget.id)
        onImageChange(data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) })
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        selectedImageItem = uri
    }

    LaunchedEffect(presentingPicker) {
        if (presentingPicker) {
            imagePicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
            presentingPicker = false
        }
    }

    LaunchedEffect(selectedImageItem) {
        val uri = selectedImageItem ?: return@LaunchedEffect
        val data = com.moblin.android.platform.DocumentPicker.loadData(uri)
        if (data != null) {
            model.imageStorage.write(widget.id, data)
            loadImage()
            model.getImageEffect(id = widget.id)?.loadImage(imageStorage = model.imageStorage, widgetId = widget.id)
        }
    }

    LaunchedEffect(Unit) {
        model.checkPhotoLibraryAuthorization()
        if (image == null) {
            loadImage()
        }
    }

    Section {
        if (image != null) {
            FormRow(onClick = { presentingPicker = true }) {
                HCenter {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(
                            (1920 / sizeScale).dp,
                            (1080 / sizeScale).dp,
                        ),
                    )
                }
            }
        } else {
            FormButton(
                title = "Select image",
                centered = true,
            ) {
                presentingPicker = true
            }
        }
    }
}

@Composable
fun WidgetImageSettingsView(model: Model = LocalModel.current, widget: SettingsWidget) {
    var image by remember { mutableStateOf<Bitmap?>(null) }

    WidgetImagePickerView(
        model = model,
        widget = widget,
        image = image,
        onImageChange = { image = it },
        sizeScale = 6.0,
    )
    WidgetEffectsView(model = model, widget = widget)
}
