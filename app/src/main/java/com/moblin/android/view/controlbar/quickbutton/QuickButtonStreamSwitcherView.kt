package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.moblin.android.various.model.reloadStream
import com.moblin.android.various.model.sceneUpdated
import com.moblin.android.various.model.setCurrentStream
import com.moblin.android.various.model.setIsLive
import com.moblin.android.various.model.startStream
import com.moblin.android.various.model.stopRecording
import com.moblin.android.various.model.stopStream

@Composable
fun QuickButtonStreamSwitcherView(model: Model = LocalModel.current, database: Database) {
    val currentStreamId by model.currentStreamId.collectAsState()
    val scope = rememberCoroutineScope()
    val streams = database.streams
    Form(title = "Switch stream") {
        Section(footer = "Automatically goes live when switching stream.") {
            Picker(
                "",
                selection = currentStreamId,
                options = streams.map { it.id },
                text = { id -> streams.firstOrNull { it.id == id }?.name ?: "" },
            ) { streamId ->
                model.currentStreamId.value = streamId
                model.stopStream()
                model.stopRecording()
                if (model.setCurrentStream(streamId = streamId)) {
                    model.reloadStream()
                    model.sceneUpdated(attachCamera = true, updateRemoteScene = false)
                    model.setIsLive(value = true)
                    scope.launch {
                        delay(3_000)
                        model.startStream(delayed = true)
                    }
                } else {
                    model.makeErrorToast(title = "Failed to switch stream")
                }
            }
        }
    }
}
