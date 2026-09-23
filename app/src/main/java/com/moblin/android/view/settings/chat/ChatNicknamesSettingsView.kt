package com.moblin.android.view.settings.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.move
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsChatNickname
import com.moblin.android.various.settings.SettingsChatNicknames
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.previewTextToSpeech
import com.moblin.android.various.model.reloadChatMessages

@Composable
private fun NicknameView(
    model: Model = LocalModel.current,
    nickname: SettingsChatNickname,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            NicknameViewDestination(model = model, nickname = nickname)
        },
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableItemPrefixView()
            Text(nickname.user)
            Spacer(modifier = Modifier.weight(1f))
            GrayTextView(text = nickname.nickname)
        }
    }
}

@Composable
fun NicknameViewDestination(
    model: Model = LocalModel.current,
    nickname: SettingsChatNickname,
) {
    Form(title = "Nickname") {
        Section {
            TextEditNavigationView(
                title = "User",
                value = nickname.user,
                onSubmit = { value ->
                    nickname.user = value
                    model.reloadChatMessages()
                },
            )
            TextEditNavigationView(
                title = "Nickname",
                value = nickname.nickname,
                onSubmit = { value ->
                    nickname.nickname = value
                    model.reloadChatMessages()
                },
            )
        }
        Section {
            FormButton(
                title = "Test",
                enabled = nickname.nickname.isNotEmpty(),
            ) {
                model.previewTextToSpeech(nickname.nickname, "This is a test message")
            }
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
    NavigationLink(
        destination = {
            ChatNicknamesSettingsViewDestination(model = model, nicknames = nicknames)
        },
    ) {
        Text(localized("Nicknames"))
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
    model.reloadChatMessages()
}

@Composable
fun ChatNicknamesSettingsViewDestination(
    model: Model = LocalModel.current,
    nicknames: SettingsChatNicknames,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "Nicknames") {
        Section(
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a nickname"))
            },
        ) {
            ForEach(
                nicknames.nicknames,
                id = { it.id },
                onDelete = { offsets ->
                    deleteNickname(model, nicknames, offsets.toList())
                },
                onMove = { froms, to ->
                    nicknames.nicknames.move(fromOffsets = froms, toOffset = to)
                },
            ) { nickname ->
                ContextMenuDeleteButton(
                    action = {
                        val offset = nicknames.nicknames.indexOfFirst { it.id == nickname.id }
                        if (offset >= 0) {
                            deleteNickname(model, nicknames, listOf(offset))
                        }
                    },
                ) {
                    NicknameView(model = model, nickname = nickname)
                }
            }
            CreateButtonView {
                nicknames.nicknames.add(SettingsChatNickname())
            }
        }
    }
}
