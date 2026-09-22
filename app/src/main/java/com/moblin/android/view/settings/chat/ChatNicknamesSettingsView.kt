package com.moblin.android.view.settings.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsChatNickname
import com.moblin.android.various.settings.SettingsChatNicknames
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NicknameView(
    model: Model = LocalModel.current,
    nickname: SettingsChatNickname,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onNavigate("Nickname") },
                onLongClick = onDelete,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DraggableItemPrefixView()
        Text(nickname.user)
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = nickname.nickname)
    }
}

@Composable
fun NicknameViewDestination(
    model: Model = LocalModel.current,
    nickname: SettingsChatNickname,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextEditNavigationView(
            title = "User",
            value = nickname.user,
            onSubmit = { value ->
                nickname.user = value
                TODO("Model.reloadChatMessages is not available")
            },
        )
        TextEditNavigationView(
            title = "Nickname",
            value = nickname.nickname,
            onSubmit = { value ->
                nickname.nickname = value
                TODO("Model.reloadChatMessages is not available")
            },
        )
        TextButtonView("Test") {
            TODO("Model.previewTextToSpeech is not available")
        }
    }
}

@Composable
fun ChatNicknamesSettingsView(
    model: Model = LocalModel.current,
    nicknames: SettingsChatNicknames,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val nicknameList = nicknames.nicknames
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("Nicknames") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Nicknames")
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = nicknameList.size.toString())
    }
}

private fun deleteNickname(
    model: Model,
    nicknames: SettingsChatNicknames,
    offsets: List<Int>,
) {
    val removed = offsets.toSet()
    val kept = nicknames.nicknames.filterIndexed { index, _ ->
        index !in removed
    }
    nicknames.nicknames.clear()
    nicknames.nicknames.addAll(kept)
    TODO("Model.reloadChatMessages is not available")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatNicknamesSettingsViewDestination(
    model: Model = LocalModel.current,
    nicknames: SettingsChatNicknames,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val nicknameList = nicknames.nicknames
    val onMove: (List<Int>, Int) -> Unit = { _, _ ->
        TODO("SwiftUI List.onMove drag to reorder has no Compose counterpart")
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(nicknameList, key = { _, item -> item.id.toString() }) { index, nickname ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        deleteNickname(model, nicknames, listOf(index))
                        true
                    } else {
                        false
                    }
                },
            )
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
                enableDismissFromStartToEnd = false,
            ) {
                NicknameView(
                    model = model,
                    nickname = nickname,
                    onNavigate = onNavigate,
                    onDelete = {
                        val offset = nicknameList.indexOfFirst { it.id == nickname.id }
                        if (offset >= 0) {
                            deleteNickname(model, nicknames, listOf(offset))
                        }
                    },
                )
            }
        }
        item {
            SwipeLeftToDeleteHelpView(kind = localized("a nickname"))
        }
        item {
            CreateButtonView {
                nicknames.nicknames.add(SettingsChatNickname())
            }
        }
    }
}
