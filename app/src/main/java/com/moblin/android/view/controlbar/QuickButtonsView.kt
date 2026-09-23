package com.moblin.android.view.controlbar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.color
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.localized
import com.moblin.android.platform.Bundle
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.QuickButtons
import com.moblin.android.various.model.ReplayProvider
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.WatchProtocolWorkoutType
import com.moblin.android.various.model.createStreamMarker
import com.moblin.android.various.model.disableInteractiveChat
import com.moblin.android.various.model.instantReplay
import com.moblin.android.various.model.makeReplayIsNotEnabledToast
import com.moblin.android.various.model.sceneUpdated
import com.moblin.android.various.model.startPhotoShoot
import com.moblin.android.various.model.startRecording
import com.moblin.android.various.model.startWorkout
import com.moblin.android.various.model.stopPhotoShoot
import com.moblin.android.various.model.stopRecording
import com.moblin.android.various.model.stopWorkout
import com.moblin.android.various.model.takeSnapshot
import com.moblin.android.various.model.toggleCameraPreview
import com.moblin.android.various.model.toggleDrawOnStream
import com.moblin.android.various.model.toggleGimbalTracking
import com.moblin.android.various.model.togglePhotoShoot
import com.moblin.android.various.model.togglePreviewStream
import com.moblin.android.various.model.toggleStealthMode
import com.moblin.android.various.model.toggleTextToSpeechPaused
import com.moblin.android.various.model.updateAutoSceneSwitcherButtonState
import com.moblin.android.various.model.updateLutsButtonState
import com.moblin.android.various.settings.SettingsQuickButton
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.view.utils.stroke

val controlBarPages = 5

@Composable
private fun MinimumScaleText(
    text: String,
    fontSize: TextUnit,
    minimumScaleFactor: Float,
    maxLines: Int,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    var scale by remember(text, fontSize) { mutableFloatStateOf(1f) }
    var ready by remember(text, fontSize) { mutableStateOf(false) }
    Text(
        text = text,
        color = color,
        fontSize = fontSize * scale,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        textAlign = textAlign,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scale > minimumScaleFactor) {
                scale = maxOf(minimumScaleFactor, scale - 0.05f)
            } else {
                ready = true
            }
        },
        modifier = modifier.drawWithContent {
            if (ready) {
                drawContent()
            }
        },
    )
}

@Composable
private fun ConfirmationDialog(
    onDismiss: () -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                buttons()
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(localized("Cancel"))
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickButtonImage(
    model: Model = LocalModel.current,
    quickButtonsSettings: SettingsQuickButtons,
    button: SettingsQuickButton,
    buttonSize: Float,
    hideImage: Boolean = false,
    onTapGesture: () -> Unit,
) {
    val isOn by button.isOn.collectAsState()
    val buttonColor by button.color.collectAsState()
    val bigButtons by quickButtonsSettings.bigButtons.collectAsState()

    fun getImage(): String {
        return if (isOn) {
            button.imageOn
        } else {
            button.imageOff
        }
    }

    fun foregroundColor(): Color {
        return if (hideImage) {
            Color.Transparent
        } else {
            Color.White
        }
    }

    val backgroundColor = remember(buttonColor) { button.backgroundColor.color() }

    fun iconSize(): TextUnit {
        return if (bigButtons) {
            20.sp
        } else {
            17.sp
        }
    }

    Box(
        modifier = Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = { onTapGesture() },
            onLongClick = { model.showQuickButtonSettings(type = button.type) },
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            SystemImage(
                name = getImage(),
                fontSize = iconSize(),
                tint = foregroundColor(),
            )
        }
        if (isOn) {
            Box(
                modifier = Modifier
                    .size(buttonSize.dp)
                    .border(1.dp, Color.White, CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstantReplayView(
    model: Model = LocalModel.current,
    replay: ReplayProvider,
    button: SettingsQuickButton,
    size: Float,
) {
    val isPlaying by replay.isPlaying.collectAsState()
    val timeLeft by replay.timeLeft.collectAsState()
    val buttonColor by button.color.collectAsState()
    val backgroundColor = remember(buttonColor) { button.backgroundColor.color() }
    if (isPlaying) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (model.stream.value.replay.enabled) {
                            model.instantReplay()
                        } else {
                            model.makeReplayIsNotEnabledToast()
                        }
                    },
                    onLongClick = {
                        model.showQuickButtonSettings(type = SettingsQuickButtonType.instantReplay)
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = timeLeft.toString(),
                fontSize = 25.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        QuickButtonImage(
            model = model,
            quickButtonsSettings = model.database.quickButtonsGeneral,
            button = button,
            buttonSize = size,
        ) {
            if (model.stream.value.replay.enabled) {
                model.instantReplay()
            } else {
                model.makeReplayIsNotEnabledToast()
            }
        }
    }
}

@Composable
fun QuickButtonPlaceholderImage(size: Float) {
    Box(
        modifier = Modifier
            .padding(0.dp)
            .size(size.dp)
            .alpha(0.0f),
        contentAlignment = Alignment.Center,
    ) {
        SystemImage(
            name = "pawprint",
            fontSize = 17.sp,
            tint = Color.Black,
        )
    }
}

@Composable
private fun ButtonTextOverlayView(text: String) {
    Box(
        modifier = Modifier.size(controlBarButtonSize.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 8.sp,
            color = Color.White,
            modifier = Modifier
                .offset(x = 10.dp, y = 0.dp)
                .rotate(-90.0f),
        )
    }
}

@Composable
fun QuickButtonsInnerView(
    model: Model = LocalModel.current,
    quickButtons: QuickButtons,
    quickButtonsSettings: SettingsQuickButtons,
    orientation: Orientation,
    button: SettingsQuickButton,
    size: Float,
    nameSize: Float,
    nameWidth: Float,
) {
    val selectedButtonType by quickButtons.selectedButtonType.collectAsState()
    val showName by quickButtonsSettings.showName.collectAsState()
    val isPortrait by orientation.isPortrait.collectAsState()
    val isOn by button.isOn.collectAsState()
    val buttonColor by button.color.collectAsState()
    var presentingRecordConfirm by remember { mutableStateOf(false) }
    var presentingPreviewStreamConfirm by remember { mutableStateOf(false) }
    var presentingStartWorkoutTypePicker by remember { mutableStateOf(false) }
    var presentingStopWorkoutConfirm by remember { mutableStateOf(false) }

    fun torchAction() {
        button.isOn.value = !button.isOn.value
        model.toggleTorch()
    }

    fun muteAction() {
        button.isOn.value = !button.isOn.value
        model.toggleMute()
    }

    fun stealthModeAction() {
        model.toggleStealthMode()
    }

    fun lockScreenAction() {
        model.toggleLockScreen()
    }

    fun imageAction() {
        model.streamOverlay.showingCamera.value = !model.streamOverlay.showingCamera.value
        model.updateImageButtonState()
    }

    fun recordAction() {
        if (!model.isRecording.value) {
            model.startRecording()
        } else {
            model.stopRecording()
        }
    }

    fun movieAction() {
        model.toggleFilterQuickButton(type = SettingsQuickButtonType.movie)
    }

    fun whirlpoolAction() {
        model.toggleWhirlpoolQuickButton()
    }

    fun pinchAction() {
        model.togglePinchQuickButton()
    }

    fun fourThreeAction() {
        model.toggleFilterQuickButton(type = SettingsQuickButtonType.fourThree)
    }

    fun crtAction() {
        model.toggleFilterQuickButton(type = SettingsQuickButtonType.crt)
    }

    fun grayScaleAction() {
        model.toggleFilterQuickButton(type = SettingsQuickButtonType.grayScale)
    }

    fun sepiaAction() {
        model.toggleFilterQuickButton(type = SettingsQuickButtonType.sepia)
    }

    fun tripleAction() {
        model.toggleFilterQuickButton(type = SettingsQuickButtonType.triple)
    }

    fun cameraManAction() {
        model.toggleCameraManQuickButton()
    }

    fun twinAction() {
        model.toggleFilterQuickButton(type = SettingsQuickButtonType.twin)
    }

    fun pixellateAction() {
        model.togglePixellateQuickButton()
    }

    fun pollAction() {
        model.togglePollQuickButton()
    }

    fun streamAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.stream, panel = ShowingPanel.streamSwitcher)
    }

    fun gridAction() {
        button.isOn.value = !button.isOn.value
        model.showingGrid.value = !model.showingGrid.value
        model.sceneUpdated(updateRemoteScene = false)
    }

    fun levelAction() {
        button.isOn.value = !button.isOn.value
        model.showingCameraLevel.value = !model.showingCameraLevel.value
        model.reloadCameraLevel()
        model.sceneUpdated(updateRemoteScene = false)
    }

    fun obsAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.obs, panel = ShowingPanel.obs)
    }

    fun remoteAction() {
        model.showingRemoteControl.value = !model.showingRemoteControl.value
        model.setQuickButton(
            type = SettingsQuickButtonType.remote,
            isOn = model.showingRemoteControl.value,
        )
    }

    fun drawAction() {
        model.toggleDrawOnStream()
    }

    fun localOverlaysAction() {
        model.toggleQuickButton(type = SettingsQuickButtonType.localOverlays)
        model.toggleLocalOverlays()
    }

    fun browserAction() {
        model.toggleQuickButton(type = SettingsQuickButtonType.browser)
        model.toggleBrowser()
    }

    fun navigationAction() {
        model.toggleQuickButton(type = SettingsQuickButtonType.navigation)
        model.toggleNavigation()
    }

    fun cameraPreviewAction() {
        button.isOn.value = !button.isOn.value
        if (button.isOn.value) {
            model.makeToast(
                title = localized("Widgets will not be visible on screen when Camera preview is on"),
                subTitle = localized("They will be visible on stream and in recordings"),
            )
        }
        model.toggleCameraPreview()
    }

    fun snapshotAction() {
        model.takeSnapshot()
    }

    fun widgetsAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.widgets, panel = ShowingPanel.sceneWidgets)
    }

    fun lutsAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.luts, panel = ShowingPanel.luts)
        model.updateLutsButtonState()
    }

    fun chatAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.chat, panel = ShowingPanel.chat)
    }

    fun interactiveChatAction() {
        model.toggleQuickButton(type = SettingsQuickButtonType.interactiveChat)
        model.chat.interactiveChat.value = button.isOn.value
        model.chatActivityFeed.interactiveChat.value = button.isOn.value
        if (!button.isOn.value) {
            model.disableInteractiveChat()
        }
    }

    fun micAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.mic, panel = ShowingPanel.mic)
    }

    fun bitrateAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.bitrate, panel = ShowingPanel.bitrate)
    }

    fun skipCurrentTtsAction() {
        model.chatTextToSpeech.skipCurrentMessage()
    }

    fun pauseTtsAction() {
        model.toggleTextToSpeechPaused()
    }

    fun streamMarkerAction() {
        model.createStreamMarker()
    }

    fun reloadBrowserWidgetsAction() {
        model.reloadBrowserWidgets()
    }

    fun djiDevicesAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.djiDevices, panel = ShowingPanel.djiDevices)
    }

    fun portraitAction() {
        model.setDisplayPortrait(portrait = !model.database.portrait)
        model.reattachCamera()
    }

    fun goProAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.goPro, panel = ShowingPanel.goPro)
    }

    fun replayAction() {
        model.streamOverlay.showingReplay.value = !model.streamOverlay.showingReplay.value
        model.toggleQuickButton(type = SettingsQuickButtonType.replay)
    }

    fun liveAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.live, panel = ShowingPanel.live)
    }

    fun connectionPrioritiesAction() {
        model.toggleShowingPanel(
            type = SettingsQuickButtonType.connectionPriorities,
            panel = ShowingPanel.connectionPriorities,
        )
    }

    fun autoSceneSwitcherAction() {
        model.toggleShowingPanel(
            type = SettingsQuickButtonType.autoSceneSwitcher,
            panel = ShowingPanel.autoSceneSwitcher,
        )
        model.updateAutoSceneSwitcherButtonState()
    }

    fun blurFacesAction() {
        model.toggleBlurFaces()
    }

    fun blurTextAction() {
        model.toggleBlurText()
    }

    fun privacyAction() {
        model.togglePrivacy()
    }

    fun moblinInMouthAction() {
        model.toggleMoblinInMouth()
    }

    fun glassesAction() {
        model.triggerGlasses()
    }

    fun sparkleAction() {
        model.triggerSparkle()
    }

    fun beautyAction() {
        model.toggleBeautyQuickButton()
    }

    fun videoPreviewAction() {
        model.toggleVideoPreview()
        model.toggleQuickButton(type = SettingsQuickButtonType.videoPreview)
    }

    fun interactiveBrowserWidgetsAction() {
        model.setInteractiveBrowserWidgets(on = !button.isOn.value)
    }

    fun macrosAction() {
        model.toggleShowingPanel(type = SettingsQuickButtonType.macros, panel = ShowingPanel.macros)
    }

    fun gimbalTrackingAction() {
        model.toggleGimbalTracking()
    }

    fun previewStreamAction() {
        model.togglePreviewStream()
    }

    fun photoShootAction() {
        model.toggleQuickButton(type = SettingsQuickButtonType.photoShoot)
        model.photoShootEnabled.value = button.isOn.value
        if (model.photoShootEnabled.value) {
            model.startPhotoShoot()
        } else {
            model.stopPhotoShoot()
        }
        model.togglePhotoShoot()
    }

    Column(
        modifier = Modifier.rotate(180.0f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            val buttonView: @Composable () -> Unit = {
                when (button.type) {
                    SettingsQuickButtonType.unknown -> {
                        QuickButtonPlaceholderImage(size = size)
                    }
                    SettingsQuickButtonType.torch -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            torchAction()
                        }
                    }
                    SettingsQuickButtonType.mute -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            muteAction()
                        }
                    }
                    SettingsQuickButtonType.bitrate -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            bitrateAction()
                        }
                    }
                    SettingsQuickButtonType.mic -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            micAction()
                        }
                    }
                    SettingsQuickButtonType.chat -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            chatAction()
                        }
                    }
                    SettingsQuickButtonType.interactiveChat -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            interactiveChatAction()
                        }
                    }
                    SettingsQuickButtonType.blackScreen -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            stealthModeAction()
                        }
                    }
                    SettingsQuickButtonType.lockScreen -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            lockScreenAction()
                        }
                    }
                    SettingsQuickButtonType.record -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            if (model.database.startStopRecordingConfirmations) {
                                presentingRecordConfirm = true
                            } else {
                                recordAction()
                            }
                        }
                        if (presentingRecordConfirm) {
                            ConfirmationDialog(onDismiss = { presentingRecordConfirm = false }) {
                                TextButton(onClick = {
                                    presentingRecordConfirm = false
                                    recordAction()
                                }) {
                                    Text(
                                        if (isOn) {
                                            localized("Stop recording")
                                        } else {
                                            localized("Start recording")
                                        },
                                    )
                                }
                            }
                        }
                    }
                    SettingsQuickButtonType.image -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            imageAction()
                        }
                    }
                    SettingsQuickButtonType.movie -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            movieAction()
                        }
                    }
                    SettingsQuickButtonType.fourThree -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            fourThreeAction()
                        }
                    }
                    SettingsQuickButtonType.crt -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            crtAction()
                        }
                    }
                    SettingsQuickButtonType.grayScale -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            grayScaleAction()
                        }
                    }
                    SettingsQuickButtonType.sepia -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            sepiaAction()
                        }
                    }
                    SettingsQuickButtonType.triple -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            tripleAction()
                        }
                    }
                    SettingsQuickButtonType.twin -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            twinAction()
                        }
                    }
                    SettingsQuickButtonType.cameraMan -> {
                        Box(contentAlignment = Alignment.Center) {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                cameraManAction()
                            }
                            Box(
                                modifier = Modifier.size(size.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                SystemImage(
                                    name = "arrow.up.and.down.and.arrow.left.and.right",
                                    fontSize = 10.sp,
                                    modifier = Modifier
                                        .offset(x = (-5).dp, y = 2.dp)
                                        .stroke(color = remember(buttonColor) { button.backgroundColor.color() }),
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                    SettingsQuickButtonType.pixellate -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            pixellateAction()
                        }
                    }
                    SettingsQuickButtonType.stream -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            streamAction()
                        }
                    }
                    SettingsQuickButtonType.grid -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            gridAction()
                        }
                    }
                    SettingsQuickButtonType.cameraLevel -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            levelAction()
                        }
                    }
                    SettingsQuickButtonType.obs -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            obsAction()
                        }
                    }
                    SettingsQuickButtonType.remote -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            remoteAction()
                        }
                    }
                    SettingsQuickButtonType.draw -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            drawAction()
                        }
                    }
                    SettingsQuickButtonType.localOverlays -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            localOverlaysAction()
                        }
                    }
                    SettingsQuickButtonType.browser -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            browserAction()
                        }
                    }
                    SettingsQuickButtonType.cameraPreview -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            cameraPreviewAction()
                        }
                    }
                    SettingsQuickButtonType.poll -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            pollAction()
                        }
                    }
                    SettingsQuickButtonType.snapshot -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            snapshotAction()
                        }
                    }
                    SettingsQuickButtonType.widgets -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            widgetsAction()
                        }
                    }
                    SettingsQuickButtonType.luts -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            lutsAction()
                        }
                    }
                    SettingsQuickButtonType.workout -> {
                        if (isOn) {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                presentingStopWorkoutConfirm = true
                            }
                            if (presentingStopWorkoutConfirm) {
                                ConfirmationDialog(onDismiss = { presentingStopWorkoutConfirm = false }) {
                                    TextButton(onClick = {
                                        presentingStopWorkoutConfirm = false
                                        model.stopWorkout()
                                    }) {
                                        Text(localized("End workout"))
                                    }
                                }
                            }
                        } else {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                presentingStartWorkoutTypePicker = true
                            }
                            if (presentingStartWorkoutTypePicker) {
                                ConfirmationDialog(onDismiss = { presentingStartWorkoutTypePicker = false }) {
                                    TextButton(onClick = {
                                        presentingStartWorkoutTypePicker = false
                                        model.startWorkout(type = TODO("walking"))
                                    }) {
                                        Text(localized("Start walking workout"))
                                    }
                                    TextButton(onClick = {
                                        presentingStartWorkoutTypePicker = false
                                        model.startWorkout(type = WatchProtocolWorkoutType.running)
                                    }) {
                                        Text(localized("Start running workout"))
                                    }
                                    TextButton(onClick = {
                                        presentingStartWorkoutTypePicker = false
                                        model.startWorkout(type = WatchProtocolWorkoutType.cycling)
                                    }) {
                                        Text(localized("Start cycling workout"))
                                    }
                                }
                            }
                        }
                    }
                    SettingsQuickButtonType.moderation -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            model.presentingModeration.value = true
                        }
                    }
                    SettingsQuickButtonType.predefinedMessages -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            model.presentingPredefinedMessages.value = true
                        }
                    }
                    SettingsQuickButtonType.skipCurrentTts -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            skipCurrentTtsAction()
                        }
                    }
                    SettingsQuickButtonType.streamMarker -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            streamMarkerAction()
                        }
                    }
                    SettingsQuickButtonType.reloadBrowserWidgets -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            reloadBrowserWidgetsAction()
                        }
                    }
                    SettingsQuickButtonType.djiDevices -> {
                        Box(contentAlignment = Alignment.Center) {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                djiDevicesAction()
                            }
                            ButtonTextOverlayView(text = localized("DJI"))
                        }
                    }
                    SettingsQuickButtonType.portrait -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            portraitAction()
                        }
                    }
                    SettingsQuickButtonType.goPro -> {
                        Box(contentAlignment = Alignment.Center) {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                goProAction()
                            }
                            ButtonTextOverlayView(text = localized("GoPro"))
                        }
                    }
                    SettingsQuickButtonType.replay -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            replayAction()
                        }
                    }
                    SettingsQuickButtonType.instantReplay -> {
                        InstantReplayView(model = model, replay = model.replay, button = button, size = size)
                    }
                    SettingsQuickButtonType.connectionPriorities -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            connectionPrioritiesAction()
                        }
                    }
                    SettingsQuickButtonType.whirlpool -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            whirlpoolAction()
                        }
                    }
                    SettingsQuickButtonType.pinch -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            pinchAction()
                        }
                    }
                    SettingsQuickButtonType.autoSceneSwitcher -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            autoSceneSwitcherAction()
                        }
                    }
                    SettingsQuickButtonType.pauseTts -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            pauseTtsAction()
                        }
                    }
                    SettingsQuickButtonType.live -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            liveAction()
                        }
                    }
                    SettingsQuickButtonType.navigation -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            navigationAction()
                        }
                    }
                    SettingsQuickButtonType.blurFaces -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            blurFacesAction()
                        }
                    }
                    SettingsQuickButtonType.blurText -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            blurTextAction()
                        }
                    }
                    SettingsQuickButtonType.moblinInMouth -> {
                        Box(contentAlignment = Alignment.Center) {
                            QuickButtonImage(
                                model,
                                quickButtonsSettings,
                                button,
                                size,
                                hideImage = true,
                            ) {
                                moblinInMouthAction()
                            }
                            AssetImage(
                                name = "MoblinInMouth",
                                modifier = Modifier
                                    .offset(x = 0.dp, y = 3.dp)
                                    .size(21.dp, 40.dp),
                            )
                        }
                    }
                    SettingsQuickButtonType.privacy -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            privacyAction()
                        }
                    }
                    SettingsQuickButtonType.glasses -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            glassesAction()
                        }
                    }
                    SettingsQuickButtonType.sparkle -> {
                        Box(contentAlignment = Alignment.Center) {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                sparkleAction()
                            }
                            Box(
                                modifier = Modifier.size(size.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                SystemImage(
                                    name = "sparkle",
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .offset(x = 11.dp, y = 0.dp)
                                        .rotate(70.0f),
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                    SettingsQuickButtonType.beauty -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            beautyAction()
                        }
                    }
                    SettingsQuickButtonType.videoPreview -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            videoPreviewAction()
                        }
                    }
                    SettingsQuickButtonType.interactiveBrowserWidgets -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            interactiveBrowserWidgetsAction()
                        }
                    }
                    SettingsQuickButtonType.macros -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            macrosAction()
                        }
                    }
                    SettingsQuickButtonType.gimbalTracking -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            gimbalTrackingAction()
                        }
                    }
                    SettingsQuickButtonType.previewStream -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            presentingPreviewStreamConfirm = true
                        }
                        if (presentingPreviewStreamConfirm) {
                            ConfirmationDialog(onDismiss = { presentingPreviewStreamConfirm = false }) {
                                TextButton(onClick = {
                                    presentingPreviewStreamConfirm = false
                                    previewStreamAction()
                                }) {
                                    Text(
                                        if (isOn) {
                                            localized("Stop preview stream")
                                        } else {
                                            localized("Start preview stream")
                                        },
                                    )
                                }
                            }
                        }
                    }
                    SettingsQuickButtonType.photoShoot -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            photoShootAction()
                        }
                    }
                }
            }
            if (button.type == selectedButtonType) {
                Box(contentAlignment = Alignment.Center) {
                    buttonView()
                    Box(
                        modifier = Modifier
                            .size(size.dp)
                            .border(2.dp, Color(0xFFFFCC00), CircleShape),
                    )
                }
            } else {
                buttonView()
            }
        }
        if (showName && !isPortrait) {
            MinimumScaleText(
                text = button.name,
                fontSize = nameSize.sp,
                minimumScaleFactor = 0.5f,
                maxLines = 2,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(0.dp)
                    .width(nameWidth.dp),
            )
        }
    }
}

@Composable
private fun SystemImage(
    name: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    com.moblin.android.platform.SystemImage(name = name, fontSize = fontSize, modifier = modifier, tint = tint)
}

@Composable
private fun AssetImage(
    name: String,
    modifier: Modifier = Modifier,
) {
    val image = remember(name) { Bundle.image(name)?.asImageBitmap() }
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}
