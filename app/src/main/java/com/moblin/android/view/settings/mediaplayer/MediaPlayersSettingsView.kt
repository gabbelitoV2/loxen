package com.moblin.android.view.settings.mediaplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.addMediaPlayer
import com.moblin.android.various.model.deleteMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayer
import com.moblin.android.various.settings.SettingsMediaPlayers
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.CreateButtonView

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
            mediaPlayers.players.forEach { player ->
                key(player.id) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MediaPlayerSettingsView(mediaPlayers = mediaPlayers, player = player)
                        SystemImage(
                            name = "trash",
                            fontSize = 20.sp,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                val offset =
                                    mediaPlayers.players.indexOfFirst { it.id == player.id }
                                if (offset != -1) {
                                    deletePlayer(model, mediaPlayers, offset)
                                }
                            },
                        )
                    }
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
            }
        }
    }
}

private fun deletePlayer(
    model: Model,
    mediaPlayers: SettingsMediaPlayers,
    offset: Int,
) {
    val players = mediaPlayers.players
    if (offset in players.indices) {
        model.deleteMediaPlayer(players[offset].id)
    }
    mediaPlayers.players = players.filterIndexed { index, _ -> index != offset }
}
