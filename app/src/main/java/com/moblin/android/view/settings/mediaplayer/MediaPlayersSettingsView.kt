package com.moblin.android.view.settings.mediaplayer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayers
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.LocalModel

@Composable
fun MediaPlayersSettingsView(
    model: Model = LocalModel.current,
    mediaPlayers: SettingsMediaPlayers,
) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Text(
                localized(
                    "Use a media player as video source in scenes and as mic to stream recordings " +
                        "or other MP4-files."
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item {
            Text(
                localized("⚠️ Audio is not yet fully supported, but might work."),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        itemsIndexed(
            items = mediaPlayers.players,
            key = { _, player -> player.id },
        ) { index, player ->
            Row(modifier = Modifier.fillMaxWidth()) {
                MediaPlayerSettingsView(mediaPlayers = mediaPlayers, player = player)
                IconButton(
                    onClick = {
                        makeOffsets(mediaPlayers.players, player.id)?.let { offsets ->
                            deletePlayer(model, mediaPlayers, offsets)
                        }
                    },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
        item {
            CreateButtonView {
                val mediaPlayer = SettingsMediaPlayer()
                mediaPlayer.name = makeUniqueName(
                    SettingsMediaPlayer.baseName,
                    mediaPlayers.players,
                )
                mediaPlayers.players.add(mediaPlayer)
                model.addMediaPlayer(settings = mediaPlayer)
            }
        }
    }
}

private fun deletePlayer(
    model: Model,
    mediaPlayers: SettingsMediaPlayers,
    offsets: List<Int>,
) {
    val players = mediaPlayers.players
    for (index in offsets) {
        model.deleteMediaPlayer(playerId = players[index].id)
    }
    for (index in offsets.sortedDescending()) {
        players.removeAt(index)
    }
}
