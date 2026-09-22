package com.moblin.android.view.settings.ingests.whepclient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.moblin.android.various.settings.SettingsWhepClient
import com.moblin.android.various.settings.SettingsWhepClientStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

private fun status(numberOfEnabledStreams: Int): String {
    return numberOfEnabledStreams.toString()
}

private fun deleteStream(model: Model, whepClient: SettingsWhepClient, indexes: List<Int>) {
    val streams = whepClient.streams.value.toMutableList()
    indexes.sortedDescending().forEach { index ->
        if (index in streams.indices) {
            streams.removeAt(index)
        }
    }
    whepClient.streams.value = streams
    model.reloadWhepClient()
}

@Composable
fun WhepClientSettingsView(
    model: Model,
    whepClient: SettingsWhepClient,
    onNavigate: (String) -> Unit,
) {
    var numberOfEnabledStreams by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        numberOfEnabledStreams = whepClient.streams.value.count { it.enabled }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("whepClient") }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("WHEP client")
        Spacer(Modifier.weight(1f))
        GrayTextView(text = status(numberOfEnabledStreams))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhepClientSettingsDestinationView(
    model: Model,
    whepClient: SettingsWhepClient,
    onNavigate: (String) -> Unit,
) {
    val streams by whepClient.streams.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("WHEP client") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            item {
                Text("The WHEP client allows Moblin to receive video streams from a WHEP endpoint.")
            }
            item {
                Text(
                    text = "Streams",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
            }
            itemsIndexed(items = streams) { index, stream ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WhepClientStreamSettingsView(
                            whepClient = whepClient,
                            stream = stream,
                            onNavigate = onNavigate,
                        )
                    }
                    IconButton(
                        onClick = {
                            deleteStream(
                                model = model,
                                whepClient = whepClient,
                                indexes = listOf(index),
                            )
                        },
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
            item {
                CreateButtonView {
                    val stream = SettingsWhepClientStream()
                    stream.name = makeUniqueName(
                        name = SettingsWhepClientStream.baseName,
                        existingNames = whepClient.streams.value,
                    )
                    whepClient.streams.value = whepClient.streams.value + stream
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a stream"))
            }
        }
    }
}
