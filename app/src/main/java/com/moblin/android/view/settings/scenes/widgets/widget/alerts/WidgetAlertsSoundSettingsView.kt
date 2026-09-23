package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.AudioPlayer
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsAlertsMediaGallery
import com.moblin.android.various.settings.SettingsAlertsMediaGalleryItem
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import java.io.File
import java.util.UUID

private fun loadSound(model: Model, soundId: UUID): AudioPlayer? {
    val bundledSound = model.database.alertsMediaGallery.bundledSounds.firstOrNull { it.id == soundId }
    val url = if (bundledSound != null) {
        Unit
    } else {
        model.alertMediaStorage.makePath(soundId)
    }
    val path = url ?: return null
    return runCatching {
        TODO()
    }.getOrNull()
}

@Composable
fun CustomSoundView(
    model: Model = LocalModel.current,
    media: SettingsAlertsMediaGalleryItem,
    initialAudioPlayer: AudioPlayer? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    var audioPlayer by remember { mutableStateOf(initialAudioPlayer) }

    fun onUrl(url: String) {
        model.alertMediaStorage.add(media.id, File(url))
        audioPlayer = loadSound(model, media.id)
        model.updateAlertsSettings()
    }

    Form(title = "Sound") {
        Section {
            TextEditNavigationView(
                title = localized("Name"),
                value = media.name,
                onSubmit = { media.name = it },
            )
        }
        Section {
            val player = audioPlayer
            if (player != null) {
                TextButtonView("Play") {
                    player.play()
                }
            }
        }
        Section {
            FormButton(
                title = if (audioPlayer != null) "Select another sound" else "Select sound",
                centered = true,
            ) {
                showPicker = true
                model.onDocumentPickerUrl = { url -> onUrl(url) }
            }
        }
    }
    Sheet(isPresented = showPicker, onDismissRequest = { showPicker = false }) {
        AlertPickerView(type = "audio")
    }
}

@Composable
fun SoundGalleryItemView(
    model: Model = LocalModel.current,
    sound: SettingsAlertsMediaGalleryItem,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            CustomSoundView(
                model = model,
                media = sound,
                initialAudioPlayer = remember(sound.id) { loadSound(model, sound.id) },
            )
        },
    ) {
        Text(sound.name)
    }
}

@Composable
fun SoundGalleryView(
    model: Model = LocalModel.current,
    gallery: SettingsAlertsMediaGallery,
    alert: SettingsWidgetAlertsAlert,
    soundId: UUID,
    onSoundIdChange: (UUID) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun deleteSound(offsets: IndexSet) {
        gallery.customSounds = gallery.customSounds.removing(atOffsets = offsets)
        model.fixAlertMedias()
        onSoundIdChange(alert.soundId)
    }

    Form(title = "My sounds") {
        Section(footerContent = { SwipeLeftToDeleteHelpView(localized("a sound")) }) {
            ForEach(
                gallery.customSounds,
                id = { it.id },
                onDelete = { deleteSound(it) },
            ) { sound ->
                ContextMenuDeleteButton(
                    action = {
                        val index = gallery.customSounds.indexOfFirst { it.id == sound.id }
                        if (index >= 0) {
                            deleteSound(setOf(index))
                        }
                    },
                ) {
                    SoundGalleryItemView(
                        model = model,
                        sound = sound,
                        onNavigate = onNavigate,
                    )
                }
            }
            TextButtonView("Add") {
                gallery.customSounds = gallery.customSounds + SettingsAlertsMediaGalleryItem(name = "My sound")
            }
        }
    }
}

private var player: AudioPlayer? = null

@Composable
fun AlertSoundSelectorView(
    model: Model = LocalModel.current,
    gallery: SettingsAlertsMediaGallery,
    alert: SettingsWidgetAlertsAlert,
    soundId: UUID,
    onSoundIdChange: (UUID) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val sounds = gallery.bundledSounds + gallery.customSounds

    Form(title = "Sound") {
        Section {
            Picker(
                title = "",
                selection = soundId,
                options = sounds.map { it.id },
                text = { id -> sounds.firstOrNull { it.id == id }?.name ?: id.toString() },
                onChange = { id ->
                    onSoundIdChange(id)
                    alert.soundId = id
                    model.updateAlertsSettings()
                },
            )
        }
        Section {
            NavigationLink(title = "My sounds") {
                SoundGalleryView(
                    model = model,
                    gallery = gallery,
                    alert = alert,
                    soundId = soundId,
                    onSoundIdChange = onSoundIdChange,
                    onNavigate = onNavigate,
                )
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            player = null
        }
    }
}
