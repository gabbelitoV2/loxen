package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.moblin.android.AppDelegate
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsAlertsMediaGallery
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

private val loadedImages: MutableMap<UUID, ByteArray> = mutableMapOf()

fun loadAlertImage(model: Model, imageId: UUID): ByteArray? {
    loadedImages[imageId]?.let { return it }
    val bundledImage = model.database.alertsMediaGallery.bundledImages
        .firstOrNull { it.id == imageId }
    val image: ByteArray? = if (bundledImage != null) {
        runCatching {
            AppDelegate.context.assets
                .open("Alerts.bundle/${bundledImage.name}.gif")
                .use { it.readBytes() }
        }.getOrNull()
    } else {
        model.alertMediaStorage.tryRead(imageId)
    }
    image?.let { loadedImages[imageId] = it }
    return image
}

@Composable
fun CustomImageView(
    model: Model = LocalModel.current,
    media: SettingsAlertsMediaGalleryItem,
    image: ByteArray?,
) {
    var showPicker by remember { mutableStateOf(false) }
    var imageState by remember { mutableStateOf(image) }

    fun onUrl(url: String) {
        model.alertMediaStorage.add(media.id, File(url))
        loadedImages.remove(media.id)
        imageState = loadAlertImage(model, media.id)
        model.updateAlertsSettings()
    }

    Form(title = localized("Image")) {
        Section {
            TextEditNavigationView(
                title = localized("Name"),
                value = media.name,
                onSubmit = { media.name = it },
            )
        }
        Section(footer = localized("Only GIF:s are supported.")) {
            FormRow(
                onClick = {
                    showPicker = true
                    model.onDocumentPickerUrl = { url -> onUrl(url) }
                },
            ) {
                HCenter {
                    val data = imageState
                    if (data != null) {
                        val bitmap = remember(data) {
                            BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.size(320.dp, 180.dp),
                            )
                        }
                    } else {
                        Text(localized("Select image"))
                    }
                }
            }
        }
        Sheet(isPresented = showPicker, onDismissRequest = { showPicker = false }) {
            AlertPickerView(model = model, type = "gif")
        }
    }
}

@Composable
fun ImageGalleryItemView(
    model: Model = LocalModel.current,
    image: SettingsAlertsMediaGalleryItem,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            CustomImageView(
                model = model,
                media = image,
                image = loadAlertImage(model, image.id),
            )
        },
    ) {
        Text(image.name)
    }
}

@Composable
fun ImageGalleryView(
    model: Model = LocalModel.current,
    gallery: SettingsAlertsMediaGallery,
    alert: SettingsWidgetAlertsAlert,
    imageId: UUID,
    onImageIdChange: (UUID) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun deleteImage(offsets: IndexSet) {
        gallery.customImages = gallery.customImages.removing(atOffsets = offsets)
        model.fixAlertMedias()
        onImageIdChange(alert.imageId)
    }

    Form(title = localized("My images")) {
        Section(footerContent = { SwipeLeftToDeleteHelpView(localized("an image")) }) {
            ForEach(
                gallery.customImages,
                id = { it.id },
                onDelete = { deleteImage(it) },
            ) { image ->
                ContextMenuDeleteButton(
                    action = {
                        val index = gallery.customImages.indexOfFirst { it.id == image.id }
                        if (index >= 0) {
                            deleteImage(setOf(index))
                        }
                    },
                ) {
                    ImageGalleryItemView(
                        model = model,
                        image = image,
                        onNavigate = onNavigate,
                    )
                }
            }
            TextButtonView("Add") {
                gallery.customImages = gallery.customImages + SettingsAlertsMediaGalleryItem(name = "My image")
            }
        }
    }
}

@Composable
fun AlertImageSelectorView(
    model: Model = LocalModel.current,
    gallery: SettingsAlertsMediaGallery,
    alert: SettingsWidgetAlertsAlert,
    imageId: UUID,
    onImageIdChange: (UUID) -> Unit,
    loopCount: Float,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var loopCountState by remember(loopCount) { mutableStateOf(loopCount) }

    Form(title = localized("Image")) {
        Section {
            val images = gallery.bundledImages + gallery.customImages
            val selected = images.firstOrNull { it.id == imageId }
            if (selected != null) {
                Picker(
                    title = "",
                    selection = selected,
                    options = images,
                    text = { it.name },
                    onChange = { item ->
                        onImageIdChange(item.id)
                        alert.imageId = item.id
                        model.updateAlertsSettings()
                    },
                )
            }
        }
        Section(footer = localized("Number of times the GIF will be played each alert.")) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(localized("Repeat"))
                FormSlider(
                    value = loopCountState,
                    onValueChange = { loopCountState = it.roundToInt().toFloat() },
                    modifier = Modifier.weight(1f),
                    valueRange = 1f..10f,
                    onValueChangeFinished = {
                        alert.imageLoopCount = loopCountState.toInt()
                        model.updateAlertsSettings()
                    },
                )
                Box(
                    modifier = Modifier.width(25.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(loopCountState.toInt().toString())
                }
            }
        }
        Section {
            NavigationLink(
                destination = {
                    ImageGalleryView(
                        model = model,
                        gallery = gallery,
                        alert = alert,
                        imageId = imageId,
                        onImageIdChange = onImageIdChange,
                        onNavigate = onNavigate,
                    )
                },
            ) {
                Text(localized("My images"))
            }
        }
    }
}
