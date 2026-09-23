package com.moblin.android.view.settings.display

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.deleteControlBarBackgroundImage
import com.moblin.android.various.model.failedToConnectMessage
import com.moblin.android.various.model.fffffMessage
import com.moblin.android.various.model.flameRedMessage
import com.moblin.android.various.model.formatWarning
import com.moblin.android.various.model.lowBatteryMessage
import com.moblin.android.various.model.lowBitrateMessage
import com.moblin.android.various.model.readControlBarBackgroundImage
import com.moblin.android.various.model.saveControlBarBackgroundImage
import com.moblin.android.various.model.updateControlBarBackgroundImage
import com.moblin.android.various.model.updateControlBarBackgroundImageOpacity
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsExternalDisplayContent
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.Form
import com.moblin.android.view.settings.FormSlider
import com.moblin.android.view.settings.LocalTint
import com.moblin.android.view.settings.NavigationLink
import com.moblin.android.view.settings.Picker
import com.moblin.android.view.settings.Section
import com.moblin.android.view.settings.Toggle
import com.moblin.android.view.settings.binding
import com.moblin.android.view.settings.display.localoverlays.LocalOverlaysSettingsView
import com.moblin.android.view.settings.display.networkinterfacenames.LocalOverlaysNetworkInterfaceNamesSettingsView
import com.moblin.android.view.settings.display.quickbuttons.QuickButtonsSettingsView
import com.moblin.android.view.settings.display.streambutton.StreamButtonsSettingsView
import com.moblin.android.view.settings.formPalette
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.AnchorPoint
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.calculatePositioningAnchorPoint
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.calculatePositioningRectangle
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.drawPositioningRectangle
import com.moblin.android.view.utils.TextButtonView

@Composable
private fun BackgroundImageCropView(
    model: Model = LocalModel.current,
    quickButtons: SettingsQuickButtons,
    image: Bitmap,
) {
    var position by remember { mutableStateOf(Offset(100f, 100f)) }
    var positionOffset by remember { mutableStateOf(Size(0f, 0f)) }
    var positionAnchorPoint by remember { mutableStateOf<AnchorPoint?>(null) }
    var latestImageUpdateTime by remember { mutableStateOf(SystemClock.elapsedRealtime()) }

    fun updatePositionAnchorPoint(location: Offset, size: Size) {
        if (positionAnchorPoint == null) {
            val (anchorPoint, offset) = calculatePositioningAnchorPoint(
                location,
                size,
                quickButtons.backgroundImageCropX,
                quickButtons.backgroundImageCropY,
                quickButtons.backgroundImageCropWidth,
                quickButtons.backgroundImageCropHeight,
            )
            positionAnchorPoint = anchorPoint
            positionOffset = offset
        }
    }

    fun createPositionRectangle(size: Size): Rect {
        val (xTopLeft, yTopLeft, xBottomRight, yBottomRight) = calculatePositioningRectangle(
            positionAnchorPoint,
            quickButtons.backgroundImageCropX,
            quickButtons.backgroundImageCropY,
            quickButtons.backgroundImageCropWidth,
            quickButtons.backgroundImageCropHeight,
            position,
            size,
            positionOffset,
        )
        quickButtons.backgroundImageCropX = xTopLeft
        quickButtons.backgroundImageCropY = yTopLeft
        quickButtons.backgroundImageCropWidth = xBottomRight - xTopLeft
        quickButtons.backgroundImageCropHeight = yBottomRight - yTopLeft
        return Rect(
            left = quickButtons.backgroundImageCropX.toFloat() * size.width,
            top = quickButtons.backgroundImageCropY.toFloat() * size.height,
            right = (quickButtons.backgroundImageCropX + quickButtons.backgroundImageCropWidth)
                .toFloat() * size.width,
            bottom = (quickButtons.backgroundImageCropY + quickButtons.backgroundImageCropHeight)
                .toFloat() * size.height,
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(image.width.toFloat() / image.height.toFloat()),
        )
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(image) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            position = change.position
                            updatePositionAnchorPoint(
                                position,
                                Size(size.width.toFloat(), size.height.toFloat()),
                            )
                            val now = SystemClock.elapsedRealtime()
                            if (now - latestImageUpdateTime > 100) {
                                latestImageUpdateTime = now
                                model.updateControlBarBackgroundImage(image = image)
                            }
                        },
                        onDragEnd = {
                            positionAnchorPoint = null
                            model.updateControlBarBackgroundImage(image = image)
                        },
                    )
                },
        ) {
            drawPositioningRectangle(createPositionRectangle(size))
        }
    }
}

@Composable
private fun BackgroundImageSettingsView(
    model: Model = LocalModel.current,
    quickButtons: SettingsQuickButtons,
) {
    val palette = formPalette()
    val context = LocalContext.current
    var image by remember { mutableStateOf<Bitmap?>(null) }
    val backgroundImageOpacity by quickButtons.backgroundImageOpacity.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val data = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (data != null) {
                image = model.saveControlBarBackgroundImage(data = data)
            }
        }
    }
    LaunchedEffect(Unit) {
        model.checkPhotoLibraryAuthorization()
        image = model.readControlBarBackgroundImage()
    }
    Form(title = "Background") {
        Section {
            image?.let {
                BackgroundImageCropView(model = model, quickButtons = quickButtons, image = it)
            }
            TextButtonView("Select image") {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            if (image != null) {
                CompositionLocalProvider(LocalTint provides palette.red) {
                    TextButtonView("Delete image") {
                        image = null
                        model.deleteControlBarBackgroundImage()
                    }
                }
            }
        }
        if (image != null) {
            Section {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = localized("Opacity"))
                    Spacer(modifier = Modifier.width(8.dp))
                    FormSlider(
                        value = backgroundImageOpacity.toFloat(),
                        onValueChange = {
                            quickButtons.backgroundImageOpacity.value = it.toDouble()
                            model.updateControlBarBackgroundImageOpacity()
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExternalDisplayContentView(
    model: Model = LocalModel.current,
    database: Database,
) {
    val externalDisplayContent = binding({ database.externalDisplayContent }) {
        database.externalDisplayContent = it
        model.setExternalDisplayContent()
    }
    Picker(
        title = "External monitor content",
        selection = externalDisplayContent.value,
        options = SettingsExternalDisplayContent.entries,
    ) {
        externalDisplayContent.value = it
    }
}

@Composable
fun DisplaySettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val showAllSettings = database.showAllSettings
    val bigButtons = binding({ database.bigButtons }) { database.bigButtons = it }
    val bigAudioLevelMeter = binding({ database.bigAudioLevelMeter }) { database.bigAudioLevelMeter = it }
    val verticalButtons = binding({ database.verticalButtons }) { database.verticalButtons = it }
    val lowBitrateWarning = binding({ database.lowBitrateWarning }) { database.lowBitrateWarning = it }
    val startStopRecordingConfirmations = binding({ database.startStopRecordingConfirmations }) {
        database.startStopRecordingConfirmations = it
    }
    val vibrate = binding({ database.vibrate }) {
        database.vibrate = it
        model.setAllowHapticsAndSystemSoundsDuringRecording()
    }
    val portrait = binding({ database.portrait }) {
        model.setDisplayPortrait(portrait = !database.portrait)
    }
    val portraitVideoOffsetFromTop by model.portraitVideoOffsetFromTop.collectAsState()
    val stream by model.stream.collectAsState()
    Form(title = "Display") {
        Section(header = "Control bar") {
            NavigationLink("Quick buttons") {
                QuickButtonsSettingsView(model = model, showAll = true)
            }
            NavigationLink("Stream button") {
                StreamButtonsSettingsView(database = database)
            }
            NavigationLink("Background") {
                BackgroundImageSettingsView(model = model, quickButtons = model.database.quickButtonsGeneral)
            }
        }
        Section(header = "General") {
            Toggle("Big buttons", isOn = bigButtons)
            Toggle("Big audio level meter", isOn = bigAudioLevelMeter)
            Toggle("Vertical buttons", isOn = verticalButtons)
            if (showAllSettings) {
                NavigationLink("Local overlays") {
                    LocalOverlaysSettingsView(show = database.show)
                }
                ExternalDisplayContentView(model = model, database = database)
                NavigationLink("Network interface names") {
                    LocalOverlaysNetworkInterfaceNamesSettingsView(model = model, database = database)
                }
                Toggle("Low bitrate warning", isOn = lowBitrateWarning)
                Toggle("Recording confirmations", isOn = startStopRecordingConfirmations)
            }
        }
        Section(footerContent = {
            Column {
                Text(text = localized("Enable to vibrate the device when the following toasts appear:"))
                Text(text = "")
                Text(text = "• $fffffMessage")
                Text(text = "• ${failedToConnectMessage("Main")}")
                Text(text = "• ${formatWarning(lowBitrateMessage)}")
                Text(text = "• ${formatWarning(lowBatteryMessage)}")
                Text(text = "• ${formatWarning(flameRedMessage)}")
                Text(text = "")
                Text(text = localized("Make sure silent mode is off for vibrations to work."))
            }
        }) {
            Toggle("Vibrate", isOn = vibrate)
        }
        if (showAllSettings) {
            if (!isMac()) {
                Section(footerContent = {
                    Column {
                        Text(text = localized("Useful when using an external camera and a portrait phone holder."))
                        Text(text = "")
                        Text(text = "To stream in portrait, enable Settings → Streams → ${stream.name} → Portrait.")
                    }
                }) {
                    Toggle("Portrait", isOn = portrait)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = localized("Video position"))
                        Spacer(modifier = Modifier.width(8.dp))
                        FormSlider(
                            value = portraitVideoOffsetFromTop.toFloat(),
                            onValueChange = {
                                model.portraitVideoOffsetFromTop.value = it.toDouble()
                                database.portraitVideoOffsetFromTop = it.toDouble()
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
