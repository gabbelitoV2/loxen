package com.moblin.android.view.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Alert
import com.moblin.android.platform.swiftui.ButtonRole
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.ChatPost
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatNickname
import com.moblin.android.view.controlbar.quickbutton.chat.QuickButtonChatChatterInfoView
import com.moblin.android.various.model.banUser
import com.moblin.android.various.model.copyMessage
import com.moblin.android.various.model.deleteMessage
import com.moblin.android.various.model.reloadChatMessages
import com.moblin.android.various.model.timeoutUser
import com.moblin.android.streamingplatforms.Platform

@Composable
private fun ActionButtonView(
    image: String,
    text: String,
    foreground: Color?,
    enabled: Boolean = true,
    action: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = action,
            ),
    ) {
        if (foreground != null) {
            SystemImage(name = image, fontSize = 22.sp, tint = foreground)
        } else {
            SystemImage(name = image, fontSize = 22.sp)
        }
        Text(
            text = localized(text),
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun banButton(
    model: Model = LocalModel.current,
    selectedPost: ChatPost,
    presentingBanConfirm: Boolean,
    onPresentingBanConfirmChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = formPalette()
    ActionButtonView(image = "nosign", text = "Ban", foreground = palette.red) {
        onPresentingBanConfirmChange(true)
    }
    ConfirmationDialog(
        title = "",
        isPresented = presentingBanConfirm,
        onDismissRequest = { onPresentingBanConfirmChange(false) },
    ) {
        Button("Ban", role = ButtonRole.destructive) {
            model.banUser(post = selectedPost)
            onDismiss()
        }
    }
}

@Composable
private fun timeoutButton(
    model: Model = LocalModel.current,
    selectedPost: ChatPost,
    presentingTimeoutConfirm: Boolean,
    onPresentingTimeoutConfirmChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ActionButtonView(image = "timer", text = "Timeout", foreground = null) {
        onPresentingTimeoutConfirmChange(true)
    }
    ConfirmationDialog(
        title = "",
        isPresented = presentingTimeoutConfirm,
        onDismissRequest = { onPresentingTimeoutConfirmChange(false) },
    ) {
        Button("5 minutes timeout", role = ButtonRole.destructive) {
            model.timeoutUser(post = selectedPost, duration = 300)
            onDismiss()
        }
        Button("1 hour timeout", role = ButtonRole.destructive) {
            model.timeoutUser(post = selectedPost, duration = 3600)
            onDismiss()
        }
        Button("24 hours timeout", role = ButtonRole.destructive) {
            model.timeoutUser(post = selectedPost, duration = 86400)
            onDismiss()
        }
    }
}

@Composable
private fun deleteButton(
    model: Model = LocalModel.current,
    selectedPost: ChatPost,
    presentingDeleteConfirm: Boolean,
    onPresentingDeleteConfirmChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ActionButtonView(image = "trash", text = "Delete", foreground = null) {
        onPresentingDeleteConfirmChange(true)
    }
    ConfirmationDialog(
        title = "",
        isPresented = presentingDeleteConfirm,
        onDismissRequest = { onPresentingDeleteConfirmChange(false) },
    ) {
        Button("Delete message", role = ButtonRole.destructive) {
            model.deleteMessage(post = selectedPost)
            onDismiss()
        }
    }
}

@Composable
private fun copyButton(
    model: Model = LocalModel.current,
    selectedPost: ChatPost,
    onDismiss: () -> Unit,
) {
    ActionButtonView(image = "document.on.document", text = "Copy", foreground = null) {
        model.copyMessage(post = selectedPost)
        onDismiss()
    }
}

@Composable
private fun nicknameButton(
    model: Model = LocalModel.current,
    chat: SettingsChat,
    selectedPost: ChatPost,
    presentingNicknameDialog: Boolean,
    onPresentingNicknameDialogChange: (Boolean) -> Unit,
    nicknameText: String,
    onNicknameTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ActionButtonView(image = "person.badge.plus", text = "Nickname", foreground = null) {
        val user = selectedPost.user
        onNicknameTextChange(user?.let { chat.nicknames.getNickname(user = it) } ?: "")
        onPresentingNicknameDialogChange(true)
    }
    Alert(
        title = "Nickname for ${selectedPost.user ?: ""}",
        isPresented = presentingNicknameDialog,
        onDismissRequest = { onPresentingNicknameDialogChange(false) },
    ) {
        TextField("Nickname", text = nicknameText, onTextChange = onNicknameTextChange)
        Button("Save") {
            saveNickname(
                model = model,
                selectedPost = selectedPost,
                nicknameText = nicknameText,
            )
            onDismiss()
        }
        Button("Cancel", role = ButtonRole.cancel) {
            onDismiss()
        }
    }
}

@Composable
private fun infoButton(
    enabled: Boolean,
    onShowInfo: () -> Unit,
) {
    ActionButtonView(
        image = "info.circle",
        text = "Info",
        foreground = null,
        enabled = enabled,
    ) {
        onShowInfo()
    }
}

private fun saveNickname(
    model: Model,
    selectedPost: ChatPost,
    nicknameText: String,
) {
    val user = selectedPost.user ?: return
    val nickname = nicknameText.trim()
    val chat = model.database.chat
    if (nickname.isEmpty()) {
        chat.nicknames.nicknames.removeAll { it.user == user }
    } else {
        val existingNickname = chat.nicknames.nicknames.firstOrNull { it.user == user }
        if (existingNickname != null) {
            existingNickname.nickname = nickname
        } else {
            val item = SettingsChatNickname()
            item.user = user
            item.nickname = nickname
            chat.nicknames.nicknames.add(item)
        }
    }
    model.reloadChatMessages()
}

@Composable
private fun lineView(
    model: Model = LocalModel.current,
    style: ChatLineStyle,
    selectedPost: ChatPost,
    onLinkUrlChange: (String?) -> Unit,
) {
    val moreThanOneStreamingPlatform by model.chat.moreThanOneStreamingPlatform.collectAsState()
    val deleted by selectedPost.state.deleted.collectAsState()
    val content = style.makeContent(
        post = selectedPost,
        platform = moreThanOneStreamingPlatform,
        deleted = deleted,
    )
    ChatLineView(content = content) { url ->
        if (url != null) {
            onLinkUrlChange(url)
        }
    }
}

@Composable
fun ChatActionButtonsView(
    model: Model = LocalModel.current,
    style: ChatLineStyle,
    selectedPost: ChatPost?,
    onSelectedPostChange: (ChatPost?) -> Unit,
    linkUrl: String?,
    onLinkUrlChange: (String?) -> Unit,
) {
    var presentingBanConfirm by remember { mutableStateOf(false) }
    var presentingTimeoutConfirm by remember { mutableStateOf(false) }
    var presentingDeleteConfirm by remember { mutableStateOf(false) }
    var presentingNicknameDialog by remember { mutableStateOf(false) }
    var nicknameText by remember { mutableStateOf("") }
    var showingChatterInfo by remember { mutableStateOf(false) }

    val palette = formPalette()

    fun dismiss() {
        showingChatterInfo = false
        onSelectedPostChange(null)
    }

    val chat = model.database.chat

    val post = selectedPost
    if (post != null) {
        if (showingChatterInfo) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .border(1.dp, palette.gray),
            ) {
                QuickButtonChatChatterInfoView(
                    model = model,
                    post = post,
                    presenting = showingChatterInfo,
                ) { showingChatterInfo = it }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onSelectedPostChange(null)
                            }
                        },
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(horizontal = 5.dp)
                        .border(1.dp, palette.gray),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .height(100.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        lineView(
                            model = model,
                            style = style,
                            selectedPost = post,
                            onLinkUrlChange = onLinkUrlChange,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 5.dp),
                    ) {
                        Spacer(Modifier.weight(1f))
                        banButton(
                            model = model,
                            selectedPost = post,
                            presentingBanConfirm = presentingBanConfirm,
                            onPresentingBanConfirmChange = { presentingBanConfirm = it },
                            onDismiss = { dismiss() },
                        )
                        Spacer(Modifier.weight(1f))
                        timeoutButton(
                            model = model,
                            selectedPost = post,
                            presentingTimeoutConfirm = presentingTimeoutConfirm,
                            onPresentingTimeoutConfirmChange = { presentingTimeoutConfirm = it },
                            onDismiss = { dismiss() },
                        )
                        Spacer(Modifier.weight(1f))
                        deleteButton(
                            model = model,
                            selectedPost = post,
                            presentingDeleteConfirm = presentingDeleteConfirm,
                            onPresentingDeleteConfirmChange = { presentingDeleteConfirm = it },
                            onDismiss = { dismiss() },
                        )
                        Spacer(Modifier.weight(1f))
                        copyButton(
                            model = model,
                            selectedPost = post,
                            onDismiss = { dismiss() },
                        )
                        Spacer(Modifier.weight(1f))
                        nicknameButton(
                            model = model,
                            chat = chat,
                            selectedPost = post,
                            presentingNicknameDialog = presentingNicknameDialog,
                            onPresentingNicknameDialogChange = { presentingNicknameDialog = it },
                            nicknameText = nicknameText,
                            onNicknameTextChange = { nicknameText = it },
                            onDismiss = { dismiss() },
                        )
                        Spacer(Modifier.weight(1f))
                        infoButton(enabled = post.platform == Platform.kick) {
                            showingChatterInfo = true
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
