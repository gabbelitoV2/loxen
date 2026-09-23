package com.moblin.android.view.settings.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsChatNickname
import com.moblin.android.various.settings.SettingsChatNicknames
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import kotlinx.coroutines.withTimeoutOrNull
import com.moblin.android.various.model.previewTextToSpeech
import com.moblin.android.various.model.reloadChatMessages

@Composable
private fun NicknameView(
    model: Model = LocalModel.current,
    nickname: SettingsChatNickname,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
    onDelete: () -> Unit,
) {
    Box(modifier = Modifier.deleteOnLongPress(onDelete)) {
        NavigationLink(
            destination = {
                NicknameViewDestination(model = model, nickname = nickname)
            },
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
    val nicknameList = nicknames.nicknames
    Form(title = "Nicknames") {
        Section(
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a nickname"))
            },
        ) {
            nicknameList.forEach { nickname ->
                key(nickname.id) {
                    NicknameView(
                        model = model,
                        nickname = nickname,
                        onDelete = {
                            val offset = nicknameList.indexOfFirst { it.id == nickname.id }
                            if (offset >= 0) {
                                deleteNickname(model, nicknames, listOf(offset))
                            }
                        },
                    )
                }
            }
            CreateButtonView {
                nicknames.nicknames.add(SettingsChatNickname())
            }
        }
    }
}

private fun Modifier.deleteOnLongPress(action: () -> Unit): Modifier = pointerInput(action) {
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        val up = withTimeoutOrNull(longPressTimeout) {
            waitForUpOrCancellation()
        }
        if (up == null) {
            action()
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
                if (event.changes.none { it.pressed }) {
                    break
                }
            }
        }
    }
}
