package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
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
import com.moblin.android.view.utils.OpenAiSettingsView
import com.moblin.android.view.utils.TextItemLocalizedView
import com.moblin.android.view.utils.VoicesView
import java.io.File
import java.util.UUID

val alertTestNames = listOf("Mark", "Natasha", "Pedro", "Anna")

@Composable
fun AlertPickerView(model: Model = LocalModel.current, type: String) {
    val launcher = com.moblin.android.platform.DocumentPicker.rememberLauncher(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            com.moblin.android.platform.DocumentPicker.copy(uri) { url -> model.onDocumentPickerUrl?.invoke(url) }
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(com.moblin.android.platform.DocumentPicker.contentTypes(type))
    }
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

    Section(header = "Text to speech") {
        Toggle(
            title = "Enabled",
            isOn = alert.textToSpeechEnabled,
            onChange = { value ->
                alert.textToSpeechEnabled = value
                model.updateAlertsSettings()
            },
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Delay")
            FormSlider(
                value = ttsDelay.toFloat(),
                onValueChange = { value ->
                    ttsDelay = value.toDouble()
                    alert.textToSpeechDelay = ttsDelay
                    model.updateAlertsSettings()
                },
                modifier = Modifier.weight(1f),
                valueRange = 0f..5f,
            )
            Box(modifier = Modifier.width(35.dp), contentAlignment = Alignment.Center) {
                Text(formatOneDecimal(ttsDelay.toFloat()))
            }
        }
        NavigationLink(
            destination = {
                VoicesView(
                    textToSpeechLanguageVoices = alert.textToSpeechLanguageVoices,
                    onVoiceChange = onVoiceChange,
                    onLanguageReset = onLanguageReset,
                    rate = rate,
                    volume = volume,
                    ttsMonsterApiToken = "",
                )
            },
        ) {
            Text("Voices")
        }
    }
}

private fun getImageName(model: Model, id: UUID?): String {
    return model.getAllAlertImages().firstOrNull { it.id == id }?.name ?: ""
}

private fun getSoundName(model: Model, id: UUID?): String {
    return model.getAllAlertSounds().firstOrNull { it.id == id }?.name ?: ""
}

@Composable
private fun VideoPickerView(model: Model = LocalModel.current) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            com.moblin.android.platform.DocumentPicker.copy(uri) { url -> model.onDocumentPickerUrl?.invoke(url) }
        }
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("video/*"))
    }
}

@Composable
private fun VideoView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        showPicker = false
        if (uri != null) {
            com.moblin.android.platform.DocumentPicker.copy(uri) { url -> model.onDocumentPickerUrl?.invoke(url) }
        }
    }

    NavigationLink(
        destination = {
            Form(title = "Video") {
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> com.moblin.android.platform.DocumentPicker.copy(uri) { url -> model.onDocumentPickerUrl?.invoke(url) } }
                Section {
                    FormButton(
                        title = if (alert.videoName.isEmpty()) "Select video" else alert.videoName,
                        centered = true,
                    ) {
                        showPicker = true
                        model.onDocumentPickerUrl = onUrl
                        launcher.launch(arrayOf("video/*"))
                    }
                }
            }
        },
    ) {
        Text("Video")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = alert.videoName)
    }
}

@Composable
fun AlertMediaView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section(header = "Media") {
        Picker(
            title = "Type",
            selection = alert.mediaType,
            options = SettingsWidgetAlertsAlertMediaType.entries,
            onChange = { value ->
                alert.mediaType = value
                model.updateAlertsSettings()
            },
        )
    }
    Section {
        when (alert.mediaType) {
            SettingsWidgetAlertsAlertMediaType.gifAndSound -> {
                NavigationLink(
                    destination = {
                        AlertImageSelectorView(
                            model = model,
                            gallery = model.database.alertsMediaGallery,
                            alert = alert,
                            imageId = alert.imageId,
                            onImageIdChange = { alert.imageId = it },
                            loopCount = alert.imageLoopCount.toFloat(),
                        )
                    },
                ) {
                    TextItemLocalizedView(name = "Image", value = getImageName(model = model, id = alert.imageId))
                }
                NavigationLink(
                    destination = {
                        AlertSoundSelectorView(
                            gallery = model.database.alertsMediaGallery,
                            alert = alert,
                            soundId = alert.soundId,
                            onSoundIdChange = { alert.soundId = it },
                        )
                    },
                ) {
                    TextItemLocalizedView(name = "Sound", value = getSoundName(model = model, id = alert.soundId))
                }
            }
            SettingsWidgetAlertsAlertMediaType.video -> {
                VideoView(model = model, alert = alert)
            }
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val alertFace = remember { Bundle.image("AlertFace")?.asImageBitmap() }
        if (alertFace != null) {
            Image(
                bitmap = alertFace,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        val image = loadAlertImage(model = model, imageId = alert.imageId)
        if (image != null) {
            val bitmap = remember(image) { BitmapFactory.decodeByteArray(image, 0, image.size) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .offset(x = imageOffset.width.dp, y = imageOffset.height.dp)
                        .size(imageWidth.dp, imageHeight.dp),
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

@Composable
fun AlertPositionView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    Section(header = "Position") {
        Picker(
            title = "Type",
            selection = alert.positionType,
            options = SettingsWidgetAlertPositionType.entries,
            onChange = { value ->
                alert.positionType = value
                model.updateAlertsSettings()
            },
        )
    }
    Section {
        when (alert.positionType) {
            SettingsWidgetAlertPositionType.face -> AlertPositionFaceView(model = model, alert = alert)
            else -> {}
        }
    }
}

@Composable
private fun AiResponseView(
    model: Model = LocalModel.current,
    alerts: SettingsWidgetAlerts,
    ai: SettingsOpenAi,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = "AI response") {
                OpenAiSettingsView(ai = ai)
            }
            var previousBaseUrl by remember { mutableStateOf(ai.baseUrl) }
            var previousApiKey by remember { mutableStateOf(ai.apiKey) }
            var previousModelName by remember { mutableStateOf(ai.model) }
            var previousPersonality by remember { mutableStateOf(ai.personality) }
            LaunchedEffect(ai.baseUrl) {
                if (previousBaseUrl != ai.baseUrl) {
                    previousBaseUrl = ai.baseUrl
                    model.updateAlertsSettings()
                }
            }
            LaunchedEffect(ai.apiKey) {
                if (previousApiKey != ai.apiKey) {
                    previousApiKey = ai.apiKey
                    model.updateAlertsSettings()
                }
            }
            LaunchedEffect(ai.model) {
                if (previousModelName != ai.model) {
                    previousModelName = ai.model
                    model.updateAlertsSettings()
                }
            }
            LaunchedEffect(ai.personality) {
                if (previousPersonality != ai.personality) {
                    previousPersonality = ai.personality
                    model.updateAlertsSettings()
                }
            }
        },
    ) {
        Toggle(
            title = "AI response",
            isOn = alerts.aiEnabled,
            enabled = ai.isConfigured(),
            onChange = { value ->
                alerts.aiEnabled = value
                model.updateAlertsSettings()
            },
        )
    }
}

@Composable
fun WidgetAlertsSettingsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Section {
        NavigationLink(destination = { WidgetAlertsTwitchSettingsView(twitch = widget.alerts.twitch) }) {
            TwitchLogoAndNameView()
        }
        NavigationLink(destination = { WidgetAlertsKickSettingsView(kick = widget.alerts.kick) }) {
            KickLogoAndNameView()
        }
        NavigationLink(destination = { WidgetAlertsChatBotSettingsView(model = model, chatBot = widget.alerts.chatBot) }) {
            Text("Chat bot")
        }
        NavigationLink(
            destination = { WidgetAlertsSpeechToTextSettingsView(model = model, speechToText = widget.alerts.speechToText) },
        ) {
            Text("Speech to text")
        }
    }
    AiResponseView(
        model = model,
        alerts = widget.alerts,
        ai = widget.alerts.ai,
        onNavigate = onNavigate,
    )
}
