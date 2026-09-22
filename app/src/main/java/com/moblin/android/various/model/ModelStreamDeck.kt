package com.moblin.android.various.model

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.moblin.android.various.settings.SettingsStreamDeckKey
import com.moblin.android.various.settings.SettingsStreamDeckLayout
import com.moblin.android.various.utils.isPad
import com.moblin.android.view.settings.streamdeck.StreamDeckKeyView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.moblin.android.LocalModel

class StreamDeck {
    val isDeviceDriverInstalled = MutableStateFlow(true)

    val streamDeck = MutableStateFlow<SettingsStreamDeckLayout?>(null)

    fun setIsDeviceDriverInstalled(value: Boolean) {
        isDeviceDriverInstalled.value = value
    }

    fun setStreamDeck(value: SettingsStreamDeckLayout?) {
        streamDeck.value = value
    }
}

@Composable
private fun StreamDeckKeyItemView(model: Model = LocalModel.current, index: Int, key: SettingsStreamDeckKey) {
    StreamDeckKeyView(
        onPressed = { pressed ->
            model.handleControllerFunction(
                buttonId = "sd:$index",
                function = key.function,
                functionData = key.functionData,
                pressed = pressed,
            )
        },
    ) {
        Text(
            text = key.text,
            modifier = Modifier
                .fillMaxSize()
                .background(key.colorColor),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StreamDeckView(model: Model = LocalModel.current, streamDeck: StreamDeck) {
    val layout by streamDeck.streamDeck.collectAsState()
    if (layout != null) {
        TODO("no Android counterpart for StreamDeckKit StreamDeckLayout and StreamDeckKeyAreaLayout")
    } else {
        TODO("no Android counterpart for StreamDeckKit StreamDeckLayout and StreamDeckKeyAreaLayout")
    }
}

fun Model.setupStreamDeck() {
    if (!isPad()) {
        return
    }
    TODO("no Android counterpart for StreamDeckKit StreamDeckSession.setUp")
    updateIsStreamDeckDeviceDriverInstalled()
}

fun Model.setSelectedStreamDeck() {
    val streamDecks = database.streamDecks
    streamDeck.setStreamDeck(streamDecks.layouts.firstOrNull { it.id == streamDecks.selectedId })
    if (streamDeck.streamDeck.value == null) {
        streamDecks.selectedId = null
    }
}

fun Model.updateIsStreamDeckDeviceDriverInstalled() {
    if (!isPad()) {
        return
    }
    streamDeck.setIsDeviceDriverInstalled(TODO("no Android counterpart for UIApplication.canOpenURL"))
}
