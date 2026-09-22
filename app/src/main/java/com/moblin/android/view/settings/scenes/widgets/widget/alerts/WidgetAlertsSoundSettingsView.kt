package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsAlertsMediaGallery
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsAlertMediaType
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import java.util.UUID

private fun loadSound(model: Model, soundId: UUID): AudioPlayer? {
    val bundledSound = model.database.alertsMediaGallery.bundledSounds.firstOrNull { it.id == soundId }
    val url = if (bundledSound != null) {
        TODO("no Android counterpart for Bundle.main.url(forResource:withExtension:)")
    } else {
        model.alertMediaStorage.makePath(soundId)
    }
    val path = url ?: return null
    return runCatching { AudioPlayer(path) }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSoundView(
    model: Model,
    media: SettingsAlertsMediaGalleryItem,
    initialAudioPlayer: AudioPlayer? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    var audioPlayer by remember { mutableStateOf(initialAudioPlayer) }

    fun onUrl(url: String) {
        model.alertMediaStorage.add(media.id, url)
        audioPlayer = loadSound(model, media.id)
        model.updateAlertsSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sound") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                TextEditNavigationView(
                    title = localized("Name"),
                    value = media.name,
                    onSubmit = { media.name = it },
                )
            }
            item {
                val player = audioPlayer
                if (player != null) {
                    TextButtonView("Play") {
                        player.play()
                    }
                }
            }
            item {
                TextButton(
                    onClick = {
                        showPicker = true
                        model.onDocumentPickerUrl = { url -> onUrl(url) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (audioPlayer != null) "Select another sound" else "Select sound",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            AlertPickerView(type = SettingsWidgetAlertsAlertMediaType.Audio)
        }
    }
}

@Composable
fun SoundGalleryItemView(
    model: Model,
    sound: SettingsAlertsMediaGalleryItem,
    onNavigate: (String) -> Unit,
) {
    Text(
        sound.name,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate("CustomSoundView/${sound.id}")
            }
            .padding(vertical = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundGalleryView(
    model: Model,
    gallery: SettingsAlertsMediaGallery,
    alert: SettingsWidgetAlertsAlert,
    soundId: UUID,
    onSoundIdChange: (UUID) -> Unit,
    onNavigate: (String) -> Unit,
) {
    fun deleteSound(sound: SettingsAlertsMediaGalleryItem) {
        val index = gallery.customSounds.indexOfFirst { it.id == sound.id }
        if (index == -1) {
            return
        }
        gallery.customSounds.removeAt(index)
        model.fixAlertMedias()
        onSoundIdChange(alert.soundId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My sounds") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                gallery.customSounds.forEach { sound ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SoundGalleryItemView(
                                model = model,
                                sound = sound,
                                onNavigate = onNavigate,
                            )
                        }
                        IconButton(onClick = { deleteSound(sound) }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    }
                }
            }
            item {
                TextButtonView("Add") {
                    gallery.customSounds.add(SettingsAlertsMediaGalleryItem(name = "My sound"))
                }
            }
            item {
                SwipeLeftToDeleteHelpView(localized("a sound"))
            }
        }
    }
}

private var player: AudioPlayer? = null

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSoundSelectorView(
    model: Model,
    gallery: SettingsAlertsMediaGallery,
    alert: SettingsWidgetAlertsAlert,
    soundId: UUID,
    onSoundIdChange: (UUID) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sound") })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                (gallery.bundledSounds + gallery.customSounds).forEach { sound ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSoundIdChange(sound.id) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = sound.id == soundId,
                            onClick = { onSoundIdChange(sound.id) },
                        )
                        Text(sound.name)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                player = loadSound(model, sound.id)
                                player?.play()
                            },
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { onNavigate("SoundGalleryView") }) {
                    Text("My sounds")
                }
            }
        }
    }
    LaunchedEffect(soundId) {
        alert.soundId = soundId
        model.updateAlertsSettings()
    }
    DisposableEffect(Unit) {
        onDispose {
            player = null
        }
    }
}
