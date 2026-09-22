package com.moblin.android.view.settings.streaminghistory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.format
import com.moblin.android.common.various.formatBytes
import com.moblin.android.common.various.formatDate
import com.moblin.android.various.model.Model
import com.moblin.android.various.storages.StreamingHistoryDatabase
import com.moblin.android.various.storages.StreamingHistoryStream
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun StreamingHistorySettingsSummaryView(database: StreamingHistoryDatabase) {
    val totalStreams by database.totalStreams.collectAsState()
    val totalTime by database.totalTime.collectAsState()
    val totalBytes by database.totalBytes.collectAsState()
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(totalStreams.toString(), style = MaterialTheme.typography.titleLarge)
            Text("Total streams", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(totalTime.format(), style = MaterialTheme.typography.titleLarge)
            Text("Total time", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatBytes(totalBytes), style = MaterialTheme.typography.titleLarge)
            Text("Total sent", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun formatStreamTitle(stream: StreamingHistoryStream): String {
    return "${formatDate(stream.startTime)}, ${stream.duration().format()}"
}

private fun deleteStream(offsets: List<Int>, database: StreamingHistoryDatabase, model: Model) {
    val removed = offsets.toSet()
    database.streams.value = database.streams.value.filterIndexed { index, _ -> index !in removed }
    model.streamingHistory.store()
}

@Composable
private fun StreamingHistorySettingsStreamsView(
    model: Model = LocalModel.current,
    database: StreamingHistoryDatabase,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val streams by database.streams.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(items = streams, key = { _, stream -> stream.id }) { index, stream ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onNavigate("StreamingHistoryStreamSettingsView/${stream.id}")
                        },
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(formatStreamTitle(stream = stream))
                    Text(stream.settings.name, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(
                    onClick = {
                        deleteStream(offsets = listOf(index), database = database, model = model)
                    },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingHistorySettingsView(model: Model = LocalModel.current, onNavigate: (String) -> Unit = LocalOnNavigate.current) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Streaming history") })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            StreamingHistorySettingsSummaryView(database = model.streamingHistory.database)
            StreamingHistorySettingsStreamsView(
                model = model,
                database = model.streamingHistory.database,
                onNavigate = onNavigate,
            )
        }
    }
}
