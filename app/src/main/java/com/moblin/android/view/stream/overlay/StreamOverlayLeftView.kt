package com.moblin.android.view.stream.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.countFormatter
import com.moblin.android.common.various.smallFont
import com.moblin.android.common.view.StreamOverlayIconAndTextPlacement
import com.moblin.android.common.view.StreamOverlayIconAndTextView
import com.moblin.android.common.view.streamOverlayContentShape
import com.moblin.android.localized
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.systemImage
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.model.Mic
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.PlatformStatus
import com.moblin.android.various.model.StatusTopLeft
import com.moblin.android.various.model.Zoom
import com.moblin.android.various.model.hasChatEmotes
import com.moblin.android.various.model.isChatConfigured
import com.moblin.android.various.model.isChatConnected
import com.moblin.android.various.model.isObsConnected
import com.moblin.android.various.model.isObsRemoteControlConfigured
import com.moblin.android.various.model.isRemoteControlChatAndEvents
import com.moblin.android.various.model.isRemoteControlStreamerConnected
import com.moblin.android.various.model.isShowingStatusChat
import com.moblin.android.various.model.isShowingStatusObs
import com.moblin.android.various.model.isShowingStatusStream
import com.moblin.android.various.model.isShowingStatusZoom
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsShow

private val systemOrange = Color(0xFFFF9500)

private val systemRed = Color(0xFFFF3B30)

@Composable
private fun CollapsedViewersView(status: StatusTopLeft) {
    val numberOfViewersIconColor by status.numberOfViewersIconColor.collectAsState()
    val numberOfViewersCompact by status.numberOfViewersCompact.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier
            .streamOverlayContentShape()
            .clip(RoundedCornerShape(5.dp))
            .background(backgroundColor),
    ) {
        Icon(
            imageVector = systemImage("eye"),
            contentDescription = null,
            tint = numberOfViewersIconColor,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(17.dp),
        )
        Text(
            text = numberOfViewersCompact,
            color = Color.White,
            style = smallFont,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

@Composable
private fun ViewersLogoView(platform: Platform) {
    val imageName = platform.imageName()
    val image = remember(imageName) { Bundle.image(imageName)?.asImageBitmap() }
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .height(18.dp)
                .padding(vertical = 2.dp),
        )
    }
}

@Composable
private fun ViewersView(status: StatusTopLeft) {
    val numberOfViewersIconColor by status.numberOfViewersIconColor.collectAsState()
    val streamingPlatformStatuses by status.streamingPlatformStatuses.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.streamOverlayContentShape(),
    ) {
        Icon(
            imageVector = systemImage("eye"),
            contentDescription = null,
            tint = numberOfViewersIconColor,
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .padding(horizontal = 2.dp)
                .size(17.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .padding(horizontal = 2.dp),
        ) {
            for (platformStatus in streamingPlatformStatuses) {
                ViewersLogoView(platform = platformStatus.platform)
                when (val streamingStatus = platformStatus.status) {
                    is PlatformStatus.live -> Text(
                        text = countFormatter.format(streamingStatus.viewerCount),
                        color = Color.White,
                        style = smallFont,
                    )
                    PlatformStatus.unknown -> Text(
                        text = localized("Unknown"),
                        color = systemOrange,
                        style = smallFont,
                    )
                    PlatformStatus.offline -> Text(
                        text = localized("Offline"),
                        color = systemRed,
                        style = smallFont,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatStatusView(status: StatusTopLeft, foregroundColor: Color) {
    val chatPlatformStatuses by status.chatPlatformStatuses.collectAsState()
    val statusChatText by status.statusChatText.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.streamOverlayContentShape(),
    ) {
        Icon(
            imageVector = systemImage("message"),
            contentDescription = null,
            tint = foregroundColor,
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .padding(horizontal = 2.dp)
                .size(17.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .padding(horizontal = 2.dp),
        ) {
            if (chatPlatformStatuses.isEmpty()) {
                Text(
                    text = statusChatText,
                    color = Color.White,
                    style = smallFont,
                )
            } else {
                for (chatPlatformStatus in chatPlatformStatuses) {
                    ViewersLogoView(platform = chatPlatformStatus.platform)
                    if (chatPlatformStatus.connected) {
                        Text(
                            text = localized("Connected"),
                            color = Color.White,
                            style = smallFont,
                        )
                    } else {
                        Text(
                            text = localized("Disconnected"),
                            color = systemRed,
                            style = smallFont,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamStatusView(status: StatusTopLeft, textPlacement: StreamOverlayIconAndTextPlacement) {
    val streamText by status.streamText.collectAsState()
    StreamOverlayIconAndTextView(
        icon = "dot.radiowaves.left.and.right",
        text = streamText,
        textPlacement = textPlacement,
    )
}

@Composable
private fun ZoomView(zoom: Zoom, textPlacement: StreamOverlayIconAndTextPlacement) {
    val x by zoom.x.collectAsState()
    StreamOverlayIconAndTextView(
        icon = "magnifyingglass",
        text = remember(x) { zoom.statusText() },
        textPlacement = textPlacement,
    )
}

private fun eventsColor(model: Model): Color {
    return if (!model.isEventsConfigured()) {
        Color.White
    } else if (model.isRemoteControlChatAndEvents(platform = null)) {
        if (model.isRemoteControlStreamerConnected()) {
            Color.White
        } else {
            systemRed
        }
    } else if (model.isEventsConnected()) {
        Color.White
    } else {
        systemRed
    }
}

private fun chatColor(model: Model): Color {
    return if (!model.isChatConfigured()) {
        Color.White
    } else if (model.isRemoteControlChatAndEvents(platform = null)) {
        if (model.isRemoteControlStreamerConnected()) {
            Color.White
        } else {
            systemRed
        }
    } else if (model.isChatConnected() && model.hasChatEmotes()) {
        Color.White
    } else {
        systemRed
    }
}

private fun obsStatusColor(model: Model): Color {
    return if (!model.isObsRemoteControlConfigured()) {
        Color.White
    } else if (model.isObsConnected()) {
        Color.White
    } else {
        systemRed
    }
}

@Composable
private fun StatusesView(
    model: Model = LocalModel.current,
    show: SettingsShow,
    status: StatusTopLeft,
    mic: Mic,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val stream by model.stream.collectAsState()
    val isLive by model.isLive.collectAsState()
    val hasZoom by model.zoom.hasZoom.collectAsState()
    val streamingPlatformStatuses by status.streamingPlatformStatuses.collectAsState()
    val statusCameraText by status.statusCameraText.collectAsState()
    val statusObsText by status.statusObsText.collectAsState()
    val statusEventsText by status.statusEventsText.collectAsState()
    val statusChatText by status.statusChatText.collectAsState()
    val currentMic by mic.current.collectAsState()
    key(stream, isLive, hasZoom, streamingPlatformStatuses) {
        if (model.isShowingStatusStream()) {
            StreamStatusView(status = status, textPlacement = textPlacement)
        }
        if (model.isShowingStatusCamera()) {
            StreamOverlayIconAndTextView(
                icon = "camera",
                text = statusCameraText,
                textPlacement = textPlacement,
            )
        }
        if (model.isShowingStatusMic()) {
            StreamOverlayIconAndTextView(
                icon = "music.mic",
                text = currentMic.name,
                textPlacement = textPlacement,
            )
        }
        if (textPlacement != StreamOverlayIconAndTextPlacement.Hide && model.isShowingStatusZoom()) {
            ZoomView(zoom = model.zoom, textPlacement = textPlacement)
        }
        if (model.isShowingStatusObs()) {
            StreamOverlayIconAndTextView(
                icon = "xserve",
                text = statusObsText,
                textPlacement = textPlacement,
                color = obsStatusColor(model),
            )
        }
        if (model.isShowingStatusEvents()) {
            StreamOverlayIconAndTextView(
                icon = "megaphone",
                text = statusEventsText,
                textPlacement = textPlacement,
                color = eventsColor(model),
            )
        }
        if (model.isShowingStatusChat()) {
            if (textPlacement == StreamOverlayIconAndTextPlacement.Hide) {
                StreamOverlayIconAndTextView(
                    icon = "message",
                    text = statusChatText,
                    textPlacement = textPlacement,
                    color = chatColor(model),
                )
            } else {
                ChatStatusView(status = status, foregroundColor = chatColor(model))
            }
        }
        if (model.isShowingStatusViewers()) {
            if (textPlacement == StreamOverlayIconAndTextPlacement.Hide) {
                CollapsedViewersView(status = status)
            } else {
                ViewersView(status = status)
            }
        }
    }
}

@Composable
fun LeftOverlayView(model: Model = LocalModel.current, database: Database) {
    var verboseStatuses by remember(database) { mutableStateOf(database.verboseStatuses) }
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.pointerInput(model, database) {
                detectTapGestures(onTap = {
                    model.toggleVerboseStatuses()
                    verboseStatuses = database.verboseStatuses
                })
            },
        ) {
            if (verboseStatuses) {
                StatusesView(
                    model = model,
                    show = database.show,
                    status = model.statusTopLeft,
                    mic = model.mic,
                    textPlacement = StreamOverlayIconAndTextPlacement.AfterIcon,
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusesView(
                        model = model,
                        show = database.show,
                        status = model.statusTopLeft,
                        mic = model.mic,
                        textPlacement = StreamOverlayIconAndTextPlacement.Hide,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
