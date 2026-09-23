package com.moblin.android.view.settings.applemusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.localized
import com.moblin.android.various.model.addMusic
import com.moblin.android.various.model.nextMusic
import com.moblin.android.various.model.pauseMusic
import com.moblin.android.various.model.playMusic
import com.moblin.android.various.model.previousMusic

private data class MusicPlayerState(val isPlaying: Boolean)

private data class MusicQueueEntry(val id: String, val title: String)

private data class MusicPlayerQueue(
    val entries: List<MusicQueueEntry>,
    val currentEntryId: String?,
)

private data class MusicSubscriptionState(val canBecomeSubscriber: Boolean)

@Composable
fun AppleMusicSettingsView(model: Model = LocalModel.current) {
    var searchText by remember { mutableStateOf("") }
    var isShowingSubscriptionOffer by remember { mutableStateOf(false) }
    var musicSubscription by remember { mutableStateOf<MusicSubscriptionState?>(null) }
    val playerState = rememberMusicPlayerState()
    val playerQueue = rememberMusicPlayerQueue()

    Form(title = "Apple Music") {
        if (musicSubscription?.canBecomeSubscriber == true) {
            Section {
                FormRow(onClick = { isShowingSubscriptionOffer = true }) {
                    Label("Join", systemImage = "applelogo")
                }
            }
        }
        Section {
            val queueEnabled = playerQueue.entries.isNotEmpty()
            FormRow(enabled = queueEnabled, highlight = false) {
                Spacer(Modifier.weight(1f))
                MusicControlButton(
                    name = "arrow.backward.to.line",
                    enabled = queueEnabled,
                ) {
                    model.previousMusic(1)
                }
                Spacer(Modifier.weight(1f))
                if (playerState.isPlaying) {
                    MusicControlButton(name = "pause", enabled = queueEnabled) {
                        model.pauseMusic()
                    }
                } else {
                    MusicControlButton(name = "play", enabled = queueEnabled) {
                        model.playMusic()
                    }
                }
                Spacer(Modifier.weight(1f))
                MusicControlButton(
                    name = "arrow.forward.to.line",
                    enabled = queueEnabled,
                ) {
                    model.nextMusic(1)
                }
                Spacer(Modifier.weight(1f))
            }
        }
        Section {
            FormRow(highlight = false) {
                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = formBodyStyle.copy(color = formPalette().label),
                    singleLine = true,
                    cursorBrush = SolidColor(formPalette().accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        model.addMusic(searchText) { }
                    }),
                    decorationBox = { content ->
                        Box {
                            if (searchText.isEmpty()) {
                                Text(
                                    localized("Add song"),
                                    style = formBodyStyle,
                                    color = formPalette().secondaryLabel,
                                )
                            }
                            content()
                        }
                    },
                )
            }
        }
        Section(header = "Playlist") {
            for (entry in playerQueue.entries) {
                key(entry.id) {
                    if (entry.id == playerQueue.currentEntryId) {
                        Text(
                            "• ${entry.title}",
                            style = formBodyStyle,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(entry.title, style = formBodyStyle)
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicControlButton(
    name: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SystemImage(
            name = name,
            fontSize = 28.sp,
            modifier = Modifier.alpha(
                when {
                    pressed -> 0.2f
                    !enabled -> 0.35f
                    else -> 1f
                },
            ),
            tint = formPalette().accent,
        )
    }
}

@Composable
private fun rememberMusicPlayerState(): MusicPlayerState =
    remember { MusicPlayerState(isPlaying = false) }

@Composable
private fun rememberMusicPlayerQueue(): MusicPlayerQueue =
    remember { MusicPlayerQueue(entries = emptyList(), currentEntryId = null) }
