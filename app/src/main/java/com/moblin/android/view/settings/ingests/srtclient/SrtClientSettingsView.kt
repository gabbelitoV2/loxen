package com.moblin.android.view.settings.ingests.srtclient

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsSrtClient
import com.moblin.android.various.settings.SettingsSrtClientStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import java.util.UUID
import com.moblin.android.various.model.reloadSrtClient

private fun status(srtClient: SettingsSrtClient): String =
    srtClient.streams.count { it.enabled }.toString()

private fun deleteStream(model: Model, srtClient: SettingsSrtClient, indexes: Set<Int>) {
    srtClient.streams =
        srtClient.streams.filterIndexed { index, _ -> index !in indexes }.toMutableList()
    model.reloadSrtClient()
}

@Composable
fun SrtClientSettingsView(
    model: Model = LocalModel.current,
    srtClient: SettingsSrtClient,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            SrtClientSettingsDestination(model = model, srtClient = srtClient)
        },
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
    var deleteMenuStreamId by remember { mutableStateOf<UUID?>(null) }
    Form(title = "SRT client") {
        Section(
            header = "Streams",
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a stream"))
            },
        ) {
            srtClient.streams.forEachIndexed { index, stream ->
                key(stream.id) {
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
                                    .background(formPalette().red),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Text(
                                    text = localized("Delete"),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                        },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
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
                                        val offsets = srtClient.streams
                                            .indexOfFirst { it.id == stream.id }
                                            .takeIf { it != -1 }
                                        if (offsets != null) {
                                            deleteStream(model, srtClient, setOf(offsets))
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
            CreateButtonView {
                val stream = SettingsSrtClientStream()
                stream.name = makeUniqueName(
                    SettingsSrtClientStream.baseName,
                    srtClient.streams,
                )
                srtClient.streams = (srtClient.streams + stream).toMutableList()
            }
        }
    }
}
