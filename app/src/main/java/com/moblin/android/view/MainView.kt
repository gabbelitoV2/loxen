package com.moblin.android.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.streamingplatforms.twitch.TwitchLoginView
import com.moblin.android.various.WebBrowserController
import com.moblin.android.various.model.AudioProvider
import com.moblin.android.various.model.Browser
import com.moblin.android.various.model.CameraState
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.ReplayProvider
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.Toast
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.various.settings.SettingsWidgetLayout
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.isPhone
import com.moblin.android.videoeffects.browser.BrowserEffect
import com.moblin.android.videoeffects.toPixels
import com.moblin.android.view.controlbar.ControlBarLandscapeView
import com.moblin.android.view.controlbar.ControlBarPortraitView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonAutoSceneSwitcherView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonBitrateView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonChatView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonDjiDevicesView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonGoProView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonLiveView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonLutsView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonMacrosView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonMicView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonObsView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonSceneWidgetsView
import com.moblin.android.view.controlbar.quickbutton.QuickButtonStreamSwitcherView
import com.moblin.android.view.controlbar.quickbutton.chat.PredefinedMessagesView
import com.moblin.android.view.controlbar.quickbutton.chat.QuickButtonChatModerationView
import com.moblin.android.view.controlbar.remotecontrolassistant.ControlBarRemoteControlAssistantView
import com.moblin.android.view.main.LockScreenView
import com.moblin.android.view.main.SnapshotCountdownView
import com.moblin.android.view.main.StealthModeView
import com.moblin.android.view.settings.SettingsView
import com.moblin.android.view.settings.display.quickbuttons.QuickButtonsButtonSettingsView
import com.moblin.android.view.settings.display.streambutton.StreamButtonsSettingsView
import com.moblin.android.view.settings.scenes.scene.SceneSettingsView
import com.moblin.android.view.settings.settingsHalfWidth
import com.moblin.android.view.settings.store.StoreSettingsView
import com.moblin.android.view.settings.streams.stream.srt.StreamSrtConnectionPriorityView
import com.moblin.android.view.stream.CameraLevelView
import com.moblin.android.view.stream.DrawOnStreamView
import com.moblin.android.view.stream.StreamGridView
import com.moblin.android.view.stream.StreamOverlayView
import com.moblin.android.view.stream.overlay.StreamOverlayNavigationView
import com.moblin.android.view.webbrowser.WebBrowserView

@Composable
fun CloseButtonView(onClose: () -> Unit) {
    IconButton(onClick = onClose) {
        Box(
            modifier = Modifier
                .padding(7.dp)
                .size(30.dp)
                .border(1.dp, Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun CloseButtonTopRightView(onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1f))
        Column(modifier = Modifier.fillMaxHeight()) {
            Box(modifier = Modifier.padding(16.dp)) {
                CloseButtonView(onClose = onClose)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HideShowButtonPanelView(model: Model) {
    IconButton(onClick = { model.panelHidden = !model.panelHidden }) {
        Icon(
            imageVector = if (model.panelHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(7.dp)
        )
    }
}

@Composable
private fun PanelButtonsView(model: Model, backgroundColor: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier
                    .padding((-3).dp)
                    .background(backgroundColor, RoundedCornerShape(7.dp))
                    .padding(3.dp)
            ) {
                HideShowButtonPanelView(model = model)
                CloseButtonView(onClose = { onClose(model = model) })
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun onClose(model: Model) {
    model.toggleShowingPanel(type = null, panel = ShowingPanel.NONE)
    model.updateLutsButtonState()
    model.updateAutoSceneSwitcherButtonState()
}

@Composable
private fun MenuView(model: Model) {
    when (model.showingPanel) {
        ShowingPanel.SETTINGS -> SettingsView(database = model.database)
        ShowingPanel.BITRATE -> QuickButtonBitrateView(
            model = model,
            database = model.database,
            stream = model.stream
        )
        ShowingPanel.MIC -> QuickButtonMicView(
            model = model,
            mics = model.database.mics,
            modelMic = model.mic
        )
        ShowingPanel.STREAM_SWITCHER -> QuickButtonStreamSwitcherView(database = model.database)
        ShowingPanel.LUTS -> QuickButtonLutsView(model = model, color = model.database.color)
        ShowingPanel.OBS -> QuickButtonObsView(
            stream = model.stream,
            obsQuickButton = model.obsQuickButton
        )
        ShowingPanel.SCENE_WIDGETS -> QuickButtonSceneWidgetsView(sceneSelector = model.sceneSelector)
        ShowingPanel.STORE -> StoreSettingsView(model = model, store = model.store)
        ShowingPanel.CHAT -> QuickButtonChatView(
            model = model,
            orientation = model.orientation,
            quickButtonChat = model.quickButtonChatState
        )
        ShowingPanel.DJI_DEVICES -> QuickButtonDjiDevicesView(
            model = model,
            djiDevices = model.database.djiDevices
        )
        ShowingPanel.SCENE_SETTINGS -> key(model.sceneSettingsPanelSceneId) {
            SceneSettingsView(
                database = model.database,
                scene = model.sceneSettingsPanelScene
            )
        }
        ShowingPanel.GO_PRO -> QuickButtonGoProView(
            goProState = model.goPro,
            goPro = model.database.goPro
        )
        ShowingPanel.CONNECTION_PRIORITIES -> StreamSrtConnectionPriorityView(stream = model.stream)
        ShowingPanel.AUTO_SCENE_SWITCHER -> QuickButtonAutoSceneSwitcherView(
            autoSceneSwitcher = model.autoSceneSwitcher,
            autoSceneSwitchers = model.database.autoSceneSwitchers
        )
        ShowingPanel.QUICK_BUTTON_SETTINGS -> model.quickButtonSettingsButton?.let { button ->
            QuickButtonsButtonSettingsView(
                model = model,
                orientation = model.orientation,
                quickButtonsSettings = model.database.quickButtonsGeneral,
                button = button,
                showAll = true
            )
        }
        ShowingPanel.STREAMING_BUTTON_SETTINGS -> StreamButtonsSettingsView(database = model.database)
        ShowingPanel.LIVE -> QuickButtonLiveView(
            model = model,
            database = model.database,
            stream = model.stream
        )
        ShowingPanel.MACROS -> QuickButtonMacrosView(model = model, macros = model.database.macros)
        ShowingPanel.NONE -> {}
    }
}

@Composable
fun BrowserWidgetView(browser: Browser, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { browser.browserEffect.webView },
        modifier = modifier,
        update = { browser.browserEffect.reload() }
    )
}

@Composable
private fun InstantReplayCountdownView(replay: ReplayProvider) {
    if (replay.instantReplayCountdown != 0) {
        Column(
            modifier = Modifier
                .widthIn(max = 200.dp)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Playing instant replay in",
                color = Color.White
            )
            Text(
                text = replay.instantReplayCountdown.toString(),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Composable
private fun MutedView(audio: AudioProvider) {
    if (audio.muted) {
        Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier
                .size(80.dp)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
        )
    }
}

@Composable
private fun PhotoShootView(enabled: Boolean) {
    if (enabled) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(10.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(60.dp)
            )
            Text(
                text = "Photo shoot",
                color = Color.White,
                fontSize = 30.sp
            )
            Text(
                text = "Taking photos periodically",
                color = Color.White
            )
        }
    }
}

@Composable
private fun WebBrowserAlertsView(model: Model) {
    TODO("UIViewControllerRepresentable (WebBrowserController) has no Compose equivalent")
}

private fun DrawScope.drawFocus(size: Size, focusPoint: Offset) {
    val sideLength = 70f
    val x = size.width * focusPoint.x - sideLength / 2
    val y = size.height * focusPoint.y - sideLength / 2
    drawRoundRect(
        color = Color.Yellow,
        topLeft = Offset(x, y),
        size = Size(sideLength, sideLength),
        cornerRadius = CornerRadius(2f, 2f),
        style = Stroke(width = 1f)
    )
}

@Composable
private fun tapToFocusIndicator(size: Size, focusPoint: Offset) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawFocus(size = size, focusPoint = focusPoint)
    }
}

@Composable
private fun StreamOverlayTapGridView(model: Model, camera: CameraState, size: Size) {
    val focusPoint = camera.manualFocusPoint
    if (model.database.tapToFocus && focusPoint != null) {
        tapToFocusIndicator(size = size, focusPoint = focusPoint)
    }
    if (model.showingGrid) {
        StreamGridView()
    }
    if (model.showingCameraLevel) {
        CameraLevelView(cameraLevel = model.cameraLevel)
    }
}

private fun browserWidgetScale(
    layout: SettingsWidgetLayout,
    browserSize: Size,
    streamSize: Size,
): Float {
    val scaleX = toPixels(layout.size.toDouble(), streamSize.width.toDouble()).toFloat() / browserSize.width
    val scaleY = toPixels(layout.size.toDouble(), streamSize.height.toDouble()).toFloat() / browserSize.height
    return minOf(scaleX, scaleY)
}

private fun browserWidgetOffset(
    layout: SettingsWidgetLayout,
    displaySize: Size,
    streamSize: Size,
): Offset {
    val x: Float = if (layout.alignment.isHorizontalCenter()) {
        (streamSize.width - displaySize.width) / 2
    } else if (layout.alignment.isLeft()) {
        toPixels(layout.x.toDouble(), streamSize.width.toDouble()).toFloat()
    } else {
        streamSize.width - toPixels(layout.x.toDouble(), streamSize.width.toDouble()).toFloat() - displaySize.width
    }
    val y: Float = if (layout.alignment.isVerticalCenter()) {
        (streamSize.height - displaySize.height) / 2
    } else if (layout.alignment.isTop()) {
        toPixels(layout.y.toDouble(), streamSize.height.toDouble()).toFloat()
    } else {
        streamSize.height - toPixels(layout.y.toDouble(), streamSize.height.toDouble()).toFloat() - displaySize.height
    }
    return Offset(x, y)
}

@Composable
private fun InteractiveBrowserView(
    browser: Browser,
    browserEffect: BrowserEffect,
    streamSize: Size,
) {
    val layout = browserEffect.layout ?: return
    val browserSize = Size(browserEffect.width.toFloat(), browserEffect.height.toFloat())
    val scale = browserWidgetScale(
        layout = layout,
        browserSize = browserSize,
        streamSize = streamSize
    )
    val displaySize = Size(
        width = scale * browserSize.width,
        height = scale * browserSize.height
    )
    val offset = browserWidgetOffset(
        layout = layout,
        displaySize = displaySize,
        streamSize = streamSize
    )
    BrowserWidgetView(
        browser = browser,
        modifier = Modifier
            .offset(offset.x.dp, offset.y.dp)
            .size(browserSize.width.dp, browserSize.height.dp)
            .scale(scale)
            .size(displaySize.width.dp, displaySize.height.dp)
    )
}

@Composable
fun MainView(
    model: Model,
    webBrowserController: WebBrowserController,
    streamView: @Composable () -> Unit,
    createStreamWizard: CreateStreamWizard,
    toast: Toast,
    orientation: Orientation,
    quickButtons: SettingsQuickButtons,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(model.showingPanel) {
        focused = model.isKeyboardActive()
    }
    LaunchedEffect(model.showBrowser) {
        focused = model.isKeyboardActive()
    }
    LaunchedEffect(model.showTwitchAuth) {
        focused = model.isKeyboardActive()
    }
    LaunchedEffect(createStreamWizard.presenting) {
        focused = model.isKeyboardActive()
    }
    LaunchedEffect(createStreamWizard.presentingSetup) {
        focused = model.isKeyboardActive()
    }
    LaunchedEffect(createStreamWizard.showTwitchAuth) {
        focused = model.isKeyboardActive()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent {
                    model.handleKeyPress(it)
                    true
                }
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(edgesToIgnore(orientation = orientation, quickButtons = quickButtons))
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) {
                    model.setup()
                    focused = true
                    focusRequester.requestFocus()
                }
                if (orientation.isPortrait) {
                    portrait(
                        model = model,
                        streamView = streamView,
                        orientation = orientation,
                        quickButtons = quickButtons
                    )
                } else {
                    landscape(
                        model = model,
                        streamView = streamView,
                        orientation = orientation,
                        quickButtons = quickButtons
                    )
                }
                if (webBrowserController.showAlert) {
                    WebBrowserAlertsView(model = model)
                }
                if (model.showStealthMode) {
                    StealthModeView(
                        model = model,
                        quickButtons = quickButtons,
                        chat = model.chat,
                        chatAlerts = model.chatActivityFeed,
                        stealthMode = model.stealthMode,
                        orientation = orientation
                    )
                }
                if (model.lockScreen) {
                    LockScreenView(model = model)
                }
                SnapshotCountdownView(snapshot = model.snapshot)
                InstantReplayCountdownView(replay = model.replay)

                if (model.showTwitchAuth) {
                    TwitchLoginView(model = model, presenting = model.showTwitchAuth)
                }
                if (model.presentingModeration) {
                    QuickButtonChatModerationView(
                        model = model,
                        presentingModeration = model.presentingModeration
                    )
                }
                if (model.presentingPredefinedMessages) {
                    PredefinedMessagesView(
                        model = model,
                        chat = model.database.chat,
                        filter = model.database.chat.predefinedMessagesFilter,
                        presentingPredefinedMessages = model.presentingPredefinedMessages
                    )
                }
                if (model.presentingSettingsImportConfirmation) {
                    AlertDialog(
                        onDismissRequest = { model.presentingSettingsImportConfirmation = false },
                        title = {
                            Text("Are you sure you want to import settings? This will replace your current settings.")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                model.pendingSettingsImportAction?.invoke()
                                model.pendingSettingsImportAction = null
                            }) {
                                Text("Import settings")
                            }
                        }
                    )
                }
                if (model.presentingStreamImportCollisionConfirmation) {
                    AlertDialog(
                        onDismissRequest = { model.presentingStreamImportCollisionConfirmation = false },
                        title = { Text(model.pendingStreamImportCollisionTitle) },
                        confirmButton = {
                            TextButton(onClick = {
                                model.pendingStreamImportCollisionAction?.invoke(true)
                                model.pendingStreamImportCollisionAction = null
                            }) {
                                Text("Merge")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                model.pendingStreamImportCollisionAction?.invoke(false)
                                model.pendingStreamImportCollisionAction = null
                            }) {
                                Text("Create new")
                            }
                        }
                    )
                }
                if (toast.showingToast) {
                    TODO("toast.toast SwiftUI view has no Compose equivalent")
                }
            }
        }
        if (isMac()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(Color.Black)
            )
        }
    }
}

private fun handleTapToFocus(model: Model, size: Size, location: Offset) {
    if (!model.database.tapToFocus) {
        return
    }
    val x = (location.x / size.width).coerceIn(0f, 1f)
    val y = (location.y / size.height).coerceIn(0f, 1f)
    model.setFocusPointOfInterest(Offset(x, y))
}

private fun handleLeaveTapToFocus(model: Model) {
    if (!model.database.tapToFocus) {
        return
    }
    model.setAutoFocus()
}

@Composable
private fun browserWidgets(model: Model, streamSize: Size) {
    Box(
        modifier = Modifier
            .size(streamSize.width.dp, streamSize.height.dp)
            .alpha(if (model.interactiveBrowsers) 1f else 0f)
    ) {
        model.browsers.forEach { browser ->
            key(browser.id) {
                InteractiveBrowserView(
                    browser = browser,
                    browserEffect = browser.browserEffect,
                    streamSize = streamSize
                )
            }
        }
    }
}

@Composable
private fun streamViewWithWidgets(model: Model, streamView: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val metrics = Size(maxWidth.value, maxHeight.value)
        val layout = model.streamViewLayout(metrics)
        Box(
            modifier = Modifier
                .size(layout.size.width.dp, layout.size.height.dp)
                .offset(layout.offset.x.dp, layout.offset.y.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { location ->
                                handleTapToFocus(
                                    model = model,
                                    size = layout.size,
                                    location = location
                                )
                            },
                            onLongPress = {
                                handleLeaveTapToFocus(model = model)
                            }
                        )
                    }
            ) {
                streamView()
            }
            StreamOverlayTapGridView(
                model = model,
                camera = model.camera,
                size = layout.size
            )
            browserWidgets(model = model, streamSize = layout.size)
        }
    }
}

@Composable
private fun portrait(
    model: Model,
    streamView: @Composable () -> Unit,
    orientation: Orientation,
    quickButtons: SettingsQuickButtons,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(Unit) {
                    while (true) {
                        var totalZoom = 1f
                        detectTransformGestures { _, _, zoom, _ ->
                            totalZoom *= zoom
                            model.changeZoomX(zoom)
                        }
                        model.commitZoomX(totalZoom)
                    }
                }
        ) {
            streamViewWithWidgets(model = model, streamView = streamView)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .padding(bottom = if (orientation.isPortrait) 5.dp else 0.dp)
                        .alpha(if (model.showLocalOverlays) 1f else 0f)
                ) {
                    StreamOverlayView(
                        streamOverlay = model.streamOverlay,
                        chatSettings = model.database.chat,
                        orientation = orientation,
                        width = maxWidth.value
                    )
                }
            }
            if (model.showDrawOnStream && model.stream.portrait) {
                DrawOnStreamView(model = model)
            }
            MutedView(audio = model.audio)
            PhotoShootView(enabled = model.photoShootEnabled)
            if (model.showBrowser) {
                WebBrowserView(
                    model = model,
                    database = model.database,
                    orientation = orientation,
                    webBrowserState = model.webBrowserState
                )
            }
            if (model.showNavigation) {
                StreamOverlayNavigationView(
                    model = model,
                    database = model.database,
                    navigation = model.navigation()
                )
            }
            if (model.showingRemoteControl) {
                ControlBarRemoteControlAssistantView(
                    model = model,
                    remoteControlSettings = model.database.remoteControl
                )
            }
            if (model.showingPanel != ShowingPanel.NONE) {
                Box(modifier = Modifier.alpha(if (model.panelHidden) 0f else 1f)) {
                    MenuView(model = model)
                }
                val backgroundColor = if (model.panelHidden) {
                    model.showingPanel.buttonsBackgroundColor()
                } else {
                    Color.Transparent
                }
                PanelButtonsView(model = model, backgroundColor = backgroundColor)
                    .let { }
            }
        }
        ControlBarPortraitView(model = model, quickButtons = quickButtons)
    }
}

@Composable
private fun landscape(
    model: Model,
    streamView: @Composable () -> Unit,
    orientation: Orientation,
    quickButtons: SettingsQuickButtons,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .pointerInput(Unit) {
                    while (true) {
                        var totalZoom = 1f
                        detectTransformGestures { _, _, zoom, _ ->
                            totalZoom *= zoom
                            model.changeZoomX(zoom)
                        }
                        model.commitZoomX(totalZoom)
                    }
                }
        ) {
            streamViewWithWidgets(model = model, streamView = streamView)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.alpha(if (model.showLocalOverlays) 1f else 0f)) {
                    StreamOverlayView(
                        streamOverlay = model.streamOverlay,
                        chatSettings = model.database.chat,
                        orientation = orientation,
                        width = maxWidth.value
                    )
                }
            }
            if (model.showDrawOnStream) {
                DrawOnStreamView(model = model)
            }
            MutedView(audio = model.audio)
            PhotoShootView(enabled = model.photoShootEnabled)
            if (model.showBrowser) {
                WebBrowserView(
                    model = model,
                    database = model.database,
                    orientation = orientation,
                    webBrowserState = model.webBrowserState
                )
            }
            if (model.showNavigation) {
                StreamOverlayNavigationView(
                    model = model,
                    database = model.database,
                    navigation = model.navigation()
                )
            }
            if (model.showingRemoteControl) {
                ControlBarRemoteControlAssistantView(
                    model = model,
                    remoteControlSettings = model.database.remoteControl
                )
            }
            if (model.showingPanel != ShowingPanel.NONE && model.panelHidden) {
                PanelButtonsView(
                    model = model,
                    backgroundColor = model.showingPanel.buttonsBackgroundColor()
                )
            }
        }
        if (model.showingPanel != ShowingPanel.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (model.panelHidden) 1.dp else settingsHalfWidth.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Box(modifier = Modifier.alpha(if (model.panelHidden) 0f else 1f)) {
                        MenuView(model = model)
                    }
                }
                if (!model.panelHidden) {
                    PanelButtonsView(model = model, backgroundColor = Color.Transparent)
                }
            }
        }
        ControlBarLandscapeView(model = model, quickButtons = quickButtons)
    }
}

private fun edgesToIgnore(
    orientation: Orientation,
    quickButtons: SettingsQuickButtons,
): WindowInsetsSides {
    return if (isPhone()) {
        if (orientation.isPortrait) {
            if (quickButtons.bigButtons && quickButtons.twoColumns) {
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            } else {
                WindowInsetsSides.All
            }
        } else if (quickButtons.bigButtons && quickButtons.twoColumns) {
            WindowInsetsSides.Bottom + WindowInsetsSides.Start
        } else {
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        }
    } else {
        WindowInsetsSides.All
    }
}
