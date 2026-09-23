package com.moblin.android.view.settings.recordings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.makeRecordingPath
import com.moblin.android.various.utils.openUrl
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.CommandCopyView
import com.moblin.android.view.utils.ExternalButtonView
import com.moblin.android.view.utils.TextButtonView
import java.io.File
import java.net.URI

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
    return null
}

private fun openInFilesApp(sharedUrl: URI) {
    openUrl(sharedUrl.toString())
}

private fun openInFinder(path: URI) {
    openUrl(path.toString())
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

@Composable
private fun HelpView(
    presentingHelp: Boolean,
    onPresentingHelpChange: (Boolean) -> Unit,
) {
    Form(
        title = localized("Help"),
        toolbar = {
            CloseToolbar(
                presenting = presentingHelp,
                onPresentingChange = onPresentingHelpChange,
            )
        },
    ) {
        Section {
            Text(
                localized(
                    "Recordings are saved as variable frame rate (VFR) fragmented MP4 to be " +
                        "resilient against crashes and other unexpected errors. Converting them " +
                        "to constant frame rate (CFR) standard MP4 can improve compatibility " +
                        "with video players and video editing software.",
                ),
            )
        }
        Section(header = localized("How to convert a recording to standard MP4")) {
            CommandCopyView(command = ffmpegCommand)
        }
        Section(header = localized("How to fix audio and video slowly desynchronizing")) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CommandCopyView(command = ffmpegAudioAndVideoSlowlyDesynchronizingCommand)
                Text("")
                Text(localized("Replace sample rates and audio codec to match your recording."))
            }
        }
        Section(
            header = localized(
                "How to convert a recording to constant frame rate (CFR) standard MP4",
            ),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CommandCopyView(command = ffmpegConstantFrameRateCommand)
                Text("")
                Text(
                    localized(
                        "Replace `hevc_videotoolbox` with your preferred encoder, typically a " +
                            "hardware encoder for faster conversion.",
                    ),
                )
            }
        }
    }
}

@Composable
fun RecordingsSettingsView(model: Model = LocalModel.current) {
    var presentingHelp by remember { mutableStateOf(false) }
    val stream by model.stream.collectAsState()

    Form(title = localized("Recordings")) {
        FilesLocationView(
            model = model,
            text = localized("Default recordings directory"),
            path = model.recordingsStorage.defaultStorageDirectory().toURI(),
        )
        val recordingPath = stream.recording.recordingPath
        if (recordingPath != null) {
            val path = makeRecordingPath(recordingPath)
            if (path != null) {
                FilesLocationView(
                    model = model,
                    text = localized("Current recordings directory"),
                    path = File(path).toURI(),
                )
            } else {
                FormRow {
                    Text(localized("Current recordings directory unavailable"))
                }
            }
        }
        FilesLocationView(
            model = model,
            text = localized("Replays directory"),
            path = model.replaysStorage.defaultStorageDirectory().toURI(),
        )
        Section {
            TextButtonView(
                title = localized("Help"),
                action = { presentingHelp = true },
            )
        }
    }

    Sheet(onDismissRequest = { presentingHelp = false }) {
        HelpView(
            presentingHelp = presentingHelp,
            onPresentingHelpChange = { presentingHelp = it },
        )
    }
}
