package com.moblin.android.view.settings.recordings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.makeRecordingPath
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.CommandCopyView
import com.moblin.android.view.utils.ExternalButtonView
import com.moblin.android.view.utils.TextButtonView
import java.io.File
import java.net.URI
import com.moblin.android.LocalModel

private const val ffmpegCommand = "ffmpeg -i input.mp4 -c copy output.mp4"

private const val ffmpegConstantFrameRateCommand = """
ffmpeg -i input.mp4 -vf "fps=60" -c:v hevc_videotoolbox -c:a copy output.mp4
"""

private const val ffmpegAudioAndVideoSlowlyDesynchronizingCommand = """
ffmpeg -i input.mp4 -af "asetrate=48002.2,aresample=48000" -c:v copy -c:a aac output.mp4
"""

@Composable
fun FilesLocationView(model: Model = LocalModel.current, text: String, path: URI) {
    val clipboard = LocalClipboardManager.current
    ExternalButtonView(
        action = {
            if (isMac()) {
                openInFinder(path = path)
            } else {
                val sharedUrl = makeSharedUrl(path = path)
                if (sharedUrl != null) {
                    openInFilesApp(sharedUrl = sharedUrl)
                } else {
                    copyPathToClipboard(model = model, path = path, clipboard = clipboard)
                }
            }
        },
    ) {
        Text(text)
    }
}

private fun makeSharedUrl(path: URI): URI? {
    TODO("no Android counterpart for the iOS shareddocuments:// URL scheme")
}

private fun openInFilesApp(sharedUrl: URI) {
    TODO("no Android counterpart for UIApplication.shared.open")
}

private fun openInFinder(path: URI) {
    TODO("no Android counterpart for UIApplication.shared.open")
}

private fun copyPathToClipboard(model: Model, path: URI, clipboard: ClipboardManager) {
    clipboard.setText(AnnotatedString(path.path ?: ""))
    val subTitle: String? = if (isMac()) {
        localized("Open it in Finder app → Go → Go to Folder...")
    } else {
        null
    }
    model.makeToast(localized("Directory copied to clipboard"), subTitle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpView(
    presentingHelp: Boolean,
    onPresentingHelpChange: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized("Help")) },
                actions = {
                    CloseToolbar(
                        presenting = presentingHelp,
                        onPresentingChange = onPresentingHelpChange,
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                Text(
                    "Recordings are saved as variable frame rate (VFR) fragmented MP4 to be resilient " +
                        "against crashes and other unexpected errors. Converting them to constant frame " +
                        "rate (CFR) standard MP4 can improve compatibility with video players and video " +
                        "editing software.",
                )
            }
            item {
                HorizontalDivider()
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        localized("How to convert a recording to standard MP4"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CommandCopyView(command = ffmpegCommand)
                }
            }
            item {
                HorizontalDivider()
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        localized("How to fix audio and video slowly desynchronizing"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Column {
                        CommandCopyView(command = ffmpegAudioAndVideoSlowlyDesynchronizingCommand)
                        Text("")
                        Text(localized("Replace sample rates and audio codec to match your recording."))
                    }
                }
            }
            item {
                HorizontalDivider()
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        localized("How to convert a recording to constant frame rate (CFR) standard MP4"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Column {
                        CommandCopyView(command = ffmpegConstantFrameRateCommand)
                        Text("")
                        Text(
                            "Replace `hevc_videotoolbox` with your preferred encoder, typically a " +
                                "hardware encoder for faster conversion.",
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsSettingsView(model: Model = LocalModel.current) {
    var presentingHelp by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Recordings")) })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                FilesLocationView(
                    model = model,
                    text = localized("Default recordings directory"),
                    path = model.recordingsStorage.defaultStorageDirectory().toURI(),
                )
            }
            val recordingPath = model.stream.value.recording.recordingPath
            if (recordingPath != null) {
                item {
                    val path = makeRecordingPath(recordingPath)
                    if (path != null) {
                        FilesLocationView(
                            model = model,
                            text = localized("Current recordings directory"),
                            path = File(path).toURI(),
                        )
                    } else {
                        Text(localized("Current recordings directory unavailable"))
                    }
                }
            }
            item {
                FilesLocationView(
                    model = model,
                    text = localized("Replays directory"),
                    path = model.replaysStorage.defaultStorageDirectory().toURI(),
                )
            }
            item {
                TextButtonView(
                    title = localized("Help"),
                    action = { presentingHelp = true },
                )
            }
        }
    }

    if (presentingHelp) {
        ModalBottomSheet(
            onDismissRequest = { presentingHelp = false },
            sheetState = sheetState,
        ) {
            HelpView(
                presentingHelp = presentingHelp,
                onPresentingHelpChange = { presentingHelp = it },
            )
        }
    }
}
