package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsAlertsMediaGallery
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID

private val loadedImages: MutableMap<UUID, ByteArray> = mutableMapOf()

fun loadAlertImage(model: Model, imageId: UUID): ByteArray? {
    loadedImages[imageId]?.let { return it }
    var image: ByteArray? = null
    val bundledImage = model.database.alertsMediaGallery.bundledImages
        .firstOrNull { it.id == imageId }
    if (bundledImage != null) {
        TODO("no Android counterpart for Bundle.main.path lookup of the Alerts.bundle GIF resources")
    } else {
        image = model.alertMediaStorage.tryRead(imageId)
    }
    image?.let { loadedImages[imageId] = it }
    return image
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomImageView(
    model: Model,
    media: SettingsAlertsMediaGalleryItem,
    image: ByteArray?,
) {
    var showPicker by remember { mutableStateOf(false) }
    var imageState by remember { mutableStateOf(image) }

    fun onUrl(url: String) {
        model.alertMediaStorage.add(media.id, url)
        loadedImages.remove(media.id)
        imageState = loadAlertImage(model, media.id)
        model.updateAlertsSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Image") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                TextEditNavigationView(
                    title = localized("Name"),
                    value = media.name,
                    onSubmit = { media.name = it },
                )
            }
            item {
                Button(
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
                            Text("Select image")
                        }
                    }
                }
            }
            item {
                Text("Only GIF:s are supported.")
            }
        }
        if (showPicker) {
            ModalBottomSheet(onDismissRequest = { showPicker = false }) {
                AlertPickerView(type = AlertPickerViewType.Gif)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageGalleryItemView(
    model: Model,
    image: SettingsAlertsMediaGalleryItem,
    onNavigate: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Text(
            text = image.name,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onNavigate("CustomImageView") },
                    onLongClick = { showMenu = true },
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(localized("Delete")) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGalleryView(
    model: Model,
    gallery: SettingsAlertsMediaGallery,
    alert: SettingsWidgetAlertsAlert,
    imageId: UUID,
    onImageIdChange: (UUID) -> Unit,
    onNavigate: (String) -> Unit,
) {
    fun deleteImage(index: Int) {
        gallery.customImages.removeAt(index)
        model.fixAlertMedias()
        onImageIdChange(alert.imageId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My images") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Column {
                    gallery.customImages.forEach { image ->
                        ImageGalleryItemView(
                            model = model,
                            image = image,
                            onNavigate = onNavigate,
                            onDelete = {
                                val index = makeOffsets(gallery.customImages, image.id)
                                if (index != null) {
                                    deleteImage(index)
                                }
                            },
                        )
                    }
                }
            }
            item {
                TextButtonView("Add") {
                    gallery.customImages.add(SettingsAlertsMediaGalleryItem(name = "My image"))
                }
            }
            item {
                SwipeLeftToDeleteHelpView(localized("an image"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertImageSelectorView(
    model: Model,
    gallery: SettingsAlertsMediaGallery,
    alert: SettingsWidgetAlertsAlert,
    imageId: UUID,
    onImageIdChange: (UUID) -> Unit,
    loopCount: Float,
    onNavigate: (String) -> Unit,
) {
    var loopCountState by remember(loopCount) { mutableStateOf(loopCount) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Image") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Column {
                    (gallery.bundledImages + gallery.customImages).forEach { image ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = imageId == image.id,
                                    onClick = {
                                        onImageIdChange(image.id)
                                        alert.imageId = image.id
                                        model.updateAlertsSettings()
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = imageId == image.id,
                                onClick = null,
                            )
                            Text(image.name)
                            Spacer(Modifier.weight(1f))
                            val data = loadAlertImage(model, image.id)
                            if (data != null) {
                                val bitmap = remember(data) {
                                    BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(90.dp, 50.dp),
                                    )
                                }
                            } else {
                                Icon(Icons.Default.AccountBox, contentDescription = null)
                            }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Repeat")
                    Slider(
                        value = loopCountState,
                        onValueChange = { loopCountState = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChangeFinished = {
                            alert.imageLoopCount = loopCountState.toInt()
                            model.updateAlertsSettings()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = loopCountState.toInt().toString(),
                        modifier = Modifier.width(25.dp),
                    )
                }
            }
            item {
                Text("Number of times the GIF will be played each alert.")
            }
            item {
                Text(
                    text = "My images",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("ImageGalleryView") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}
