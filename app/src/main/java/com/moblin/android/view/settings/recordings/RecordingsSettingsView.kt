package com.moblin.android.view.settings.recordings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.moblin.android.AppDelegate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.uikit.UIApplication
import com.moblin.android.platform.uikit.canOpenURL
import com.moblin.android.platform.uikit.open
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

private val ffmpegCommand = "ffmpeg -i input.mp4 -c copy output.mp4"

private val ffmpegConstantFrameRateCommand =
    "ffmpeg -i input.mp4 -vf \"fps=60\" -c:v hevc_videotoolbox -c:a copy output.mp4"

private val ffmpegAudioAndVideoSlowlyDesynchronizingCommand =
    "ffmpeg -i input.mp4 -af \"asetrate=48002.2,aresample=48000\" -c:v copy -c:a aac output.mp4"

private fun makeSharedUrl(path: URI): String? {
    val sharedUrl = "shareddocuments://${path.path}"
    return if (UIApplication.shared.canOpenURL(sharedUrl)) {
        sharedUrl
    } else {
        null
    }
}

private fun openInFilesApp(sharedUrl: String) {
    UIApplication.shared.open(sharedUrl)
}

private fun openInFinder(model: Model, path: URI) {
    UIApplication.shared.open(path) { success ->
        if (!success) {
            copyPathToClipboard(model = model, path = path)
        }
    }
}

private fun copyPathToClipboard(model: Model, path: URI) {
    AppDelegate.context.getSystemService(ClipboardManager::class.java)
        ?.setPrimaryClip(ClipData.newPlainText(null, path.path))
    val subTitle: String? = if (isMac()) {
        localized("Open it in Finder app → Go → Go to Folder...")
    } else {
        null
    }
    model.makeToast(title = localized("Directory copied to clipboard"), subTitle = subTitle)
}

@Composable
fun FilesLocationView(model: Model = LocalModel.current, text: String, path: URI) {
    ExternalButtonView(action = {
        if (isMac()) {
            openInFinder(model = model, path = path)
        } else {
            val sharedUrl = makeSharedUrl(path = path)
            if (sharedUrl != null) {
                openInFilesApp(sharedUrl = sharedUrl)
            } else {
                copyPathToClipboard(model = model, path = path)
            }
        }
    }) {
        Text(localized(text))
    }
}

@Composable
private fun HelpView(presentingHelp: MutableState<Boolean>) {
    Form(title = "Help", toolbar = {
        CloseToolbar(presenting = presentingHelp.value, onPresentingChange = { presentingHelp.value = it })
    }) {
        Section {
            Text(
                localized(
                    "Recordings are saved as variable frame rate (VFR) fragmented MP4 to be resilient against crashes and other unexpected errors. Converting them to constant frame rate (CFR) standard MP4 can improve compatibility with video players and video editing software."
                )
            )
        }
        Section(header = "How to convert a recording to standard MP4") {
            CommandCopyView(command = ffmpegCommand)
        }
        Section(header = "How to fix audio and video slowly desynchronizing") {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CommandCopyView(command = ffmpegAudioAndVideoSlowlyDesynchronizingCommand)
                Text(localized(""))
                Text(localized("Replace sample rates and audio codec to match your recording."))
            }
        }
        Section(header = "How to convert a recording to constant frame rate (CFR) standard MP4") {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CommandCopyView(command = ffmpegConstantFrameRateCommand)
                Text(localized(""))
                Text(
                    localized(
                        "Replace `hevc_videotoolbox` with your preferred encoder, typically a hardware encoder for faster conversion."
                    )
                )
            }
        }
    }
}

@Composable
fun RecordingsSettingsView(model: Model = LocalModel.current) {
    val presentingHelp = remember { mutableStateOf(false) }
    val stream by model.stream.collectAsState()
    Form(title = "Recordings") {
        FilesLocationView(
            model = model,
            text = "Default recordings directory",
            path = model.recordingsStorage.defaultStorageDirectory().toURI(),
        )
        val recordingPath = stream.recording.recordingPath
        if (recordingPath != null) {
            val path = makeRecordingPath(recordingPath = recordingPath)
            if (path != null) {
                FilesLocationView(
                    model = model,
                    text = "Current recordings directory",
                    path = File(path).toURI(),
                )
            } else {
                Section { Text(localized("Current recordings directory unavailable")) }
            }
        }
        FilesLocationView(
            model = model,
            text = "Replays directory",
            path = model.replaysStorage.defaultStorageDirectory().toURI(),
        )
        Section {
            TextButtonView("Help") {
                presentingHelp.value = true
            }
            Sheet(isPresented = presentingHelp) {
                HelpView(presentingHelp = presentingHelp)
            }
        }
    }
}
