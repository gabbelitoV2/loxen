package com.moblin.android.view.settings.mediaplayer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.addMediaPlayer
import com.moblin.android.various.model.deleteMediaPlayer
import com.moblin.android.various.model.updateMediaPlayerVideoSourcesAndMics
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayers
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.LocalModel

@Composable
fun MediaPlayersSettingsView(
    model: Model = LocalModel.current,
    mediaPlayers: SettingsMediaPlayers,
) {
    Form(title = "Media players") {
        Section {
            Text(
                localized(
                    "Use a media player as video source in scenes and as mic to stream recordings " +
                        "or other MP4-files."
                ),
            )
        }
        Section {
            Text(localized("⚠️ Audio is not yet fully supported, but might work."))
        }
        Section {
            ForEach(
                mediaPlayers.players,
                id = { it.id },
                onDelete = { deletePlayer(model, mediaPlayers, it) },
            ) { player ->
                ContextMenuDeleteButton(action = {
                    val offset = mediaPlayers.players.indexOfFirst { it.id == player.id }
                    if (offset != -1) {
                        deletePlayer(model, mediaPlayers, setOf(offset))
                    }
                }) {
                    MediaPlayerSettingsView(model = model, mediaPlayers = mediaPlayers, player = player)
                }
            }
            CreateButtonView {
                val mediaPlayer = SettingsMediaPlayer()
                mediaPlayer.name = makeUniqueName(
                    SettingsMediaPlayer.baseName,
                    mediaPlayers.players,
                )
                mediaPlayers.players = mediaPlayers.players + mediaPlayer
                model.addMediaPlayer(mediaPlayer)
                model.updateMediaPlayerVideoSourcesAndMics()
            }
        }
    }
}

private fun deletePlayer(
    model: Model,
    mediaPlayers: SettingsMediaPlayers,
    offsets: IndexSet,
) {
    val players = mediaPlayers.players
    val playerIds = offsets.filter { it in players.indices }.map { players[it].id }
    mediaPlayers.players = players.removing(atOffsets = offsets)
    for (playerId in playerIds) {
        model.deleteMediaPlayer(playerId)
    }
    model.updateMediaPlayerVideoSourcesAndMics()
}
