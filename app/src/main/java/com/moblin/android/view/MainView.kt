package com.moblin.android.view

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ButtonRole
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Visibility
import com.moblin.android.streamingplatforms.twitch.TwitchLoginView
import com.moblin.android.various.WebBrowserAlertDialog
import com.moblin.android.various.WebBrowserController
import com.moblin.android.various.model.AudioProvider
import com.moblin.android.various.model.Browser
import com.moblin.android.various.model.CameraState
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.KeyPress
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.ReplayProvider
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.Toast
import com.moblin.android.various.model.changeZoomX
import com.moblin.android.various.model.commitZoomX
import com.moblin.android.various.model.handleKeyPress
import com.moblin.android.various.model.isKeyboardActive
import com.moblin.android.various.model.navigation
import com.moblin.android.various.model.setAutoFocus
import com.moblin.android.various.model.setFocusPointOfInterest
import com.moblin.android.various.model.updateAutoSceneSwitcherButtonState
import com.moblin.android.various.model.updateLutsButtonState
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
import com.moblin.android.view.controlbar.quickbutton.chat.QuickButtonChatView
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
import com.moblin.android.view.stream.StreamViewInsets
import com.moblin.android.view.stream.StreamViewLayout
import com.moblin.android.view.stream.StreamViewMetrics
import com.moblin.android.view.stream.overlay.StreamOverlayNavigationView
import com.moblin.android.view.webbrowser.WebBrowserView
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.delay

private fun Modifier.opacityAndHitTesting(enabled: Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        if (enabled) {
            placeable.place(0, 0)
        }
    }
}

private fun Modifier.magnificationGesture(
    onChanged: (Float) -> Unit,
    onEnded: (Float) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var amount = 1f
        var magnifying = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } >= 2) {
                amount *= event.calculateZoom()
                if (!magnifying && abs(amount - 1f) > 0.01f) {
                    magnifying = true
                }
                if (magnifying) {
                    onChanged(amount)
                }
            }
            if (magnifying) {
                event.changes.forEach {
                    if (it.positionChanged()) {
                        it.consume()
                    }
                }
            }
        } while (event.changes.any { it.pressed })
        if (magnifying) {
            onEnded(amount)
        }
    }
}

@Composable
private fun PlainButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun CircleButtonImage(name: String) {
    Box(
        modifier = Modifier
            .padding(7.dp)
            .size(30.dp)
            .border(1.dp, Color.Gray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        SystemImage(name = name, fontSize = 17.sp, tint = Color.Gray)
    }
}

@Composable
fun CloseButtonView(onClose: () -> Unit) {
    PlainButton(onClick = onClose) {
        CircleButtonImage(name = "xmark")
    }
}

@Composable
fun CloseButtonTopRightView(onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
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
private fun HideShowButtonPanelView(model: Model = LocalModel.current) {
    val panelHidden by model.panelHidden.collectAsState()
    PlainButton(onClick = { model.panelHidden.value = !model.panelHidden.value }) {
        CircleButtonImage(name = if (panelHidden) "eye" else "eye.slash")
    }
}

@Composable
private fun PanelButtonsView(
    model: Model = LocalModel.current,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.drawBehind {
                    val inset = 3.dp.toPx()
                    drawRoundRect(
                        color = backgroundColor,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - 2 * inset, size.height - 2 * inset),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                    )
                }
            ) {
                HideShowButtonPanelView(model = model)
                CloseButtonView(onClose = { onClose(model = model) })
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun onClose(model: Model) {
    model.toggleShowingPanel(type = null, panel = ShowingPanel.none)
    model.updateLutsButtonState()
    model.updateAutoSceneSwitcherButtonState()
}

@Composable
private fun MenuView(model: Model = LocalModel.current) {
    val showingPanel by model.showingPanel.collectAsState()
    val stream by model.stream.collectAsState()
    val quickButtonSettingsButton by model.quickButtonSettingsButton.collectAsState()
    val sceneSettingsPanelSceneId by model.sceneSettingsPanelSceneId.collectAsState()
    when (showingPanel) {
        ShowingPanel.settings -> SettingsView(database = model.database)
        ShowingPanel.bitrate -> QuickButtonBitrateView(
            model = model,
            database = model.database,
            stream = stream
        )
        ShowingPanel.mic -> QuickButtonMicView(
            model = model,
            mics = model.database.mics,
            modelMic = model.mic
        )
        ShowingPanel.streamSwitcher -> QuickButtonStreamSwitcherView(database = model.database)
        ShowingPanel.luts -> QuickButtonLutsView(model = model, color = model.database.color)
        ShowingPanel.obs -> QuickButtonObsView(
            stream = stream,
            obsQuickButton = model.obsQuickButton
        )
        ShowingPanel.sceneWidgets -> QuickButtonSceneWidgetsView(sceneSelector = model.sceneSelector)
        ShowingPanel.store -> StoreSettingsView(model = model, store = model.store)
        ShowingPanel.chat -> QuickButtonChatView(
            model = model,
            orientation = model.orientation,
            quickButtonChat = model.quickButtonChatState
        )
        ShowingPanel.djiDevices -> QuickButtonDjiDevicesView(
            model = model,
            djiDevices = model.database.djiDevices
        )
        ShowingPanel.sceneSettings -> key(sceneSettingsPanelSceneId) {
            SceneSettingsView(
                database = model.database,
                scene = model.sceneSettingsPanelScene
            )
        }
        ShowingPanel.goPro -> QuickButtonGoProView(
            goProState = model.goPro,
            goPro = model.database.goPro
        )
        ShowingPanel.connectionPriorities -> StreamSrtConnectionPriorityView(stream = stream)
        ShowingPanel.autoSceneSwitcher -> QuickButtonAutoSceneSwitcherView(
            autoSceneSwitcher = model.autoSceneSwitcher,
            autoSceneSwitchers = model.database.autoSceneSwitchers
        )
        ShowingPanel.quickButtonSettings -> quickButtonSettingsButton?.let { button ->
            key(button.id) {
                QuickButtonsButtonSettingsView(
                    model = model,
                    orientation = model.orientation,
                    quickButtonsSettings = model.database.quickButtonsGeneral,
                    button = button,
                    showAll = true
                )
            }
        }
        ShowingPanel.streamingButtonSettings -> StreamButtonsSettingsView(database = model.database)
        ShowingPanel.live -> QuickButtonLiveView(
            model = model,
            database = model.database,
            stream = stream
        )
        ShowingPanel.macros -> QuickButtonMacrosView(model = model, macros = model.database.macros)
        ShowingPanel.none -> {}
    }
}

private class BrowserWidgetContainerView(context: Context) : FrameLayout(context) {
    var allowsHitTesting = true

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return allowsHitTesting && super.dispatchTouchEvent(event)
    }
}

@Composable
fun BrowserWidgetView(browser: Browser, modifier: Modifier = Modifier, allowsHitTesting: Boolean = true) {
    AndroidView(
        factory = { context ->
            val webView = browser.browserEffect.webView.borrowView()
            (webView.parent as? ViewGroup)?.removeView(webView)
            val container = BrowserWidgetContainerView(context)
            container.addView(
                webView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
            browser.browserEffect.reload()
            container
        },
        modifier = modifier,
        onRelease = { it.removeAllViews(); browser.browserEffect.webView.returnView() },
        update = { it.allowsHitTesting = allowsHitTesting; browser.browserEffect.webView.setBorrowedViewVisible(allowsHitTesting) }
    )
}

@Composable
private fun InstantReplayCountdownView(replay: ReplayProvider) {
    val instantReplayCountdown by replay.instantReplayCountdown.collectAsState()
    if (instantReplayCountdown != 0) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(10.dp)
                .widthIn(max = 200.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = localized("Playing instant replay in"),
                color = Color.White,
                fontSize = 17.sp
            )
            Text(
                text = instantReplayCountdown.toString(),
                color = Color.White,
                fontSize = 28.sp
            )
        }
    }
}

@Composable
private fun MutedView(audio: AudioProvider) {
    val muted by audio.muted.collectAsState()
    if (muted) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(10.dp)
        ) {
            SystemImage(name = "microphone.slash", fontSize = 80.sp, tint = Color.Red)
        }
    }
}

@Composable
private fun PhotoShootView(enabled: Boolean) {
    if (enabled) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SystemImage(name = "person.crop.square.badge.camera", fontSize = 60.sp, tint = Color.White)
            Text(
                text = localized("Photo shoot"),
                color = Color.White,
                fontSize = 30.sp
            )
            Text(
                text = localized("Taking photos periodically"),
                color = Color.White,
                fontSize = 17.sp
            )
        }
    }
}

@Composable
private fun WebBrowserAlertsView(model: Model = LocalModel.current) {
    WebBrowserAlertDialog(controller = model.webBrowserController)
}

private fun DrawScope.drawFocus(size: Size, focusPoint: Offset) {
    val sideLength = 70.dp.toPx()
    val x = size.width.dp.toPx() * focusPoint.x - sideLength / 2
    val y = size.height.dp.toPx() * focusPoint.y - sideLength / 2
    drawRoundRect(
        color = Color.Yellow,
        topLeft = Offset(x, y),
        size = Size(sideLength, sideLength),
        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        style = Stroke(width = 1.dp.toPx())
    )
}

@Composable
private fun tapToFocusIndicator(size: Size, focusPoint: Offset) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawFocus(size = size, focusPoint = focusPoint)
    }
}

@Composable
private fun StreamOverlayTapGridView(model: Model = LocalModel.current, camera: CameraState, size: Size) {
    val manualFocusPoint by camera.manualFocusPoint.collectAsState()
    val showingGrid by model.showingGrid.collectAsState()
    val showingCameraLevel by model.showingCameraLevel.collectAsState()
    val focusPoint = manualFocusPoint
    if (model.database.tapToFocus && focusPoint != null) {
        tapToFocusIndicator(size = size, focusPoint = Offset(focusPoint.x, focusPoint.y))
    }
    if (showingGrid) {
        StreamGridView()
    }
    if (showingCameraLevel) {
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
    allowsHitTesting: Boolean,
) {
    val browserLayout by browserEffect.layout.collectAsState()
    val layout = browserLayout ?: return
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
    Box(
        modifier = Modifier
            .offset(offset.x.dp, offset.y.dp)
            .size(displaySize.width.dp, displaySize.height.dp),
        contentAlignment = Alignment.Center
    ) {
        BrowserWidgetView(
            browser = browser,
            modifier = Modifier
                .requiredSize(browserSize.width.dp, browserSize.height.dp)
                .scale(scale),
            allowsHitTesting = allowsHitTesting
        )
    }
}

@Composable
private fun AlertToastView(toast: Toast) {
    val showingToast by toast.showingToast.collectAsState()
    val alertToast by toast.toast.collectAsState()
    LaunchedEffect(showingToast, alertToast) {
        if (showingToast) {
            delay(5000)
            toast.showingToast.value = false
        }
    }
    val darkMode = isSystemInDarkTheme()
    val textColor = if (darkMode) Color.White else Color.Black
    AnimatedVisibility(
        visible = showingToast,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (darkMode) Color(0xE6252525) else Color(0xE6F2F2F7))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    toast.onTapped?.invoke()
                    toast.showingToast.value = false
                }
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = alertToast.title,
                color = alertToast.titleColor ?: textColor,
                fontSize = 17.sp,
                fontWeight = if (alertToast.titleFont == null) FontWeight.Bold else FontWeight.Normal,
                fontFamily = alertToast.titleFont,
                textAlign = TextAlign.Center
            )
            alertToast.subTitle?.let { subTitle ->
                Text(
                    text = subTitle,
                    color = textColor,
                    fontSize = if (alertToast.subTitleFont == null) 13.sp else 17.sp,
                    fontFamily = alertToast.subTitleFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(0.7f)
                )
            }
        }
    }
}

@Composable
fun MainView(
    model: Model = LocalModel.current,
    webBrowserController: WebBrowserController,
    streamView: @Composable () -> Unit,
    createStreamWizard: CreateStreamWizard,
    toast: Toast,
    orientation: Orientation,
    quickButtons: SettingsQuickButtons,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val bigButtons by quickButtons.bigButtons.collectAsState()
    val twoColumns by quickButtons.twoColumns.collectAsState()
    val showStealthMode by model.showStealthMode.collectAsState()
    val lockScreen by model.lockScreen.collectAsState()
    val showingPanel by model.showingPanel.collectAsState()
    val showBrowser by model.showBrowser.collectAsState()
    val showTwitchAuth by model.showTwitchAuth.collectAsState()
    val presentingModeration by model.presentingModeration.collectAsState()
    val presentingPredefinedMessages by model.presentingPredefinedMessages.collectAsState()
    val presentingSettingsImportConfirmation by model.presentingSettingsImportConfirmation.collectAsState()
    val presentingStreamImportCollisionConfirmation by model.presentingStreamImportCollisionConfirmation
        .collectAsState()
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }
    val safeAreaSides = edgesToIgnore(isPortrait = isPortrait, bigButtons = bigButtons, twoColumns = twoColumns)
    val streamSafeAreaInsets = streamViewSafeAreaInsets(sides = safeAreaSides, isPortrait = isPortrait)

    LaunchedEffect(
        showingPanel,
        showBrowser,
        showTwitchAuth,
        createStreamWizard.presenting,
        createStreamWizard.presentingSetup,
        createStreamWizard.showTwitchAuth
    ) {
        if (appeared) {
            focused = model.isKeyboardActive()
        }
    }
    LaunchedEffect(Unit) {
        model.setup()
        focused = true
        appeared = true
    }
    LaunchedEffect(focused) {
        if (focused) {
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(safeAreaWindowInsets().only(safeAreaSides))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) {
                        return@onKeyEvent false
                    }
                    val codePoint = event.utf16CodePoint
                    if (codePoint == 0) {
                        return@onKeyEvent false
                    }
                    val press = KeyPress(characters = String(Character.toChars(codePoint)))
                    model.handleKeyPress(press = press) == KeyPress.Result.handled
                }
                .focusRequester(focusRequester)
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            if (isPortrait) {
                portrait(
                    model = model,
                    streamView = streamView,
                    orientation = orientation,
                    quickButtons = quickButtons,
                    safeAreaInsets = streamSafeAreaInsets
                )
            } else {
                landscape(
                    model = model,
                    streamView = streamView,
                    orientation = orientation,
                    quickButtons = quickButtons,
                    safeAreaInsets = streamSafeAreaInsets
                )
            }
            WebBrowserAlertsView(model = model)
            if (showStealthMode) {
                StealthModeView(
                    model = model,
                    quickButtons = quickButtons,
                    chat = model.chat,
                    chatAlerts = model.chatActivityFeed,
                    stealthMode = model.stealthMode,
                    orientation = orientation
                )
            }
            if (lockScreen) {
                LockScreenView(model = model)
            }
            SnapshotCountdownView(snapshot = model.snapshot)
            InstantReplayCountdownView(replay = model.replay)
            AlertToastView(toast = toast)
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
    Sheet(isPresented = showTwitchAuth, onDismissRequest = { model.showTwitchAuth.value = false }) {
        TwitchLoginView(
            model = model,
            presenting = showTwitchAuth,
            onPresentingChange = { model.showTwitchAuth.value = it }
        )
    }
    Sheet(isPresented = presentingModeration, onDismissRequest = { model.presentingModeration.value = false }) {
        QuickButtonChatModerationView(
            model = model,
            presentingModeration = presentingModeration,
            onPresentingModerationChange = { model.presentingModeration.value = it }
        )
    }
    Sheet(
        isPresented = presentingPredefinedMessages,
        onDismissRequest = { model.presentingPredefinedMessages.value = false }
    ) {
        var messageToSend by remember { mutableStateOf<UUID?>(null) }
        PredefinedMessagesView(
            model = model,
            chat = model.database.chat,
            filter = model.database.chat.predefinedMessagesFilter,
            presentingPredefinedMessages = presentingPredefinedMessages,
            onPresentingPredefinedMessagesChange = { model.presentingPredefinedMessages.value = it },
            messageToSend = messageToSend,
            onMessageToSendChange = { messageToSend = it }
        )
    }
    ConfirmationDialog(
        title = "Are you sure you want to import settings? This will replace your current settings.",
        isPresented = presentingSettingsImportConfirmation,
        onDismissRequest = { model.presentingSettingsImportConfirmation.value = false },
        titleVisibility = Visibility.visible
    ) {
        Button("Import settings", role = ButtonRole.destructive) {
            model.pendingSettingsImportAction?.invoke()
            model.pendingSettingsImportAction = null
        }
    }
    ConfirmationDialog(
        title = model.pendingStreamImportCollisionTitle,
        isPresented = presentingStreamImportCollisionConfirmation,
        onDismissRequest = { model.presentingStreamImportCollisionConfirmation.value = false },
        titleVisibility = Visibility.visible
    ) {
        Button("Create new") {
            model.pendingStreamImportCollisionAction?.invoke(false)
            model.pendingStreamImportCollisionAction = null
        }
        Button("Merge", role = ButtonRole.destructive) {
            model.pendingStreamImportCollisionAction?.invoke(true)
            model.pendingStreamImportCollisionAction = null
        }
    }
}

private fun handleTapToFocus(model: Model, size: Size, location: Offset) {
    if (!model.database.tapToFocus) {
        return
    }
    val x = (location.x / size.width).coerceIn(0f, 1f)
    val y = (location.y / size.height).coerceIn(0f, 1f)
    model.setFocusPointOfInterest(PointF(x, y))
}

private fun handleLeaveTapToFocus(model: Model) {
    if (!model.database.tapToFocus) {
        return
    }
    model.setAutoFocus()
}

@Composable
private fun browserWidgets(model: Model = LocalModel.current, streamSize: Size) {
    val browsers by model.browsers.collectAsState()
    val interactiveBrowsers by model.interactiveBrowsers.collectAsState()
    Box(
        modifier = Modifier
            .size(streamSize.width.dp, streamSize.height.dp)
            .alpha(if (interactiveBrowsers) 1f else 0f)
    ) {
        for (browser in browsers) {
            key(browser.id) {
                InteractiveBrowserView(
                    browser = browser,
                    browserEffect = browser.browserEffect,
                    streamSize = streamSize,
                    allowsHitTesting = interactiveBrowsers
                )
            }
        }
    }
}

@Composable
private fun safeAreaWindowInsets(): WindowInsets {
    return WindowInsets.systemBars.union(WindowInsets.displayCutout)
}

@Composable
private fun streamViewSafeAreaInsets(sides: WindowInsetsSides, isPortrait: Boolean): StreamViewInsets {
    val density = LocalDensity.current
    val insets = safeAreaWindowInsets().only(sides)
    val leading = insets.getLeft(density, LayoutDirection.Ltr) / density.density
    val top = insets.getTop(density) / density.density
    val trailing = insets.getRight(density, LayoutDirection.Ltr) / density.density
    val bottom = insets.getBottom(density) / density.density
    return if (isPortrait) {
        StreamViewInsets(leading = leading, top = top, trailing = trailing, bottom = 0f)
    } else {
        StreamViewInsets(leading = leading, top = top, trailing = 0f, bottom = bottom)
    }
}

@Composable
private fun streamViewWithWidgets(
    model: Model = LocalModel.current,
    streamView: @Composable () -> Unit,
    safeAreaInsets: StreamViewInsets,
) {
    val stream by model.stream.collectAsState()
    val isPortrait by model.orientation.isPortrait.collectAsState()
    val portraitVideoOffsetFromTop by model.portraitVideoOffsetFromTop.collectAsState()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val dimensions = stream.dimensions()
        val streamLayout = StreamViewLayout(
            metrics = StreamViewMetrics(
                size = Size(maxWidth.value, maxHeight.value),
                safeAreaInsets = safeAreaInsets
            ),
            aspectRatio = dimensions.width.toFloat() / dimensions.height.toFloat(),
            portraitOrientation = isPortrait,
            portraitStream = stream.portrait,
            portraitVideoOffset = if (stream.portrait) 0.0 else portraitVideoOffsetFromTop
        )
        Box(
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(
                    Constraints.fixed(
                        streamLayout.size.width.dp.roundToPx(),
                        streamLayout.size.height.dp.roundToPx()
                    )
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(
                        streamLayout.offset.width.dp.roundToPx(),
                        streamLayout.offset.height.dp.roundToPx()
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { location ->
                                handleTapToFocus(
                                    model = model,
                                    size = Size(size.width.toFloat(), size.height.toFloat()),
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
                size = streamLayout.size
            )
            browserWidgets(model = model, streamSize = streamLayout.size)
        }
    }
}

@Composable
private fun portrait(
    model: Model = LocalModel.current,
    streamView: @Composable () -> Unit,
    orientation: Orientation,
    quickButtons: SettingsQuickButtons,
    safeAreaInsets: StreamViewInsets,
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val showLocalOverlays by model.showLocalOverlays.collectAsState()
    val showDrawOnStream by model.showDrawOnStream.collectAsState()
    val stream by model.stream.collectAsState()
    val photoShootEnabled by model.photoShootEnabled.collectAsState()
    val showBrowser by model.showBrowser.collectAsState()
    val showNavigation by model.showNavigation.collectAsState()
    val showingRemoteControl by model.showingRemoteControl.collectAsState()
    val showingPanel by model.showingPanel.collectAsState()
    val panelHidden by model.panelHidden.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .magnificationGesture(
                    onChanged = { amount -> model.changeZoomX(amount = amount) },
                    onEnded = { amount -> model.commitZoomX(amount = amount) }
                ),
            contentAlignment = Alignment.Center
        ) {
            streamViewWithWidgets(model = model, streamView = streamView, safeAreaInsets = safeAreaInsets)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val width = maxWidth.value
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (isPortrait) 5.dp else 0.dp)
                        .opacityAndHitTesting(showLocalOverlays)
                ) {
                    StreamOverlayView(
                        streamOverlay = model.streamOverlay,
                        chatSettings = model.database.chat,
                        orientation = orientation,
                        width = width
                    )
                }
            }
            if (showDrawOnStream && stream.portrait) {
                DrawOnStreamView(model = model)
            }
            MutedView(audio = model.audio)
            PhotoShootView(enabled = photoShootEnabled)
            if (showBrowser) {
                WebBrowserView(
                    model = model,
                    database = model.database,
                    orientation = orientation,
                    webBrowserState = model.webBrowserState
                )
            }
            if (showNavigation) {
                StreamOverlayNavigationView(
                    model = model,
                    database = model.database,
                    navigation = model.navigation()
                )
            }
            if (showingRemoteControl) {
                ControlBarRemoteControlAssistantView(
                    model = model,
                    remoteControlSettings = model.database.remoteControl
                )
            }
            if (showingPanel != ShowingPanel.none) {
                Box(modifier = Modifier.opacityAndHitTesting(!panelHidden)) {
                    MenuView(model = model)
                }
                val backgroundColor = if (panelHidden) {
                    showingPanel.buttonsBackgroundColor()
                } else {
                    Color.Transparent
                }
                PanelButtonsView(
                    model = model,
                    backgroundColor = backgroundColor,
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .offset(y = (-7).dp)
                )
            }
        }
        ControlBarPortraitView(model = model, quickButtons = quickButtons)
    }
}

@Composable
private fun landscape(
    model: Model = LocalModel.current,
    streamView: @Composable () -> Unit,
    orientation: Orientation,
    quickButtons: SettingsQuickButtons,
    safeAreaInsets: StreamViewInsets,
) {
    val showLocalOverlays by model.showLocalOverlays.collectAsState()
    val showDrawOnStream by model.showDrawOnStream.collectAsState()
    val photoShootEnabled by model.photoShootEnabled.collectAsState()
    val showBrowser by model.showBrowser.collectAsState()
    val showNavigation by model.showNavigation.collectAsState()
    val showingRemoteControl by model.showingRemoteControl.collectAsState()
    val showingPanel by model.showingPanel.collectAsState()
    val panelHidden by model.panelHidden.collectAsState()
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .magnificationGesture(
                    onChanged = { amount -> model.changeZoomX(amount = amount) },
                    onEnded = { amount -> model.commitZoomX(amount = amount) }
                ),
            contentAlignment = Alignment.Center
        ) {
            streamViewWithWidgets(model = model, streamView = streamView, safeAreaInsets = safeAreaInsets)
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val width = maxWidth.value
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .opacityAndHitTesting(showLocalOverlays)
                ) {
                    StreamOverlayView(
                        streamOverlay = model.streamOverlay,
                        chatSettings = model.database.chat,
                        orientation = orientation,
                        width = width
                    )
                }
            }
            if (showDrawOnStream) {
                DrawOnStreamView(model = model)
            }
            MutedView(audio = model.audio)
            PhotoShootView(enabled = photoShootEnabled)
            if (showBrowser) {
                WebBrowserView(
                    model = model,
                    database = model.database,
                    orientation = orientation,
                    webBrowserState = model.webBrowserState
                )
            }
            if (showNavigation) {
                StreamOverlayNavigationView(
                    model = model,
                    database = model.database,
                    navigation = model.navigation()
                )
            }
            if (showingRemoteControl) {
                ControlBarRemoteControlAssistantView(
                    model = model,
                    remoteControlSettings = model.database.remoteControl
                )
            }
            if (showingPanel != ShowingPanel.none && panelHidden) {
                PanelButtonsView(
                    model = model,
                    backgroundColor = showingPanel.buttonsBackgroundColor(),
                    modifier = Modifier.offset(x = 1.dp)
                )
            }
        }
        if (showingPanel != ShowingPanel.none) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (panelHidden) 1.dp else settingsHalfWidth.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Box(modifier = Modifier.opacityAndHitTesting(!panelHidden)) {
                        MenuView(model = model)
                    }
                }
                if (!panelHidden) {
                    PanelButtonsView(model = model, backgroundColor = Color.Transparent)
                }
            }
        }
        ControlBarLandscapeView(model = model, quickButtons = quickButtons)
    }
}

private fun edgesToIgnore(
    isPortrait: Boolean,
    bigButtons: Boolean,
    twoColumns: Boolean,
): WindowInsetsSides {
    return if (isPhone()) {
        if (isPortrait) {
            if (bigButtons && twoColumns) {
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            } else {
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom
            }
        } else if (bigButtons && twoColumns) {
            WindowInsetsSides.Bottom + WindowInsetsSides.Start
        } else {
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        }
    } else {
        WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom
    }
}
