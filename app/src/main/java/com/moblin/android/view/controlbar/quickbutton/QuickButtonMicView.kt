package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Mic
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsMicsMic
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickButtonMicMicView(
    model: Model,
    mic: SettingsMicsMic,
    modelMic: Mic,
    deleteEnabled: Boolean,
    onDelete: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentMic by modelMic.current.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    scope.launch {
                        model.updateMicsListAsync()
                        if (mic.connected) {
                            model.manualSelectMicById(mic.id)
                        }
                    }
                },
                onLongClick = {
                    if (deleteEnabled) {
                        onDelete()
                    }
                },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DraggableItemPrefixView()
        Icon(
            imageVector = if (mic.connected) Icons.Filled.Cable else Icons.Filled.LinkOff,
            contentDescription = null,
        )
        Text(
            text = mic.name,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = if (mic == currentMic) Color.Blue else Color.Transparent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonMicView(
    model: Model,
    mics: SettingsMics,
    modelMic: Mic,
) {
    val micsList by mics.mics.collectAsState()
    val autoSwitch by mics.autoSwitch.collectAsState()
    val currentMic by modelMic.current.collectAsState()
    val onMove: (Int, Int) -> Unit = { _, _ ->
        TODO("no Compose counterpart for SwiftUI List onMove reordering")
    }

    LaunchedEffect(Unit) {
        model.updateMicsListAsync()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(localized("Mic")) })
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(micsList, key = { it.id.toString() }) { mic ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart && mic != currentMic) {
                            mics.mics.value = mics.mics.value.filterNot { it.id == mic.id }
                            true
                        } else {
                            false
                        }
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {},
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = mic != currentMic,
                ) {
                    QuickButtonMicMicView(
                        model = model,
                        mic = mic,
                        modelMic = modelMic,
                        deleteEnabled = mic != currentMic,
                        onDelete = {
                            TODO("no Compose counterpart for contextMenuDeleteButton")
                        },
                    )
                }
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("Highest priority mic at the top of the list."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a mic"))
                }
            }
            if (false) {
                item {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(localized("Auto switch"))
                            Switch(
                                checked = autoSwitch,
                                onCheckedChange = { mics.autoSwitch.value = it },
                            )
                        }
                        Text(localized("Automatically switch to highest priority mic when plugged in."))
                    }
                }
            }
        }
    }
}
