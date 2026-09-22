package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.settings.DeepLinkCreator
import com.moblin.android.various.settings.DeepLinkCreatorStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.CreateButtonView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeepLinkCreatorStreamsSettingsView(deepLinkCreator: DeepLinkCreator) {
    val streams = deepLinkCreator.streams
    var streamToDelete by remember { mutableStateOf<DeepLinkCreatorStream?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Streams")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(streams, key = { it.id }) { stream ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { streamToDelete = stream }
                        )
                ) {
                    DeepLinkCreatorStreamSettingsView(deepLinkCreator = deepLinkCreator, stream = stream)
                }
            }
            item {
                CreateButtonView {
                    val stream = DeepLinkCreatorStream()
                    stream.name = makeUniqueName(DeepLinkCreatorStream.baseName, streams)
                    deepLinkCreator.streams.add(stream)
                }
            }
        }
    }

    streamToDelete?.let { stream ->
        AlertDialog(
            onDismissRequest = { streamToDelete = null },
            title = { Text(localized("Delete stream")) },
            confirmButton = {
                TextButton(onClick = {
                    deepLinkCreator.streams.removeAll { it.id == stream.id }
                    streamToDelete = null
                }) {
                    Text(localized("Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { streamToDelete = null }) {
                    Text(localized("Cancel"))
                }
            }
        )
    }
}
