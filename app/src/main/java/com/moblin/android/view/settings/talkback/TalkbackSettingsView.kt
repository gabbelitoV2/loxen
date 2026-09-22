package com.moblin.android.view.settings.talkback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getMicById
import com.moblin.android.various.model.updateTalkback
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsTalkback
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.IngestsShortcutView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.InlinePickerView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.LocalModel

private fun onChange(micId: String, model: Model, talkback: SettingsTalkback) {
    talkback.micId.value = micId
    model.updateTalkback()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalkbackSettingsView(
    model: Model = LocalModel.current,
    mics: SettingsMics,
    talkback: SettingsTalkback,
) {
    val enabled by talkback.enabled.collectAsState()
    val micId by talkback.micId.collectAsState()
    val micList by mics.mics.collectAsState()
    var showMicPicker by remember { mutableStateOf(false) }

    if (showMicPicker) {
        InlinePickerView(
            title = "Mic",
            onChange = { id ->
                onChange(id, model, talkback)
                showMicPicker = false
            },
            items = micList
                .filter { it.isNetwork() }
                .map { InlinePickerItem(id = it.id, text = it.name) },
            initialSelectedId = micId,
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Talkback") }) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("Play audio from an Ingest in your speakers.")
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Enabled")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            talkback.enabled.value = it
                            model.updateTalkback()
                        },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMicPicker = true },
                ) {
                    Text("Mic")
                    Spacer(modifier = Modifier.weight(1f))
                    GrayTextView(text = model.getMicById(micId)?.name ?: "Unknown 😢")
                }
                ShortcutSectionView {
                    IngestsShortcutView(model = model)
                }
            }
        }
    }
}
