package com.moblin.android.view.settings.applemusic

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model

private data class MusicPlayerState(val isPlaying: Boolean)

private data class MusicQueueEntry(val id: String, val title: String)

private data class MusicPlayerQueue(
    val entries: List<MusicQueueEntry>,
    val currentEntryId: String?,
)

private data class MusicSubscriptionState(val canBecomeSubscriber: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicSettingsView(model: Model) {
    var searchText by remember { mutableStateOf("") }
    var isShowingSubscriptionOffer by remember { mutableStateOf(false) }
    var musicSubscription by remember { mutableStateOf<MusicSubscriptionState?>(null) }
    val playerState = rememberMusicPlayerState()
    val playerQueue = rememberMusicPlayerQueue()

    LaunchedEffect(Unit) {
        TODO("no Android counterpart for MusicKit")
    }

    if (isShowingSubscriptionOffer) {
        TODO("no Android counterpart for MusicKit")
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Apple Music") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (musicSubscription?.canBecomeSubscriber == true) {
                item {
                    TextButton(onClick = { isShowingSubscriptionOffer = true }) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                        Text("Join", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            item {
                val queueEnabled = playerQueue.entries.isNotEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { model.previousMusic(count = 1) },
                        enabled = queueEnabled,
                    ) {
                        Icon(
                            Icons.Default.FastRewind,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (playerState.isPlaying) {
                        IconButton(
                            onClick = { model.pauseMusic() },
                            enabled = queueEnabled,
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { model.playMusic() },
                            enabled = queueEnabled,
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { model.nextMusic(count = 1) },
                        enabled = queueEnabled,
                    ) {
                        Icon(
                            Icons.Default.FastForward,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Add song") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        model.addMusic(title = searchText) { }
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
            item {
                Text(
                    "Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(playerQueue.entries, key = { it.id }) { entry ->
                if (entry.id == playerQueue.currentEntryId) {
                    Text(
                        "• ${entry.title}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    Text(
                        entry.title,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberMusicPlayerState(): MusicPlayerState =
    TODO("no Android counterpart for MusicKit")

@Composable
private fun rememberMusicPlayerQueue(): MusicPlayerQueue =
    TODO("no Android counterpart for MusicKit")
