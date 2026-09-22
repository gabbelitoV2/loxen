package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsOpenAi
import com.moblin.android.various.settings.SettingsVoice
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetAlertPositionType
import com.moblin.android.various.settings.SettingsWidgetAlerts
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsAlertMediaType
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.AnchorPoint
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.calculatePositioningAnchorPoint
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.calculatePositioningRectangle
import com.moblin.android.view.settings.scenes.widgets.widget.videosource.drawPositioningRectangle
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.settings.streams.stream.KickLogoAndNameView
import com.moblin.android.view.settings.streams.stream.TwitchLogoAndNameView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextItemLocalizedView
import java.io.File
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

val alertTestNames = listOf("Mark", "Natasha", "Pedro", "Anna")

@Composable
fun AlertPickerView(model: Model = LocalModel.current, type: String) {
    Unit
}

@Composable
fun AlertTextToSpeechView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var ttsDelay by remember(alert) { mutableStateOf(alert.textToSpeechDelay) }
    var rate by remember { mutableFloatStateOf(0.4f) }
    var volume by remember { mutableFloatStateOf(0.6f) }

    val onVoiceChange: (String, SettingsVoice) -> Unit = { languageCode, voice ->
        alert.textToSpeechLanguageVoices = alert.textToSpeechLanguageVoices + (languageCode to voice)
        model.updateAlertsSettings()
    }
    val onLanguageReset: (String) -> Unit = { languageCode ->
        alert.textToSpeechLanguageVoices = alert.textToSpeechLanguageVoices - languageCode
        model.updateAlertsSettings()
    }

    Text("Text to speech", style = MaterialTheme.typography.titleSmall)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Enabled", modifier = Modifier.weight(1f))
        Switch(
            checked = alert.textToSpeechEnabled,
            onCheckedChange = { value ->
                alert.textToSpeechEnabled = value
                model.updateAlertsSettings()
            },
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Delay")
        Slider(
            value = ttsDelay.toFloat(),
            onValueChange = { ttsDelay = it.toDouble() },
            valueRange = 0f..5f,
            steps = 9,
            modifier = Modifier.weight(1f),
        )
        Text(formatOneDecimal(ttsDelay.toFloat()), modifier = Modifier.width(35.dp))
    }
    LaunchedEffect(ttsDelay) {
        alert.textToSpeechDelay = ttsDelay
        model.updateAlertsSettings()
    }
    Text("Voices", modifier = Modifier.clickable { onNavigate("voices") })
}

private fun getImageName(model: Model, id: UUID?): String {
    return model.getAllAlertImages().firstOrNull { it.id == id }?.name ?: ""
}

private fun getSoundName(model: Model, id: UUID?): String {
    return model.getAllAlertSounds().firstOrNull { it.id == id }?.name ?: ""
}

@Composable
private fun VideoPickerView(model: Model = LocalModel.current) {
    Unit
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    var showForm by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    val onUrl: (String) -> Unit = { url ->
        alert.videoName = url.substringAfterLast('/')
        val filename = alert.makeVideoFilename()
        if (filename != null) {
            model.alertMediaStorage.videos.remove(filename)
            model.alertMediaStorage.videos.add(filename, File(url))
            model.updateAlertsSettings()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showForm = true },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Video")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = alert.videoName)
    }
    if (showForm) {
        ModalBottomSheet(onDismissRequest = { showForm = false }) {
            Text("Video", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    showPicker = true
                    model.onDocumentPickerUrl = onUrl
                },
            ) {
                HCenter {
                    if (alert.videoName.isEmpty()) {
                        Text("Select video")
                    } else {
                        Text(alert.videoName)
                    }
                }
            }
        }
    }
    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            VideoPickerView(model = model)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertMediaView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Text("Media", style = MaterialTheme.typography.titleSmall)
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = alert.mediaType.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SettingsWidgetAlertsAlertMediaType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.toString()) },
                    onClick = {
                        alert.mediaType = type
                        expanded = false
                        model.updateAlertsSettings()
                    },
                )
            }
        }
    }
    when (alert.mediaType) {
        SettingsWidgetAlertsAlertMediaType.gifAndSound -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("alertImageSelector") },
            ) {
                TextItemLocalizedView(name = "Image", value = getImageName(model = model, id = alert.imageId))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("alertSoundSelector") },
            ) {
                TextItemLocalizedView(name = "Sound", value = getSoundName(model = model, id = alert.soundId))
            }
        }
        SettingsWidgetAlertsAlertMediaType.video -> {
            VideoView(model = model, alert = alert)
        }
    }
}

@Composable
private fun AlertPositionFaceView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    var facePosition by remember { mutableStateOf(Offset(100f, 100f)) }
    var facePositionOffset by remember { mutableStateOf(Size(0f, 0f)) }
    var facePositionAnchorPoint by remember { mutableStateOf<AnchorPoint?>(null) }
    var imageWidth by remember { mutableFloatStateOf(100f) }
    var imageHeight by remember { mutableFloatStateOf(100f) }
    var imageOffset by remember { mutableStateOf(Size(0f, 0f)) }

    fun updateFacePositionAnchorPoint(location: Offset, size: Size) {
        if (facePositionAnchorPoint == null) {
            val (anchorPoint, offset) = calculatePositioningAnchorPoint(
                location,
                size,
                alert.facePosition.x,
                alert.facePosition.y,
                alert.facePosition.width,
                alert.facePosition.height,
            )
            facePositionAnchorPoint = anchorPoint
            facePositionOffset = offset
        }
    }

    fun createFacePositionRectangleAndUpdateImage(size: Size): Rect {
        val (xTopLeft, yTopLeft, xBottomRight, yBottomRight) = calculatePositioningRectangle(
            facePositionAnchorPoint,
            alert.facePosition.x,
            alert.facePosition.y,
            alert.facePosition.width,
            alert.facePosition.height,
            facePosition,
            size,
            facePositionOffset,
        )
        alert.facePosition.x = xTopLeft
        alert.facePosition.y = yTopLeft
        alert.facePosition.width = xBottomRight - xTopLeft
        alert.facePosition.height = yBottomRight - yTopLeft
        val xPoints = alert.facePosition.x * size.width
        val yPoints = alert.facePosition.y * size.height
        val widthPoints = alert.facePosition.width * size.width
        val heightPoints = alert.facePosition.height * size.height
        imageWidth = widthPoints.toFloat()
        imageHeight = heightPoints.toFloat()
        imageOffset = Size(
            width = (xPoints + widthPoints / 2 - size.width / 2).toFloat(),
            height = (yPoints + heightPoints / 2 - size.height / 2).toFloat(),
        )
        return Rect(xPoints.toFloat(), yPoints.toFloat(), (xPoints + widthPoints).toFloat(), (yPoints + heightPoints).toFloat())
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Unit
        val image = loadAlertImage(model = model, imageId = alert.imageId)
        if (image != null) {
            val bitmap = remember(image) { BitmapFactory.decodeByteArray(image, 0, image.size) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(imageWidth.dp)
                        .height(imageHeight.dp)
                        .offset(x = imageOffset.width.dp, y = imageOffset.height.dp),
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { location ->
                            facePosition = location
                            updateFacePositionAnchorPoint(
                                location,
                                Size(size.width.toFloat(), size.height.toFloat()),
                            )
                        },
                        onDragEnd = { facePositionAnchorPoint = null },
                        onDrag = { change, _ ->
                            facePosition = change.position
                            updateFacePositionAnchorPoint(
                                change.position,
                                Size(size.width.toFloat(), size.height.toFloat()),
                            )
                            change.consume()
                        },
                    )
                },
        ) {
            drawPositioningRectangle(createFacePositionRectangleAndUpdateImage(size))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertPositionView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    Text("Position", style = MaterialTheme.typography.titleSmall)
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = alert.positionType.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SettingsWidgetAlertPositionType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.toString()) },
                    onClick = {
                        alert.positionType = type
                        expanded = false
                        model.updateAlertsSettings()
                    },
                )
            }
        }
    }
    when (alert.positionType) {
        SettingsWidgetAlertPositionType.face -> AlertPositionFaceView(model = model, alert = alert)
        else -> {}
    }
}

@Composable
private fun AiResponseView(
    model: Model = LocalModel.current,
    alerts: SettingsWidgetAlerts,
    ai: SettingsOpenAi,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("openAiSettings") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("AI response", modifier = Modifier.weight(1f))
        Switch(
            checked = alerts.aiEnabled,
            onCheckedChange = { value ->
                alerts.aiEnabled = value
                model.updateAlertsSettings()
            },
            enabled = ai.isConfigured(),
        )
    }
    LaunchedEffect(ai.baseUrl) {
        model.updateAlertsSettings()
    }
    LaunchedEffect(ai.apiKey) {
        model.updateAlertsSettings()
    }
    LaunchedEffect(ai.model) {
        model.updateAlertsSettings()
    }
    LaunchedEffect(ai.personality) {
        model.updateAlertsSettings()
    }
    LaunchedEffect(alerts.aiEnabled) {
        model.updateAlertsSettings()
    }
}

@Composable
fun WidgetAlertsSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("twitchAlerts") },
    ) {
        TwitchLogoAndNameView()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("kickAlerts") },
    ) {
        KickLogoAndNameView()
    }
    Text(
        "Chat bot",
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("chatBotAlerts") },
    )
    Text(
        "Speech to text",
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("speechToTextAlerts") },
    )
    AiResponseView(
        model = model,
        alerts = widget.alerts,
        ai = widget.alerts.ai,
        onNavigate = onNavigate,
    )
}
