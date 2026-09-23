package com.moblin.android.view.settings.mediaplayer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.move
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayerFile
import com.moblin.android.various.settings.SettingsMediaPlayers
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.NameEditView
import java.io.File
import com.moblin.android.various.model.updateMediaPlayerSettings

private fun appendMedia(model: Model, player: SettingsMediaPlayer, url: String) {
    val file = SettingsMediaPlayerFile()
    model.mediaStorage.add(id = file.id, url = File(url))
    player.playlist.add(file)
    model.updateMediaPlayerSettings(playerId = player.id, settings = player)
}

private fun deletePlaylistFile(model: Model, player: SettingsMediaPlayer, offsets: List<Int>) {
    offsets.sortedDescending().forEach { index ->
        player.playlist.removeAt(index)
    }
    model.updateMediaPlayerSettings(playerId = player.id, settings = player)
}

@Composable
fun MediaPlayerSettingsView(
    model: Model = LocalModel.current,
    mediaPlayers: SettingsMediaPlayers,
    player: SettingsMediaPlayer,
    onNavigate: (String) -> Unit = {},
) {
    val presentingPicker = remember { mutableStateOf(false) }
    val selectedVideoItem = remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        selectedVideoItem.value = uri
    }

    LaunchedEffect(presentingPicker.value) {
        if (presentingPicker.value) {
            picker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
            )
            presentingPicker.value = false
        }
    }

    LaunchedEffect(selectedVideoItem.value) {
        val videoItem = selectedVideoItem.value
        if (videoItem != null) {
            appendMedia(model = model, player = player, url = videoItem.toString())
            selectedVideoItem.value = null
        }
    }

    NavigationLink(
        destination = {
            Form(title = "Media player") {
                Section {
                    NameEditView(
                        name = player.name,
                        existingNames = mediaPlayers.players,
                        onNameChange = { name ->
                            player.name = name
                            model.updateMediaPlayerSettings(
                                playerId = player.id,
                                settings = player,
                            )
                        },
                    )
                }
                if (false) {
                    Section {
                        Toggle(
                            title = "Auto select mic",
                            isOn = player.autoSelectMic,
                            onChange = { autoSelectMic ->
                                player.autoSelectMic = autoSelectMic
                            },
                        )
                    }
                }
                Section(header = "Playlist") {
                    ForEach(
                        player.playlist,
                        id = { it.id },
                        onDelete = { deletePlaylistFile(model = model, player = player, offsets = it.toList()) },
                        onMove = { froms, to ->
                            player.playlist.move(fromOffsets = froms, toOffset = to)
                            model.updateMediaPlayerSettings(playerId = player.id, settings = player)
                        },
                    ) { file ->
                        ContextMenuDeleteButton(action = {
                            val offset = player.playlist.indexOfFirst { it.id == file.id }
                            if (offset != -1) {
                                deletePlaylistFile(model = model, player = player, offsets = listOf(offset))
                            }
                        }) {
                            MediaPlayerFileSettingsView(player = player, file = file)
                        }
                    }
                    FormButton(
                        title = "Add",
                        centered = true,
                        enabled = selectedVideoItem.value == null,
                    ) {
                        presentingPicker.value = true
                    }
                }
            }
        },
    ) {
        Text(player.name)
        Spacer(Modifier.weight(1f))
    }
}
