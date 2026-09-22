package com.moblin.android.view.controlbar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.color
import com.moblin.android.common.various.controlBarButtonSize
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.QuickButtons
import com.moblin.android.various.model.ReplayProvider
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.WorkoutType
import com.moblin.android.various.settings.SettingsPanel
import com.moblin.android.various.settings.SettingsQuickButton
import com.moblin.android.various.settings.SettingsQuickButtonType
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.LocalModel

val controlBarPages = 5

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
    val image = if (button.isOn) {
        button.imageOn
    } else {
        button.imageOff
    }
    val foregroundColor = if (hideImage) {
        Color.Transparent
    } else {
        Color.White
    }
    val backgroundColor = button.backgroundColor.color()
    val iconSize = if (quickButtonsSettings.bigButtons) {
        20.sp
    } else {
        MaterialTheme.typography.bodyLarge.fontSize
    }
    Box(
        modifier = Modifier
            .size(buttonSize.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .combinedClickable(
                onClick = { onTapGesture() },
                onLongClick = { model.showQuickButtonSettings(type = button.type) },
            ),
    ) {
        SystemImage(
            name = image,
            fontSize = iconSize,
            modifier = Modifier.align(Alignment.Center),
            tint = foregroundColor,
        )
        if (button.isOn) {
            Box(
                modifier = Modifier
                    .size((buttonSize - 1).dp)
                    .border(1.dp, Color.White, CircleShape)
                    .align(Alignment.Center),
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
    if (replay.isPlaying) {
        Text(
            text = replay.timeLeft.toString(),
            fontSize = 25.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(button.backgroundColor.color())
                .combinedClickable(
                    onClick = {
                        if (model.stream.replay.enabled) {
                            model.instantReplay()
                        } else {
                            model.makeReplayIsNotEnabledToast()
                        }
                    },
                    onLongClick = {
                        model.showQuickButtonSettings(type = SettingsQuickButtonType.instantReplay)
                    },
                ),
        )
    } else {
        QuickButtonImage(
            model = model,
            quickButtonsSettings = model.database.quickButtonsGeneral,
            button = button,
            buttonSize = size,
        ) {
            if (model.stream.replay.enabled) {
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
        modifier = Modifier.padding(0.dp),
    ) {
        SystemImage(
            name = "pawprint",
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
            modifier = Modifier
                .size(size.dp)
                .alpha(0.0f),
            tint = Color.Black,
        )
    }
}

@Composable
private fun ButtonTextOverlayView(text: String) {
    Text(
        text = text,
        fontSize = 8.sp,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .rotate(-90.0f)
            .offset(x = 10.dp, y = 0.dp)
            .size(controlBarButtonSize.dp, controlBarButtonSize.dp),
    )
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
    val presentingRecordConfirm = remember { mutableStateOf(false) }
    val presentingPreviewStreamConfirm = remember { mutableStateOf(false) }
    val presentingStartWorkoutTypePicker = remember { mutableStateOf(false) }
    val presentingStopWorkoutConfirm = remember { mutableStateOf(false) }

    fun torchAction() {
        button.isOn = !button.isOn
        model.toggleTorch()
    }

    fun muteAction() {
        button.isOn = !button.isOn
        model.toggleMute()
    }

    fun stealthModeAction() {
        model.toggleStealthMode()
    }

    fun lockScreenAction() {
        model.toggleLockScreen()
    }

    fun imageAction() {
        model.streamOverlay.showingCamera = !model.streamOverlay.showingCamera
        model.updateImageButtonState()
    }

    fun recordAction() {
        if (!model.isRecording) {
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
        model.toggleShowingPanel(type = ShowingPanel.stream, panel = SettingsPanel.streamSwitcher)
    }

    fun gridAction() {
        button.isOn = !button.isOn
        model.showingGrid = !model.showingGrid
        model.sceneUpdated(updateRemoteScene = false)
    }

    fun levelAction() {
        button.isOn = !button.isOn
        model.showingCameraLevel = !model.showingCameraLevel
        model.reloadCameraLevel()
        model.sceneUpdated(updateRemoteScene = false)
    }

    fun obsAction() {
        model.toggleShowingPanel(type = ShowingPanel.obs, panel = SettingsPanel.obs)
    }

    fun remoteAction() {
        model.showingRemoteControl = !model.showingRemoteControl
        model.setQuickButton(
            type = SettingsQuickButtonType.remote,
            isOn = model.showingRemoteControl,
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
        button.isOn = !button.isOn
        if (button.isOn) {
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
        model.toggleShowingPanel(type = ShowingPanel.widgets, panel = SettingsPanel.sceneWidgets)
    }

    fun lutsAction() {
        model.toggleShowingPanel(type = ShowingPanel.luts, panel = SettingsPanel.luts)
        model.updateLutsButtonState()
    }

    fun chatAction() {
        model.toggleShowingPanel(type = ShowingPanel.chat, panel = SettingsPanel.chat)
    }

    fun interactiveChatAction() {
        model.toggleQuickButton(type = SettingsQuickButtonType.interactiveChat)
        model.chat.interactiveChat = button.isOn
        model.chatActivityFeed.interactiveChat = button.isOn
        if (!button.isOn) {
            model.disableInteractiveChat()
        }
    }

    fun micAction() {
        model.toggleShowingPanel(type = ShowingPanel.mic, panel = SettingsPanel.mic)
    }

    fun bitrateAction() {
        model.toggleShowingPanel(type = ShowingPanel.bitrate, panel = SettingsPanel.bitrate)
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
        model.toggleShowingPanel(type = ShowingPanel.djiDevices, panel = SettingsPanel.djiDevices)
    }

    fun portraitAction() {
        model.setDisplayPortrait(portrait = !model.database.portrait)
        model.reattachCamera()
    }

    fun goProAction() {
        model.toggleShowingPanel(type = ShowingPanel.goPro, panel = SettingsPanel.goPro)
    }

    fun replayAction() {
        model.streamOverlay.showingReplay = !model.streamOverlay.showingReplay
        model.toggleQuickButton(type = SettingsQuickButtonType.replay)
    }

    fun liveAction() {
        model.toggleShowingPanel(type = ShowingPanel.live, panel = SettingsPanel.live)
    }

    fun connectionPrioritiesAction() {
        model.toggleShowingPanel(
            type = ShowingPanel.connectionPriorities,
            panel = SettingsPanel.connectionPriorities,
        )
    }

    fun autoSceneSwitcherAction() {
        model.toggleShowingPanel(
            type = ShowingPanel.autoSceneSwitcher,
            panel = SettingsPanel.autoSceneSwitcher,
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
        model.setInteractiveBrowserWidgets(on = !button.isOn)
    }

    fun macrosAction() {
        model.toggleShowingPanel(type = ShowingPanel.macros, panel = SettingsPanel.macros)
    }

    fun gimbalTrackingAction() {
        model.toggleGimbalTracking()
    }

    fun previewStreamAction() {
        model.togglePreviewStream()
    }

    fun photoShootAction() {
        model.toggleQuickButton(type = SettingsQuickButtonType.photoShoot)
        model.photoShootEnabled = button.isOn
        if (model.photoShootEnabled) {
            model.startPhotoShoot()
        } else {
            model.stopPhotoShoot()
        }
        model.togglePhotoShoot()
    }

    Column(
        modifier = Modifier.rotate(180.0f),
    ) {
        Box {
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
                                presentingRecordConfirm.value = true
                            } else {
                                recordAction()
                            }
                        }
                        if (presentingRecordConfirm.value) {
                            AlertDialog(
                                onDismissRequest = { presentingRecordConfirm.value = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        presentingRecordConfirm.value = false
                                        recordAction()
                                    }) {
                                        Text(
                                            if (button.isOn) {
                                                localized("Stop recording")
                                            } else {
                                                localized("Start recording")
                                            },
                                        )
                                    }
                                },
                            )
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
                        Box {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                cameraManAction()
                            }
                            SystemImage(
                                name = "arrow.up.and.down.and.arrow.left.and.right",
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .offset(x = (-5).dp, y = 2.dp)
                                    .size(size.dp),
                                tint = Color.White,
                            )
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
                        if (button.isOn) {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                presentingStopWorkoutConfirm.value = true
                            }
                            if (presentingStopWorkoutConfirm.value) {
                                AlertDialog(
                                    onDismissRequest = { presentingStopWorkoutConfirm.value = false },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            presentingStopWorkoutConfirm.value = false
                                            model.stopWorkout()
                                        }) {
                                            Text(localized("End workout"))
                                        }
                                    },
                                )
                            }
                        } else {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                presentingStartWorkoutTypePicker.value = true
                            }
                            if (presentingStartWorkoutTypePicker.value) {
                                AlertDialog(
                                    onDismissRequest = { presentingStartWorkoutTypePicker.value = false },
                                    confirmButton = {
                                        Column {
                                            TextButton(onClick = {
                                                presentingStartWorkoutTypePicker.value = false
                                                model.startWorkout(type = WorkoutType.walking)
                                            }) {
                                                Text(localized("Start walking workout"))
                                            }
                                            TextButton(onClick = {
                                                presentingStartWorkoutTypePicker.value = false
                                                model.startWorkout(type = WorkoutType.running)
                                            }) {
                                                Text(localized("Start running workout"))
                                            }
                                            TextButton(onClick = {
                                                presentingStartWorkoutTypePicker.value = false
                                                model.startWorkout(type = WorkoutType.cycling)
                                            }) {
                                                Text(localized("Start cycling workout"))
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                    SettingsQuickButtonType.moderation -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            model.presentingModeration = true
                        }
                    }
                    SettingsQuickButtonType.predefinedMessages -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            model.presentingPredefinedMessages = true
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
                        Box {
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
                        Box {
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
                        Box {
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
                                    .size(21.dp, 40.dp)
                                    .offset(x = 0.dp, y = 3.dp),
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
                        Box {
                            QuickButtonImage(model, quickButtonsSettings, button, size) {
                                sparkleAction()
                            }
                            SystemImage(
                                name = "sparkle",
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .rotate(70.0f)
                                    .offset(x = 11.dp, y = 0.dp)
                                    .size(size.dp),
                                tint = Color.White,
                            )
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
                            presentingPreviewStreamConfirm.value = true
                        }
                        if (presentingPreviewStreamConfirm.value) {
                            AlertDialog(
                                onDismissRequest = { presentingPreviewStreamConfirm.value = false },
                                confirmButton = {
                                    TextButton(onClick = {
                                        presentingPreviewStreamConfirm.value = false
                                        previewStreamAction()
                                    }) {
                                        Text(
                                            if (button.isOn) {
                                                localized("Stop preview stream")
                                            } else {
                                                localized("Start preview stream")
                                            },
                                        )
                                    }
                                },
                            )
                        }
                    }
                    SettingsQuickButtonType.photoShoot -> {
                        QuickButtonImage(model, quickButtonsSettings, button, size) {
                            photoShootAction()
                        }
                    }
                }
            }
            if (button.type == quickButtons.selectedButtonType) {
                Box {
                    buttonView()
                    Box(
                        modifier = Modifier
                            .size((size - 2).dp)
                            .border(2.dp, Color.Yellow, CircleShape)
                            .align(Alignment.Center),
                    )
                }
            } else {
                buttonView()
            }
        }
        if (quickButtonsSettings.showName && !orientation.isPortrait) {
            Text(
                text = button.name,
                fontSize = nameSize.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(nameWidth.dp)
                    .padding(0.dp),
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
    TODO("no Android counterpart for SF Symbol $name")
}

@Composable
private fun AssetImage(
    name: String,
    modifier: Modifier = Modifier,
) {
    TODO("port the asset $name to an Android drawable resource")
}
