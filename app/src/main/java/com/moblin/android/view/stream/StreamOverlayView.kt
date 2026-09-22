package com.moblin.android.view.stream

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.controlBarWidthDefault
import com.moblin.android.view.controlbar.controlBarWidth
import com.moblin.android.view.stream.overlay.LeftOverlayView
import com.moblin.android.view.stream.overlay.RightOverlayBottomView
import com.moblin.android.view.stream.overlay.RightOverlayTopView
import com.moblin.android.view.stream.overlay.StreamOverlayChatView
import com.moblin.android.view.stream.overlay.StreamOverlayDebugView
import com.moblin.android.view.utils.BannersView
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.Show
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.StreamOverlay
import com.moblin.android.various.model.chat.ChatProvider
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.LocalModel

private const val startRadiusFraction = 0.45f

private const val endRadiusFraction = 0.5f

private fun Modifier.allowsHitTesting(allowed: Boolean): Modifier = if (allowed) {
    this
} else {
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }
}

@Composable
fun ChatInfo(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .allowsHitTesting(false)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Row {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(vertical = 5.dp, horizontal = 10.dp)
            ) {
                Text(
                    text = message,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ChatOverlayView(
    model: Model = LocalModel.current,
    chatSettings: SettingsChat,
    chat: ChatProvider,
    chatActivityFeed: ChatProvider,
    orientation: Orientation,
    quickButtons: SettingsQuickButtons,
    show: Show,
    fullSize: Boolean
) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val chatPhone by show.chatPhone.collectAsState()
    if (isPortrait) {
        Column {
            StreamOverlayChatView(
                model = model,
                database = model.database,
                chatSettings = chatSettings,
                chat = chat,
                chatActivityFeed = chatActivityFeed,
                fullSize = fullSize
            )
            if (!fullSize) {
                if (!chatPhone) {
                    Spacer(modifier = Modifier.height(85.dp))
                }
            } else {
                HorizontalDivider(
                    modifier = Modifier.allowsHitTesting(false),
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(controlBarWidthDefault.dp))
            }
        }
    } else {
        Row {
            Column {
                Row {
                    Box(modifier = Modifier.fillMaxWidth(0.95f)) {
                        StreamOverlayChatView(
                            model = model,
                            database = model.database,
                            chatSettings = chatSettings,
                            chat = chat,
                            chatActivityFeed = chatActivityFeed,
                            fullSize = fullSize
                        )
                    }
                }
                if (!fullSize && !chatPhone) {
                    Spacer(modifier = Modifier.height(chatSettings.bottomPoints.dp))
                }
            }
            if (fullSize) {
                VerticalDivider(color = Color.Gray)
                Spacer(modifier = Modifier.width(controlBarWidth(quickButtons = quickButtons).dp))
            }
        }
    }
}

@Composable
private fun FrontTorchView(orientation: Orientation) {
    val isPortrait by orientation.isPortrait.collectAsState()
    val gradient = Brush.radialGradient(
        colorStops = arrayOf(
            0f to Color.Transparent,
            startRadiusFraction to Color.Transparent,
            endRadiusFraction to Color.White,
            1f to Color.White
        )
    )
    if (isPortrait) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(gradient)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .background(gradient)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun leadingPadding(orientation: Orientation): Dp {
    val isPortrait by orientation.isPortrait.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet =
        (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >=
            Configuration.SCREENLAYOUT_SIZE_LARGE
    return if (isTablet || isPortrait) {
        15.dp
    } else {
        0.dp
    }
}

@Composable
fun StreamOverlayView(
    model: Model = LocalModel.current,
    streamOverlay: StreamOverlay,
    chatSettings: SettingsChat,
    orientation: Orientation,
    width: Float
) {
    val isTorchOn by streamOverlay.isTorchOn.collectAsState()
    val isFrontCameraSelected by streamOverlay.isFrontCameraSelected.collectAsState()
    val showingPanel by model.showingPanel.collectAsState()
    val isPortrait by orientation.isPortrait.collectAsState()
    val chatEnabled by chatSettings.enabled.collectAsState()
    Box {
        if (isTorchOn && isFrontCameraSelected) {
            FrontTorchView(orientation = orientation)
        }
        Box(modifier = Modifier.padding(top = 10.dp)) {
            if (showingPanel != ShowingPanel.chat) {
                Box(
                    modifier = Modifier
                        .alpha(if (chatEnabled) 1f else 0f)
                        .allowsHitTesting(chatEnabled)
                ) {
                    ChatOverlayView(
                        model = model,
                        chatSettings = chatSettings,
                        chat = model.chat,
                        chatActivityFeed = model.chatActivityFeed,
                        orientation = orientation,
                        quickButtons = model.database.quickButtonsGeneral,
                        show = model.show,
                        fullSize = false
                    )
                }
                Box(
                    modifier = if (isPortrait) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.width(400.dp)
                    }
                ) {
                    BannersView(model = model, banners = model.banners)
                }
            }
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.padding(end = 10.dp)) {
                    RightOverlayBottomView(
                        database = model.database,
                        show = model.database.show,
                        streamOverlay = model.streamOverlay,
                        zoom = model.zoom,
                        width = width
                    )
                }
            }
            Row {
                Box(modifier = Modifier.padding(start = leadingPadding(orientation))) {
                    LeftOverlayView(model = model, database = model.database)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.padding(end = 10.dp)) {
                    RightOverlayTopView(model = model, database = model.database)
                }
            }
            Row(modifier = Modifier.allowsHitTesting(false)) {
                Box(modifier = Modifier.padding(start = leadingPadding(orientation))) {
                    StreamOverlayDebugView(debugOverlay = model.debugOverlay)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
