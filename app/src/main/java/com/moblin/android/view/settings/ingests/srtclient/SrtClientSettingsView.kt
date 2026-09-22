package com.moblin.android.view.settings.ingests.srtclient

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsSrtClient
import com.moblin.android.various.settings.SettingsSrtClientStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import java.util.UUID
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

private fun status(srtClient: SettingsSrtClient): String =
    srtClient.streams.count { it.enabled }.toString()

private fun deleteStream(model: Model, srtClient: SettingsSrtClient, indexes: Set<Int>) {
    val streams = srtClient.streams
    srtClient.streams = streams.filterIndexed { index, _ -> index !in indexes }.toMutableList()
    TODO("model.reloadSrtClient() has no Android counterpart")
}

@Composable
fun SrtClientSettingsView(
    model: Model = LocalModel.current,
    srtClient: SettingsSrtClient,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("SrtClientSettingsDestination") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(localized("SRT client"))
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = status(srtClient))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SrtClientSettingsDestination(
    model: Model = LocalModel.current,
    srtClient: SettingsSrtClient,
) {
    val streams = srtClient.streams
    var deleteMenuStreamId by remember { mutableStateOf<UUID?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("SRT client")) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                Text(
                    text = localized("Streams"),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            itemsIndexed(items = streams, key = { _, stream -> stream.id }) { index, stream ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            deleteStream(model, srtClient, setOf(index))
                            true
                        } else {
                            false
                        }
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer),
                        )
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onLongClick = { deleteMenuStreamId = stream.id },
                                onClick = {},
                            ),
                    ) {
                        SrtClientStreamSettingsView(
                            model = model,
                            srtClient = srtClient,
                            stream = stream,
                            onNameChange = { name -> stream.name = name },
                            onEnabledChange = { enabled -> stream.enabled = enabled },
                            onUrlChange = { url -> stream.url = url },
                        )
                        DropdownMenu(
                            expanded = deleteMenuStreamId == stream.id,
                            onDismissRequest = { deleteMenuStreamId = null },
                        ) {
                            DropdownMenuItem(
                                text = { Text(localized("Delete")) },
                                onClick = {
                                    deleteMenuStreamId = null
                                    val offsets = makeOffsets(streams, stream.id)
                                    if (offsets != null) {
                                        deleteStream(model, srtClient, setOf(offsets))
                                    }
                                },
                            )
                        }
                    }
                }
            }
            item {
                CreateButtonView {
                    val stream = SettingsSrtClientStream()
                    stream.name = makeUniqueName(
                        SettingsSrtClientStream.baseName,
                        streams,
                    )
                    srtClient.streams = (streams + stream).toMutableList()
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a stream"))
            }
        }
    }
}
