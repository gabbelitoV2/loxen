package com.moblin.android.view.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.controlBarWidthDefault
import com.moblin.android.common.various.stealthModeButtonSize
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Orientation
import com.moblin.android.various.model.StealthMode
import com.moblin.android.various.model.chat.ChatProvider
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.view.controlbar.controlBarWidth
import com.moblin.android.view.stream.ChatOverlayView
import com.moblin.android.view.stream.overlay.RightOverlayTopView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel

private val mainScope = CoroutineScope(Dispatchers.Main)

@Composable
private fun StealthButtonView(
    image: ImageVector,
    text: String,
    action: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(stealthModeButtonSize)
            .clip(CircleShape)
            .background(Color.Black)
            .clickable(onClick = action),
    ) {
        Icon(
            imageVector = image,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(bottom = 2.dp)
                .size(20.dp),
        )
        Text(
            text = localized(text),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
    }
}

@Composable
fun StealthModeView(
    model: Model = LocalModel.current,
    quickButtons: SettingsQuickButtons,
    chat: ChatProvider,
    chatAlerts: ChatProvider,
    stealthMode: StealthMode,
    orientation: Orientation,
) {
    val scope = rememberCoroutineScope()
    val showChat by quickButtons.stealthModeShowChat.collectAsState()
    val showStatus by quickButtons.stealthModeShowStatus.collectAsState()
    val showButtonsState by stealthMode.showButtons.collectAsState()
    val stealthImage by stealthMode.image.collectAsState()
    val isPortrait by orientation.isPortrait.collectAsState()
    var hideButtonsJob by remember { mutableStateOf<Job?>(null) }

    val showButtons: () -> Unit = {
        stealthMode.showButtons.value = true
        hideButtonsJob?.cancel()
        hideButtonsJob = scope.launch {
            delay(3_000)
            stealthMode.showButtons.value = false
        }
    }

    LaunchedEffect(Unit) {
        showButtons()
        model.disableScreenPreview()
    }

    DisposableEffect(Unit) {
        onDispose {
            model.maybeEnableScreenPreview()
            mainScope.launch {
                tryUnpause(model, chat, chatAlerts)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { showButtons() },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Row {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        stealthImage?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (showChat) {
            ChatOverlayView(
                model = model,
                chatSettings = model.database.chat,
                chat = model.chat,
                chatActivityFeed = model.chatActivityFeed,
                orientation = orientation,
                quickButtons = quickButtons,
                show = model.show,
                fullSize = true,
            )
        }
        if (showStatus) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.padding(top = 16.dp, end = 16.dp)) {
                    RightOverlayTopView(model = model, database = model.database)
                }
                if (!isPortrait) {
                    Spacer(modifier = Modifier.width(controlBarWidth(quickButtons)))
                }
            }
        }
        if (showButtonsState) {
            if (isPortrait) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 30.dp)
                            .height(controlBarWidthDefault),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        chatButton(show = showChat) {
                            quickButtons.stealthModeShowChat.value = !showChat
                            showButtons()
                        }
                        statusButton(show = showStatus) {
                            quickButtons.stealthModeShowStatus.value = !showStatus
                            showButtons()
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        returnButton {
                            model.toggleStealthMode()
                        }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier
                            .padding(top = 30.dp, bottom = 5.dp)
                            .width(controlBarWidth(quickButtons)),
                    ) {
                        chatButton(show = showChat) {
                            quickButtons.stealthModeShowChat.value = !showChat
                            showButtons()
                        }
                        statusButton(show = showStatus) {
                            quickButtons.stealthModeShowStatus.value = !showStatus
                            showButtons()
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        returnButton {
                            model.toggleStealthMode()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusButton(show: Boolean, onClick: () -> Unit) {
    StealthButtonView(
        image = if (show) Icons.Filled.BarChart else Icons.Outlined.BarChart,
        text = "Status",
        action = onClick,
    )
}

@Composable
private fun chatButton(show: Boolean, onClick: () -> Unit) {
    StealthButtonView(
        image = if (show) Icons.Filled.Message else Icons.Outlined.Message,
        text = "Chat",
        action = onClick,
    )
}

@Composable
private fun returnButton(onClick: () -> Unit) {
    StealthButtonView(
        image = Icons.Default.Reply,
        text = "Return",
        action = onClick,
    )
}

private fun tryUnpause(model: Model, chat: ChatProvider) {
    if (!chat.interactiveChat.value) {
        return
    }
    if (chat.paused.value) {
        model.endOfChatReachedWhenPaused(chat)
        chat.triggerScrollToBottom.value = !chat.triggerScrollToBottom.value
    }
}

private fun tryUnpause(model: Model, chat: ChatProvider, chatAlerts: ChatProvider) {
    tryUnpause(model, chat)
    tryUnpause(model, chatAlerts)
}
