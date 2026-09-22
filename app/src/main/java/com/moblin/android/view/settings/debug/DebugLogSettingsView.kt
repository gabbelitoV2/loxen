package com.moblin.android.view.settings.debug

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.LogEntry
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.ShareSheetView
import java.util.UUID

private data class ShareItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
)

private fun isMessageVisible(logFilter: String, message: String): Boolean =
    logFilter.isEmpty() || message.lowercase().contains(logFilter.lowercase())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogSettingsView(
    model: Model,
    debug: SettingsDebug,
    log: List<LogEntry>,
    presentingLog: Boolean,
    onPresentingLogChange: (Boolean) -> Unit,
    reloadLog: () -> Unit,
    clearLog: () -> Unit,
    onLogChange: (List<LogEntry>) -> Unit,
) {
    val logFilter by debug.logFilter.collectAsState()
    var shareItem by remember { mutableStateOf<ShareItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log") },
                navigationIcon = {
                    CloseToolbar(
                        presenting = presentingLog,
                        onPresentingChange = onPresentingLogChange,
                    )
                },
                actions = {
                    IconButton(
                        enabled = log.isNotEmpty(),
                        onClick = {
                            shareItem = ShareItem(
                                url = model.formatLog(
                                    log.filter { isMessageVisible(logFilter, it.message) },
                                ),
                            )
                        },
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                    }
                    IconButton(
                        enabled = log.isNotEmpty(),
                        onClick = {
                            onLogChange(emptyList())
                            clearLog()
                        },
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                    IconButton(
                        onClick = { reloadLog() },
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                OutlinedTextField(
                    value = logFilter,
                    onValueChange = { debug.logFilter.value = it },
                    label = { Text("Filter") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (log.isEmpty()) {
                item {
                    Text("The log is empty.")
                }
            } else {
                items(log) { item ->
                    if (isMessageVisible(logFilter, item.message)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(item.message)
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    shareItem?.let { item ->
        ModalBottomSheet(onDismissRequest = { shareItem = null }) {
            ShareSheetView(activityItems = listOf(item.url))
        }
    }
}
