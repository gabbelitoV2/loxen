package com.moblin.android.view.settings.streams.stream.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamEmotesSettingsView(model: Model, stream: SettingsStream) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Emotes") })
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("BTTV")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = stream.chat.bttvEmotes,
                        onCheckedChange = { value ->
                            stream.chat.bttvEmotes = value
                            if (stream.enabled) {
                                model.bttvEmotesEnabledUpdated()
                            }
                        },
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("FFZ")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = stream.chat.ffzEmotes,
                        onCheckedChange = { value ->
                            stream.chat.ffzEmotes = value
                            if (stream.enabled) {
                                model.ffzEmotesEnabledUpdated()
                            }
                        },
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("7TV")
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = stream.chat.seventvEmotes,
                        onCheckedChange = { value ->
                            stream.chat.seventvEmotes = value
                            if (stream.enabled) {
                                model.seventvEmotesEnabledUpdated()
                            }
                        },
                    )
                }
            }
        }
    }
}
