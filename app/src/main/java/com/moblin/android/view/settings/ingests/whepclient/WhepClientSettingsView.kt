package com.moblin.android.view.settings.ingests.whepclient

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWhepClient
import com.moblin.android.various.settings.SettingsWhepClientStream
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.various.model.reloadWhepClient
import com.moblin.android.various.model.updateWhepVideoSourcesAndMics

private fun status(numberOfEnabledStreams: Int): String {
    return numberOfEnabledStreams.toString()
}

private fun deleteStream(model: Model, whepClient: SettingsWhepClient, indexes: List<Int>) {
    val streams = whepClient.streams.toMutableList()
    indexes.sortedDescending().forEach { index ->
        if (index in streams.indices) {
            streams.removeAt(index)
        }
    }
    whepClient.streams.clear()
    whepClient.streams.addAll(streams)
    model.reloadWhepClient()
    model.updateWhepVideoSourcesAndMics()
}

@Composable
fun WhepClientSettingsView(
    model: Model = LocalModel.current,
    whepClient: SettingsWhepClient,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var numberOfEnabledStreams by remember { mutableStateOf(0) }
    LaunchedEffect(whepClient.streams.size) {
        numberOfEnabledStreams = whepClient.streams.count { it.enabled }
    }
    NavigationLink(
        destination = {
            WhepClientSettingsDestinationView(
                model = model,
                whepClient = whepClient,
                onNavigate = onNavigate,
            )
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized("WHEP client"))
            Spacer(Modifier.weight(1f))
            GrayTextView(text = status(numberOfEnabledStreams))
        }
    }
}

@Composable
fun WhepClientSettingsDestinationView(
    model: Model = LocalModel.current,
    whepClient: SettingsWhepClient,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Form(title = "WHEP client") {
        Section {
            Text(
                localized(
                    "The WHEP client allows Moblin to receive video streams from a WHEP endpoint.",
                ),
            )
        }
        Section(
            header = "Streams",
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a stream"))
            },
        ) {
            ForEach(
                whepClient.streams,
                id = { it.id },
                onDelete = { offsets ->
                    deleteStream(model, whepClient, offsets.toList())
                },
            ) { stream ->
                ContextMenuDeleteButton(
                    action = {
                        val offset = whepClient.streams.indexOfFirst { it.id == stream.id }
                        if (offset >= 0) {
                            deleteStream(model, whepClient, listOf(offset))
                        }
                    },
                ) {
                    WhepClientStreamSettingsView(
                        whepClient = whepClient,
                        stream = stream,
                        onNavigate = onNavigate,
                    )
                }
            }
            CreateButtonView {
                val stream = SettingsWhepClientStream()
                stream.name = makeUniqueName(
                    name = SettingsWhepClientStream.baseName,
                    existingNames = whepClient.streams,
                )
                whepClient.streams.add(stream)
                model.updateWhepVideoSourcesAndMics()
            }
        }
    }
}
