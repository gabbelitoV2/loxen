package com.moblin.android.view.settings.applemusic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Label
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.monospacedDigit
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.MusicPlaylist
import com.moblin.android.various.model.MusicPlaylistSong
import com.moblin.android.various.model.MusicQueueState
import com.moblin.android.localized
import com.moblin.android.various.model.addMusic
import com.moblin.android.various.model.loadMusic
import com.moblin.android.various.model.nextMusic
import com.moblin.android.various.model.pauseMusic
import com.moblin.android.various.model.playMusic
import com.moblin.android.various.model.previousMusic
import com.moblin.android.various.model.removeMusic
import com.moblin.android.various.model.selectMusic

private data class MusicPlayerState(val isPlaying: Boolean)

private data class MusicQueueEntry(val id: String, val title: String)

private data class MusicPlayerQueue(
    val entries: List<MusicQueueEntry>,
    val currentEntryId: String?,
)

private data class MusicSubscriptionState(val canBecomeSubscriber: Boolean)

@Composable
fun MusicArtistView(artist: String) {
    Text(
        artist,
        style = SwiftUIFonts.system(size = 15f).copy(color = formPalette().secondaryLabel),
    )
}

fun formatMusicDuration(duration: Int): String {
    val seconds = (duration % 60).toString().padStart(2, '0')
    if (duration < 3600) {
        return "${duration / 60}:$seconds"
    }
    val minutes = (duration / 60 % 60).toString().padStart(2, '0')
    return "${duration / 3600}:$minutes:$seconds"
}

@Composable
fun MusicDurationView(duration: Int) {
    Text(
        formatMusicDuration(duration),
        style = SwiftUIFonts.system(size = 15f).copy(color = formPalette().secondaryLabel).monospacedDigit(),
    )
}

@Composable
private fun PlaylistEndView(
    musicPlaylist: MusicPlaylist,
    queue: MusicPlayerQueue,
) {
    Unit
}

@Composable
fun MusicAddedByView(user: String) {
    Text(
        localized("Added by: $user"),
        style = SwiftUIFonts.system(size = 15f),
    )
}

@Composable
private fun ControlsView(
    model: Model = LocalModel.current,
    playerState: MusicPlayerState,
    isEmpty: Boolean,
) {
    Section {
        FormRow(enabled = !isEmpty, highlight = false) {
            Spacer(Modifier.weight(1f))
            MusicControlButton(name = "arrow.backward.to.line", enabled = !isEmpty) {
                model.previousMusic(count = 1)
            }
            Spacer(Modifier.weight(1f))
            if (playerState.isPlaying) {
                MusicControlButton(name = "pause", enabled = !isEmpty) {
                    model.pauseMusic()
                }
            } else {
                MusicControlButton(name = "play", enabled = !isEmpty) {
                    model.playMusic()
                }
            }
            Spacer(Modifier.weight(1f))
            MusicControlButton(name = "arrow.forward.to.line", enabled = !isEmpty) {
                model.nextMusic(count = 1)
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlaylistView(
    model: Model = LocalModel.current,
    musicPlaylist: MusicPlaylist,
    queue: MusicPlayerQueue,
) {
    val songs = musicPlaylist.songs()
    val currentIndex = musicPlaylist.currentIndex()
    Section(headerContent = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("Playlist"))
            Spacer(Modifier.weight(1f))
            PlaylistEndView(musicPlaylist = musicPlaylist, queue = queue)
        }
    }) {
        ForEach(
            items = songs.withIndex().toList(),
            id = { it.value.id },
            onDelete = { offsets ->
                for (offset in offsets) {
                    model.removeMusic(entryId = songs[offset].id)
                }
            },
        ) { indexed ->
            val song = indexed.value
            val isPlaying = indexed.index == currentIndex
            FormRow(onClick = { model.selectMusic(entryId = song.id) }, highlight = false) {
                if (isPlaying) {
                    SystemImage(
                        name = "music.quarternote.3",
                        fontSize = 17.sp,
                        tint = formPalette().label,
                    )
                }
                Column(horizontalAlignment = Alignment.Start) {
                    if (isPlaying) {
                        Text(
                            song.title,
                            style = formBodyStyle,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Text(song.title, style = formBodyStyle)
                    }
                    song.artist?.let { MusicArtistView(artist = it) }
                    song.addedBy?.let { MusicAddedByView(user = it) }
                }
                Spacer(Modifier.weight(1f))
                song.duration?.let { MusicDurationView(duration = it) }
            }
        }
    }
}

@Composable
fun AppleMusicSettingsView(
    model: Model = LocalModel.current,
    musicPlaylist: MusicPlaylist,
) {
    var searchText by remember { mutableStateOf("") }
    var isShowingSubscriptionOffer by remember { mutableStateOf(false) }
    var musicSubscription by remember { mutableStateOf<MusicSubscriptionState?>(null) }
    val playerState = rememberMusicPlayerState()
    val playerQueue = rememberMusicPlayerQueue()
    val playlistState by musicPlaylist.state.collectAsState()

    Form(title = "Apple Music") {
        if (musicSubscription?.canBecomeSubscriber == true) {
            Section {
                FormRow(onClick = { isShowingSubscriptionOffer = true }) {
                    Label("Join", systemImage = "applelogo")
                }
            }
        }
        ControlsView(
            model = model,
            playerState = playerState,
            isEmpty = playlistState != MusicQueueState.loaded,
        )
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
                        model.addMusic(title = searchText, user = null) { }
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
        PlaylistView(
            model = model,
            musicPlaylist = musicPlaylist,
            queue = playerQueue,
        )
    }
    DisposableEffect(Unit) {
        model.loadMusic()
        onDispose { }
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

private val MusicPlaylistSong.duration: Int?
    get() = null
