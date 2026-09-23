package com.moblin.android.view.settings.streams.stream.chat

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream

@Composable
fun StreamEmotesSettingsView(model: Model = LocalModel.current, stream: SettingsStream) {
    Form(title = "Emotes") {
        Section {
            Toggle(
                title = "BTTV",
                isOn = binding(
                    get = { stream.chat.bttvEmotes },
                    set = { value ->
                        stream.chat.bttvEmotes = value
                        if (stream.enabled) {
                            model.bttvEmotesEnabledUpdated()
                        }
                    },
                ),
            )
            Toggle(
                title = "FFZ",
                isOn = binding(
                    get = { stream.chat.ffzEmotes },
                    set = { value ->
                        stream.chat.ffzEmotes = value
                        if (stream.enabled) {
                            model.ffzEmotesEnabledUpdated()
                        }
                    },
                ),
            )
            Toggle(
                title = "7TV",
                isOn = binding(
                    get = { stream.chat.seventvEmotes },
                    set = { value ->
                        stream.chat.seventvEmotes = value
                        if (stream.enabled) {
                            model.seventvEmotesEnabledUpdated()
                        }
                    },
                ),
            )
        }
    }
}
