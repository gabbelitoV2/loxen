package com.moblin.android.view.settings.deeplinkcreator

import androidx.compose.runtime.Composable
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.move
import com.moblin.android.platform.swiftui.remove
import com.moblin.android.various.settings.DeepLinkCreator
import com.moblin.android.various.settings.DeepLinkCreatorStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView

@Composable
fun DeepLinkCreatorStreamsSettingsView(deepLinkCreator: DeepLinkCreator) {
    Form(title = "Streams") {
        Section {
            ForEach(
                deepLinkCreator.streams,
                id = { it.id },
                onDelete = { offsets ->
                    deepLinkCreator.streams.remove(atOffsets = offsets)
                },
                onMove = { froms, to ->
                    deepLinkCreator.streams.move(fromOffsets = froms, toOffset = to)
                },
            ) { stream ->
                ContextMenuDeleteButton(action = {
                    deepLinkCreator.streams.removeAll { it.id == stream.id }
                }) {
                    DeepLinkCreatorStreamSettingsView(
                        deepLinkCreator = deepLinkCreator,
                        stream = stream
                    )
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
}
