package com.moblin.android.view.settings.display

import android.graphics.Bitmap
import android.net.Uri
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.failedToConnectMessage
import com.moblin.android.various.model.flameRedMessage
import com.moblin.android.various.model.formatWarning
import com.moblin.android.various.model.fffffMessage
import com.moblin.android.various.model.lowBatteryMessage
import com.moblin.android.various.model.lowBitrateMessage
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsExternalDisplayContent
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.various.utils.isMac
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.AnchorPoint
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.calculatePositioningAnchorPoint
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.calculatePositioningRectangle
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.drawPositioningRectangle
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun BackgroundImageCropView(
    model: Model = LocalModel.current,
    quickButtons: SettingsQuickButtons,
    image: Bitmap
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
                quickButtons.backgroundImageCropHeight
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
            positionOffset
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
                .toFloat() * size.height
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(image.width.toFloat() / image.height.toFloat())
        )
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, _ ->
                            position = change.position
                            updatePositionAnchorPoint(position, Size(size.width.toFloat(), size.height.toFloat()))
                            val now = SystemClock.elapsedRealtime()
                            if (now - latestImageUpdateTime > 100) {
                                latestImageUpdateTime = now
                                Unit
                            }
                        },
                        onDragEnd = {
                            positionAnchorPoint = null
                            Unit
                        }
                    )
                }
        ) {
            drawPositioningRectangle(createPositionRectangle(size))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundImageSettingsView(
    model: Model = LocalModel.current,
    quickButtons: SettingsQuickButtons
) {
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var presentingPicker by remember { mutableStateOf(false) }
    var selectedImageItem by remember { mutableStateOf<Uri?>(null) }
    val backgroundImageOpacity by quickButtons.backgroundImageOpacity.collectAsState()

    LaunchedEffect(Unit) {
        model.checkPhotoLibraryAuthorization()
        image = TODO("Model.readControlBarBackgroundImage is not implemented")
    }
    LaunchedEffect(presentingPicker) {
        if (presentingPicker) {
            Unit
        }
    }
    LaunchedEffect(selectedImageItem) {
        if (selectedImageItem == null) {
            return@LaunchedEffect
        }
        selectedImageItem = null
        Unit
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Background") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Column {
                    image?.let {
                        BackgroundImageCropView(model = model, quickButtons = quickButtons, image = it)
                    }
                    TextButtonView("Select image") {
                        presentingPicker = true
                    }
                    if (image != null) {
                        CompositionLocalProvider(LocalContentColor provides Color.Red) {
                            TextButtonView("Delete image") {
                                image = null
                                Unit
                            }
                        }
                    }
                }
            }
            if (image != null) {
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Opacity")
                            Slider(
                                value = backgroundImageOpacity.toFloat(),
                                onValueChange = {
                                    quickButtons.backgroundImageOpacity.value = it.toDouble()
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        LaunchedEffect(backgroundImageOpacity) {
                            Unit
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalDisplayContentView(
    model: Model = LocalModel.current,
    database: Database
) {
    var expanded by remember { mutableStateOf(false) }
    val externalDisplayContent = database.externalDisplayContent

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("External monitor content")
        Spacer(Modifier.weight(1f))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = externalDisplayContent.toString(),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                SettingsExternalDisplayContent.entries.forEach { content ->
                    DropdownMenuItem(
                        text = { Text(content.toString()) },
                        onClick = {
                            database.externalDisplayContent = content
                            expanded = false
                        }
                    )
                }
            }
        }
    }
    LaunchedEffect(externalDisplayContent) {
        model.setExternalDisplayContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val bigButtons = database.bigButtons
    val bigAudioLevelMeter = database.bigAudioLevelMeter
    val verticalButtons = database.verticalButtons
    val showAllSettings = database.showAllSettings
    val lowBitrateWarning = database.lowBitrateWarning
    val startStopRecordingConfirmations = database.startStopRecordingConfirmations
    val vibrate = database.vibrate
    val portrait = database.portrait
    val portraitVideoOffsetFromTop by model.portraitVideoOffsetFromTop.collectAsState()
    val stream by model.stream.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Display") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Column {
                    Text("Control bar", style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { onNavigate("Quick buttons") }) {
                        Text("Quick buttons")
                    }
                    TextButton(onClick = { onNavigate("Stream button") }) {
                        Text("Stream button")
                    }
                    TextButton(onClick = { onNavigate("Background") }) {
                        Text("Background")
                    }
                }
            }
            item {
                Column {
                    Text("General", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Big buttons")
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = bigButtons,
                            onCheckedChange = { database.bigButtons = it }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Big audio level meter")
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = bigAudioLevelMeter,
                            onCheckedChange = { database.bigAudioLevelMeter = it }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vertical buttons")
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = verticalButtons,
                            onCheckedChange = { database.verticalButtons = it }
                        )
                    }
                    if (showAllSettings) {
                        TextButton(onClick = { onNavigate("Local overlays") }) {
                            Text("Local overlays")
                        }
                        ExternalDisplayContentView(model = model, database = database)
                        TextButton(onClick = { onNavigate("Network interface names") }) {
                            Text("Network interface names")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Low bitrate warning")
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = lowBitrateWarning,
                                onCheckedChange = { database.lowBitrateWarning = it }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Recording confirmations")
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = startStopRecordingConfirmations,
                                onCheckedChange = { database.startStopRecordingConfirmations = it }
                            )
                        }
                    }
                }
            }
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vibrate")
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = vibrate,
                            onCheckedChange = { database.vibrate = it }
                        )
                    }
                    LaunchedEffect(vibrate) {
                        model.setAllowHapticsAndSystemSoundsDuringRecording()
                    }
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("Enable to vibrate the device when the following toasts appear:")
                        Text("")
                        Text("• $fffffMessage")
                        Text("• ${failedToConnectMessage("Main")}")
                        Text("• ${formatWarning(lowBitrateMessage)}")
                        Text("• ${formatWarning(lowBatteryMessage)}")
                        Text("• ${formatWarning(flameRedMessage)}")
                        Text("")
                        Text("Make sure silent mode is off for vibrations to work.")
                    }
                }
            }
            if (showAllSettings) {
                if (!isMac()) {
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Portrait")
                                Spacer(Modifier.weight(1f))
                                Switch(
                                    checked = portrait,
                                    onCheckedChange = { model.setDisplayPortrait(!portrait) }
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Video position")
                                Slider(
                                    value = portraitVideoOffsetFromTop.toFloat(),
                                    onValueChange = {
                                        database.portraitVideoOffsetFromTop = it.toDouble()
                                    },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            LaunchedEffect(portraitVideoOffsetFromTop) {
                                database.portraitVideoOffsetFromTop = portraitVideoOffsetFromTop
                            }
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Useful when using an external camera and a portrait phone holder.")
                                Text("")
                                Text(
                                    "To stream in portrait, enable Settings → Streams → ${stream.name} → Portrait."
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
