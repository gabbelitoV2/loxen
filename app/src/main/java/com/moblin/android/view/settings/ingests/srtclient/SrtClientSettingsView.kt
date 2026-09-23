package com.moblin.android.view.settings.ingests.srtclient

import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsSrtClient
import com.moblin.android.various.settings.SettingsSrtClientStream
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
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

@Composable
fun SrtClientSettingsDestination(
    model: Model = LocalModel.current,
    srtClient: SettingsSrtClient,
) {
    Form(title = "SRT client") {
        Section(
            header = "Streams",
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = localized("a stream"))
            },
        ) {
            ForEach(
                srtClient.streams,
                id = { it.id },
                onDelete = { offsets ->
                    deleteStream(model, srtClient, offsets)
                },
            ) { stream ->
                ContextMenuDeleteButton(
                    action = {
                        val offsets = srtClient.streams
                            .indexOfFirst { it.id == stream.id }
                            .takeIf { it != -1 }
                        if (offsets != null) {
                            deleteStream(model, srtClient, setOf(offsets))
                        }
                    },
                ) {
                    SrtClientStreamSettingsView(
                        model = model,
                        srtClient = srtClient,
                        stream = stream,
                        onNameChange = { name -> stream.name = name },
                        onEnabledChange = { enabled -> stream.enabled = enabled },
                        onUrlChange = { url -> stream.url = url },
                    )
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
