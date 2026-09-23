package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.platform.swiftui.Alert
import com.moblin.android.platform.swiftui.ButtonRole
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
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

    val pendingDelete = streamToDelete
    Alert(
        title = "Delete stream",
        isPresented = pendingDelete != null,
        onDismissRequest = { streamToDelete = null },
    ) {
        Button("Cancel", role = ButtonRole.cancel)
        Button("Delete", role = ButtonRole.destructive) {
            if (pendingDelete != null) {
                deepLinkCreator.streams.removeAll { it.id == pendingDelete.id }
            }
        }
    }
}
