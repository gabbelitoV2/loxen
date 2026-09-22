package com.moblin.android.view.settings.mediaplayer

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayerFile
import com.moblin.android.various.settings.SettingsMediaPlayers
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.LocalModel
import java.io.File

data class Video(val url: String) {
    companion object {
        val transferRepresentation: Any
            get() = TODO("no Android counterpart for Transferable")
    }
}

private fun appendMedia(model: Model, player: SettingsMediaPlayer, url: String) {
    val file = SettingsMediaPlayerFile()
    model.mediaStorage.add(id = file.id, url = File(url))
    player.playlist.add(file)
    TODO("updateMediaPlayerSettings")
}

private fun deletePlaylistFile(model: Model, player: SettingsMediaPlayer, offsets: List<Int>) {
    offsets.sortedDescending().forEach { index ->
        player.playlist.removeAt(index)
    }
    TODO("updateMediaPlayerSettings")
}

private suspend fun loadTransferableVideo(item: Uri): Result<Video?> =
    TODO("PhotosPickerItem.loadTransferable has no Android counterpart")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerSettingsView(
    model: Model = LocalModel.current,
    mediaPlayers: SettingsMediaPlayers,
    player: SettingsMediaPlayer,
    onNavigate: (String) -> Unit = {},
) {
    val presentingPicker = remember { mutableStateOf(false) }
    val selectedVideoItem = remember { mutableStateOf<Uri?>(null) }
    val onContextMenuDelete: (SettingsMediaPlayerFile) -> Unit = { file ->
        val index = player.playlist.indexOfFirst { it.id == file.id }
        if (index != -1) {
            deletePlaylistFile(model = model, player = player, offsets = listOf(index))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Media player") })
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            item {
                NameEditView(
                    name = player.name,
                    existingNames = mediaPlayers.players,
                    onNameChange = { name ->
                        player.name = name
                        TODO("updateMediaPlayerSettings")
                    },
                )
            }
            if (false) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto select mic")
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = player.autoSelectMic,
                            onCheckedChange = { autoSelectMic ->
                                player.autoSelectMic = autoSelectMic
                            },
                        )
                    }
                }
            }
            item {
                Text("Playlist", style = MaterialTheme.typography.titleMedium)
            }
            items(player.playlist, key = { file -> file.id.toString() }) { file ->
                MediaPlayerFileSettingsView(player = player, file = file)
                TODO("contextMenuDeleteButton has no Compose counterpart")
            }
            item {
                TODO("List.onMove has no Compose counterpart")
                TODO("List.onDelete has no Compose counterpart")
            }
            item {
                Button(
                    onClick = { presentingPicker.value = true },
                    enabled = selectedVideoItem.value == null,
                ) {
                    HCenter {
                        if (selectedVideoItem.value != null) {
                            CircularProgressIndicator()
                        } else {
                            Text("Add")
                        }
                    }
                }
                TODO("photosPicker has no Compose counterpart")
            }
        }
    }

    LaunchedEffect(selectedVideoItem.value) {
        val videoItem = selectedVideoItem.value
        if (videoItem != null) {
            loadTransferableVideo(videoItem)
                .onSuccess { video ->
                    if (video != null) {
                        appendMedia(model = model, player = player, url = video.url)
                    } else {
                        Log.i("MediaPlayerSettingsView", "media-player: Media is nil")
                    }
                }
                .onFailure { error ->
                    Log.i("MediaPlayerSettingsView", "media-player: Media error: $error")
                }
            selectedVideoItem.value = null
        }
    }
}
