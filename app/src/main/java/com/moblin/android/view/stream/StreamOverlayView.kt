package com.moblin.android.view.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.moblin.android.various.utils.isPad
import com.moblin.android.LocalModel

private const val startRadiusFraction = 0.45f

private const val endRadiusFraction = 0.5f

private fun Modifier.opacityAndHitTesting(enabled: Boolean): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        if (enabled) {
            placeable.place(0, 0)
        }
    }
}

@Composable
fun ChatInfo(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(vertical = 5.dp, horizontal = 10.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 17.sp,
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
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StreamOverlayChatView(
                    model = model,
                    database = model.database,
                    chatSettings = chatSettings,
                    chat = chat,
                    chatActivityFeed = chatActivityFeed,
                    fullSize = fullSize
                )
            }
            if (!fullSize) {
                if (!chatPhone) {
                    Spacer(modifier = Modifier.height(85.dp))
                }
            } else {
                HorizontalDivider(color = Color.Gray)
                Spacer(modifier = Modifier.height(controlBarWidthDefault.dp))
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.width(maxWidth * 0.95f)) {
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
        0f to Color.Transparent,
        startRadiusFraction * 2f to Color.Transparent,
        endRadiusFraction * 2f to Color.White
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
    val chatEnabled = chatSettings.enabled
    val leadingPadding = if (isPad() || isPortrait) 15.dp else 0.dp
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isTorchOn && isFrontCameraSelected) {
            FrontTorchView(orientation = orientation)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showingPanel != ShowingPanel.chat) {
                Box(modifier = Modifier.opacityAndHitTesting(chatEnabled)) {
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
                        Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth()
                    },
                    contentAlignment = Alignment.Center
                ) {
                    BannersView(model = model, banners = model.banners)
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    RightOverlayBottomView(
                        database = model.database,
                        show = model.database.show,
                        streamOverlay = model.streamOverlay,
                        zoom = model.zoom,
                        width = width
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.padding(start = leadingPadding)) {
                    LeftOverlayView(model = model, database = model.database)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    RightOverlayTopView(model = model, database = model.database)
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.padding(start = leadingPadding)) {
                    StreamOverlayDebugView(debugOverlay = model.debugOverlay)
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
