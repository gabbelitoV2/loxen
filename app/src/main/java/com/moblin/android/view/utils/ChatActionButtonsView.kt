package com.moblin.android.view.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.ChatPost
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsChat
import com.moblin.android.various.settings.SettingsChatNickname
import com.moblin.android.view.controlbar.quickbutton.chat.QuickButtonChatChatterInfoView
import com.moblin.android.LocalModel

@Composable
private fun ActionButtonView(
    image: ImageVector,
    text: String,
    foreground: Color?,
    enabled: Boolean = true,
    action: () -> Unit,
) {
    Button(onClick = action, enabled = enabled) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (foreground != null) {
                Icon(
                    imageVector = image,
                    contentDescription = null,
                    tint = foreground,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Icon(
                    imageVector = image,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = localized(text),
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
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
    ActionButtonView(image = Icons.Default.Block, text = "Ban", foreground = Color.Red) {
        onPresentingBanConfirmChange(true)
    }
    if (presentingBanConfirm) {
        AlertDialog(
            onDismissRequest = { onPresentingBanConfirmChange(false) },
            title = null,
            text = null,
            confirmButton = {
                TextButton(onClick = {
                    TODO("banUser is not implemented on Android")
                    onDismiss()
                }) {
                    Text(localized("Ban"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onPresentingBanConfirmChange(false) }) {
                    Text(localized("Cancel"))
                }
            },
        )
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
    ActionButtonView(image = Icons.Default.Timer, text = "Timeout", foreground = null) {
        onPresentingTimeoutConfirmChange(true)
    }
    if (presentingTimeoutConfirm) {
        AlertDialog(
            onDismissRequest = { onPresentingTimeoutConfirmChange(false) },
            title = null,
            text = null,
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        TODO("timeoutUser is not implemented on Android")
                        onDismiss()
                    }) {
                        Text(localized("5 minutes timeout"), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = {
                        TODO("timeoutUser is not implemented on Android")
                        onDismiss()
                    }) {
                        Text(localized("1 hour timeout"), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = {
                        TODO("timeoutUser is not implemented on Android")
                        onDismiss()
                    }) {
                        Text(localized("24 hours timeout"), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { onPresentingTimeoutConfirmChange(false) }) {
                    Text(localized("Cancel"))
                }
            },
        )
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
    ActionButtonView(image = Icons.Default.Delete, text = "Delete", foreground = null) {
        onPresentingDeleteConfirmChange(true)
    }
    if (presentingDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { onPresentingDeleteConfirmChange(false) },
            title = null,
            text = null,
            confirmButton = {
                TextButton(onClick = {
                    TODO("deleteMessage is not implemented on Android")
                    onDismiss()
                }) {
                    Text(localized("Delete message"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onPresentingDeleteConfirmChange(false) }) {
                    Text(localized("Cancel"))
                }
            },
        )
    }
}

@Composable
private fun copyButton(
    model: Model = LocalModel.current,
    selectedPost: ChatPost,
    onDismiss: () -> Unit,
) {
    ActionButtonView(image = Icons.Default.ContentCopy, text = "Copy", foreground = null) {
        TODO("copyMessage is not implemented on Android")
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
    ActionButtonView(image = Icons.Default.PersonAdd, text = "Nickname", foreground = null) {
        val user = selectedPost.user
        onNicknameTextChange(user?.let { chat.nicknames.getNickname(user = it) } ?: "")
        onPresentingNicknameDialogChange(true)
    }
    if (presentingNicknameDialog) {
        AlertDialog(
            onDismissRequest = { onPresentingNicknameDialogChange(false) },
            title = { Text("Nickname for ${selectedPost.user ?: ""}") },
            text = {
                OutlinedTextField(
                    value = nicknameText,
                    onValueChange = onNicknameTextChange,
                    label = { Text(localized("Nickname")) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    saveNickname(model = model, selectedPost = selectedPost, nicknameText = nicknameText)
                    onDismiss()
                }) {
                    Text(localized("Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(localized("Cancel"))
                }
            },
        )
    }
}

@Composable
private fun infoButton(
    enabled: Boolean,
    onShowInfo: () -> Unit,
) {
    ActionButtonView(
        image = Icons.Default.Info,
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
    TODO("reloadChatMessages is not implemented on Android")
}

@Composable
private fun lineView(
    model: Model = LocalModel.current,
    style: ChatLineStyle,
    selectedPost: ChatPost,
    onLinkUrlChange: (String?) -> Unit,
) {
    val content = style.makeContent(
        post = selectedPost,
        platform = model.chat.moreThanOneStreamingPlatform.value,
        deleted = selectedPost.state.deleted.value,
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
                    .border(1.dp, Color.Gray)
            ) {
                QuickButtonChatChatterInfoView(
                    model = model,
                    post = post,
                    presenting = showingChatterInfo,
                ) { showingChatterInfo = it }
            }
        } else {
            Column {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clickable { onSelectedPostChange(null) }
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(horizontal = 5.dp)
                        .border(1.dp, Color.Gray)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 5.dp)
                            .height(100.dp)
                            .verticalScroll(rememberScrollState())
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
                        modifier = Modifier.padding(bottom = 5.dp)
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
                        infoButton(enabled = post.platform?.name == "kick") {
                            showingChatterInfo = true
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
