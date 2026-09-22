package com.moblin.android.view.stream.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.countFormatter
import com.moblin.android.common.various.smallFont
import com.moblin.android.common.view.StreamOverlayIconAndTextPlacement
import com.moblin.android.common.view.StreamOverlayIconAndTextView
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.various.model.Database
import com.moblin.android.various.model.Mic
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StatusTopLeft
import com.moblin.android.various.model.StreamingPlatformStatus
import com.moblin.android.various.model.Zoom
import com.moblin.android.various.settings.SettingsShow

@Composable
private fun CollapsedViewersView(status: StatusTopLeft) {
    val numberOfViewersIconColor by status.numberOfViewersIconColor.collectAsState()
    val numberOfViewersCompact by status.numberOfViewersCompact.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier = Modifier.background(backgroundColor, RoundedCornerShape(5.dp)),
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = numberOfViewersIconColor,
            modifier = Modifier
                .size(17.dp)
                .padding(start = 2.dp),
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
    val context = LocalContext.current
    val imageName = platform.imageName()
    val resourceId = remember(imageName) {
        context.resources.getIdentifier(imageName, "drawable", context.packageName)
    }
    if (resourceId != 0) {
        Image(
            painter = painterResource(id = resourceId),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(vertical = 2.dp)
                .height(18.dp),
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
    ) {
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = null,
            tint = numberOfViewersIconColor,
            modifier = Modifier
                .size(17.dp)
                .padding(horizontal = 2.dp)
                .background(backgroundColor, RoundedCornerShape(5.dp)),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .background(backgroundColor, RoundedCornerShape(5.dp)),
        ) {
            for (platformStatus in streamingPlatformStatuses) {
                ViewersLogoView(platform = platformStatus.platform)
                when (val streamingStatus = platformStatus.status) {
                    is StreamingPlatformStatus.Status.Live -> Text(
                        text = countFormatter.format(streamingStatus.viewerCount),
                        color = Color.White,
                        style = smallFont,
                    )
                    StreamingPlatformStatus.Status.Unknown -> Text(
                        text = "Unknown",
                        color = Color.Orange,
                        style = smallFont,
                    )
                    StreamingPlatformStatus.Status.Offline -> Text(
                        text = "Offline",
                        color = Color.Red,
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
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            tint = foregroundColor,
            modifier = Modifier
                .size(17.dp)
                .padding(horizontal = 2.dp)
                .background(backgroundColor, RoundedCornerShape(5.dp)),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .background(backgroundColor, RoundedCornerShape(5.dp)),
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
                            text = "Connected",
                            color = Color.White,
                            style = smallFont,
                        )
                    } else {
                        Text(
                            text = "Disconnected",
                            color = Color.Red,
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
    StreamOverlayIconAndTextView(
        icon = "magnifyingglass",
        text = zoom.statusText(),
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
            Color.Red
        }
    } else if (model.isEventsConnected()) {
        Color.White
    } else {
        Color.Red
    }
}

private fun chatColor(model: Model): Color {
    return if (!model.isChatConfigured()) {
        Color.White
    } else if (model.isRemoteControlChatAndEvents(platform = null)) {
        if (model.isRemoteControlStreamerConnected()) {
            Color.White
        } else {
            Color.Red
        }
    } else if (model.isChatConnected() && model.hasChatEmotes()) {
        Color.White
    } else {
        Color.Red
    }
}

private fun obsStatusColor(model: Model): Color {
    return if (!model.isObsRemoteControlConfigured()) {
        Color.White
    } else if (model.isObsConnected()) {
        Color.White
    } else {
        Color.Red
    }
}

@Composable
private fun StatusesView(
    model: Model,
    show: SettingsShow,
    status: StatusTopLeft,
    mic: Mic,
    textPlacement: StreamOverlayIconAndTextPlacement,
) {
    val statusCameraText by status.statusCameraText.collectAsState()
    val statusObsText by status.statusObsText.collectAsState()
    val statusEventsText by status.statusEventsText.collectAsState()
    val statusChatText by status.statusChatText.collectAsState()
    val currentMic by mic.current.collectAsState()

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
    if (textPlacement != StreamOverlayIconAndTextPlacement.hide && model.isShowingStatusZoom()) {
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
        if (textPlacement == StreamOverlayIconAndTextPlacement.hide) {
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
        if (textPlacement == StreamOverlayIconAndTextPlacement.hide) {
            CollapsedViewersView(status = status)
        } else {
            ViewersView(status = status)
        }
    }
}

@Composable
fun LeftOverlayView(model: Model, database: Database) {
    val verboseStatuses by database.verboseStatuses.collectAsState()
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier.clickable {
                model.toggleVerboseStatuses()
            },
        ) {
            if (verboseStatuses) {
                StatusesView(
                    model = model,
                    show = database.show,
                    status = model.statusTopLeft,
                    mic = model.mic,
                    textPlacement = StreamOverlayIconAndTextPlacement.afterIcon,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    StatusesView(
                        model = model,
                        show = database.show,
                        status = model.statusTopLeft,
                        mic = model.mic,
                        textPlacement = StreamOverlayIconAndTextPlacement.hide,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
