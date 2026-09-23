package com.moblin.android.view.settings.streaminghistory

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.format
import com.moblin.android.common.various.formatDate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.NavigationTitle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.storages.StreamingHistoryDatabase
import com.moblin.android.various.storages.StreamingHistoryStream
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.localized

@Composable
private fun StreamingHistorySettingsSummaryView(database: StreamingHistoryDatabase) {
    val totalStreams by database.totalStreams.collectAsState()
    val totalTime by database.totalTime.collectAsState()
    val totalBytes by database.totalBytes.collectAsState()
    val palette = formPalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(totalStreams.toString(), fontSize = 22.sp, color = palette.label)
            Text(localized("Total streams"), fontSize = 15.sp, color = palette.label)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(totalTime.format(), fontSize = 22.sp, color = palette.label)
            Text(localized("Total time"), fontSize = 15.sp, color = palette.label)
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(totalBytes.formatBytes(), fontSize = 22.sp, color = palette.label)
            Text(localized("Total sent"), fontSize = 15.sp, color = palette.label)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

private fun Long.formatBytes(): String {
    val units = listOf("kB", "MB", "GB", "TB")
    var value = this.toDouble()
    var unitIndex = -1
    while (value >= 1000.0 && unitIndex < units.size - 1) {
        value /= 1000.0
        unitIndex += 1
    }
    return if (unitIndex == -1) "$this B" else "${formatOneDecimal(value)} ${units[unitIndex]}"
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
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val streams by database.streams.collectAsState()
    Form(modifier = modifier) {
        Section {
            ForEach(
                streams,
                id = { it.id },
                onDelete = { deleteStream(offsets = it.toList(), database = database, model = model) },
            ) { stream ->
                ContextMenuDeleteButton(action = {
                    val offset = database.streams.value.indexOfFirst { it.id == stream.id }
                    if (offset != -1) {
                        deleteStream(offsets = listOf(offset), database = database, model = model)
                    }
                }) {
                    NavigationLink(
                        destination = {
                            StreamingHistoryStreamSettingsView(stream = stream)
                        },
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(formatStreamTitle(stream = stream))
                            Text(stream.settings.name, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamingHistorySettingsView(
    model: Model = LocalModel.current,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationTitle("Streaming history")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isSystemInDarkTheme()) Color.Black else Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StreamingHistorySettingsSummaryView(database = model.streamingHistory.database)
        StreamingHistorySettingsStreamsView(
            model = model,
            database = model.streamingHistory.database,
            modifier = Modifier.weight(1f),
            onNavigate = onNavigate,
        )
    }
}
