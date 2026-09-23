package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.settings.DeepLinkCreator
import com.moblin.android.various.settings.DeepLinkCreatorStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.CreateButtonView

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeepLinkCreatorStreamsSettingsView(deepLinkCreator: DeepLinkCreator) {
    var streamToDelete by remember { mutableStateOf<DeepLinkCreatorStream?>(null) }

    Form(title = "Streams") {
        Section {
            deepLinkCreator.streams.forEach { stream ->
                key(stream.id) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { streamToDelete = stream }
                            )
                    ) {
                        DeepLinkCreatorStreamSettingsView(
                            deepLinkCreator = deepLinkCreator,
                            stream = stream
                        )
                    }
                }
            }
            CreateButtonView {
                val stream = DeepLinkCreatorStream()
                stream.name = makeUniqueName(
                    DeepLinkCreatorStream.baseName,
                    deepLinkCreator.streams
                )
                deepLinkCreator.streams.add(stream)
            }
        }
    }

    streamToDelete?.let { stream ->
        val palette = formPalette()
        AlertDialog(
            onDismissRequest = { streamToDelete = null },
            title = { Text(localized("Delete stream")) },
            confirmButton = {
                TextButton(onClick = {
                    deepLinkCreator.streams.removeAll { it.id == stream.id }
                    streamToDelete = null
                }) {
                    Text(localized("Delete"), color = palette.red)
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
