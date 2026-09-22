package com.moblin.android.view.settings.streams.stream.replay

import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamReplay
import com.moblin.android.various.settings.SettingsStreamReplayStinger
import com.moblin.android.various.settings.SettingsStreamReplayTransitionType
import com.moblin.android.view.settings.scenes.widgets.widget.AlignmentOptionView
import com.moblin.android.view.settings.scenes.widgets.widget.SaveLoadLayoutView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.CommandCopyView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.PositionEditView
import com.moblin.android.view.utils.SizeEditView
import com.moblin.android.view.utils.TextButtonView
import java.io.File
import com.moblin.android.LocalModel

@Composable
private fun VideoPickerView(model: Model = LocalModel.current, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                val name = uri.lastPathSegment ?: return@runCatching
                val file = File(context.filesDir, name)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                model.onDocumentPickerUrl?.invoke(file.toURI().toString())
            }
        }
        onDismiss()
    }
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf("video/*"))
    }
}

private const val ffmpegCommand =
    "ffmpeg -c:v libvpx-vp9 -i input.webm -c:v hevc_videotoolbox -alpha_quality 1 -vtag hvc1 output.mov"

@Composable
private fun HelpView(presentingHelp: Boolean, onPresentingHelpChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CloseToolbar(
            presenting = presentingHelp,
            onPresentingChange = onPresentingHelpChange,
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = localized("How to convert `.webm` (VP9) to `.mov` (HEVC) with alpha channel"),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(localized("NOTE: Only works on Mac as `hevc_videotoolbox` uses Apple’s encoder."))
            Text("")
            CommandCopyView(command = ffmpegCommand)
        }
    }
}

@Composable
private fun StingerView(
    model: Model = LocalModel.current,
    title: String,
    stinger: SettingsStreamReplayStinger,
) {
    var showPicker by remember { mutableStateOf(false) }
    var presentingHelp by remember { mutableStateOf(false) }
    var showDetail by remember { mutableStateOf(false) }

    fun onUrl(url: String) {
        stinger.name = url.substringAfterLast('/')
        val filename = stinger.makeFilename()
        if (filename != null) {
            model.replayTransitionsStorage.remove(filename = filename)
            model.replayTransitionsStorage.add(filename = filename, url = File(url))
        }
    }

    if (showDetail) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = localized(title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                TextButton(
                    onClick = {
                        showPicker = true
                        model.onDocumentPickerUrl = { url -> onUrl(url) }
                    },
                ) {
                    HCenter {
                        if (stinger.name.isEmpty()) {
                            Text(localized("Select video"))
                        } else {
                            Text(stinger.name)
                        }
                    }
                }
                if (showPicker) {
                    VideoPickerView(
                        model = model,
                        onDismiss = { showPicker = false },
                    )
                }
                Text(localized("Use the HEVC/H.265 codec with alpha channel for transparent background."))
                TextButtonView(
                    title = localized("Help"),
                    action = { presentingHelp = true },
                )
                if (presentingHelp) {
                    HelpView(
                        presentingHelp = presentingHelp,
                        onPresentingHelpChange = { presentingHelp = it },
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDetail = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(localized(title))
            Spacer(modifier = Modifier.weight(1f))
            GrayTextView(text = stinger.name)
        }
    }
}

@Composable
private fun LayoutView(
    model: Model = LocalModel.current,
    database: Database,
    replay: SettingsStreamReplay,
) {
    val layout = remember { mutableStateOf(replay.layout) }

    fun dimensions(): Size {
        return model.stream.value.resolution.dimensions(model.stream.value.portrait)
    }

    fun horizontalIncrement(): Double {
        return 100.0 / dimensions().width
    }

    fun verticalIncrement(): Double {
        return 100.0 / dimensions().height
    }

    fun setXBasedOnYIfLocked() {
        if (!replay.layout.positioningLock) {
            return
        }
        replay.layout.x = replay.layout.y * horizontalIncrement() / verticalIncrement()
        replay.layout.xString = replay.layout.x.toString()
    }

    fun setYBasedOnXIfLocked() {
        if (!replay.layout.positioningLock) {
            return
        }
        replay.layout.y = replay.layout.x * verticalIncrement() / horizontalIncrement()
        replay.layout.yString = replay.layout.y.toString()
    }

    @Composable
    fun generalAndAlignmentPicker() {
        Row {
            Row(modifier = Modifier.weight(1f)) {
                SaveLoadLayoutView(
                    layout = layout,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            VerticalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.topLeft,
                    )
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.topCenter,
                    )
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.topRight,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.leftCenter,
                    )
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.center,
                    )
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.rightCenter,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.bottomLeft,
                    )
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.bottomCenter,
                    )
                    AlignmentOptionView(
                        layout = layout,
                        alignment = SettingsAlignment.bottomRight,
                    )
                }
            }
        }
    }

    val alignment = replay.layout.alignment
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = localized("Layout"),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(16.dp),
        )
        generalAndAlignmentPicker()
        if (!alignment.isHorizontalCenter() && !alignment.isVerticalCenter()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Column {
                    PositionEditView(
                        number = replay.layout.x,
                        value = replay.layout.xString,
                        onNumberChange = { replay.layout.x = it },
                        onValueChange = { replay.layout.xString = it },
                        onSubmit = { setYBasedOnXIfLocked() },
                        numericInput = database.sceneNumericInput,
                        onNumericInputChange = { database.sceneNumericInput = it },
                        incrementImageName = "arrow.forward.circle",
                        decrementImageName = "arrow.backward.circle",
                        mirror = alignment.mirrorPositionHorizontally(),
                        increment = horizontalIncrement(),
                    )
                    Spacer(modifier = Modifier.padding(bottom = 10.dp))
                    PositionEditView(
                        number = replay.layout.y,
                        value = replay.layout.yString,
                        onNumberChange = { replay.layout.y = it },
                        onValueChange = { replay.layout.yString = it },
                        onSubmit = { setXBasedOnYIfLocked() },
                        numericInput = database.sceneNumericInput,
                        onNumericInputChange = { database.sceneNumericInput = it },
                        incrementImageName = "arrow.down.circle",
                        decrementImageName = "arrow.up.circle",
                        mirror = alignment.mirrorPositionVertically(),
                        increment = verticalIncrement(),
                    )
                }
                TextButton(
                    onClick = {
                        replay.layout.positioningLock = !replay.layout.positioningLock
                        setYBasedOnXIfLocked()
                    },
                    modifier = Modifier.width(48.dp),
                ) {
                    Text(if (replay.layout.positioningLock) "🔒" else "🔓")
                }
            }
        } else if (!alignment.isHorizontalCenter()) {
            PositionEditView(
                number = replay.layout.x,
                value = replay.layout.xString,
                onNumberChange = { replay.layout.x = it },
                onValueChange = { replay.layout.xString = it },
                onSubmit = {},
                numericInput = database.sceneNumericInput,
                onNumericInputChange = { database.sceneNumericInput = it },
                incrementImageName = "arrow.forward.circle",
                decrementImageName = "arrow.backward.circle",
                mirror = alignment.mirrorPositionHorizontally(),
                increment = horizontalIncrement(),
            )
        } else if (!alignment.isVerticalCenter()) {
            PositionEditView(
                number = replay.layout.y,
                value = replay.layout.yString,
                onNumberChange = { replay.layout.y = it },
                onValueChange = { replay.layout.yString = it },
                onSubmit = {},
                numericInput = database.sceneNumericInput,
                onNumericInputChange = { database.sceneNumericInput = it },
                incrementImageName = "arrow.down.circle",
                decrementImageName = "arrow.up.circle",
                mirror = alignment.mirrorPositionVertically(),
                increment = verticalIncrement(),
            )
        }
        SizeEditView(
            number = replay.layout.size,
            value = replay.layout.sizeString,
            onNumberChange = { replay.layout.size = it },
            onValueChange = { replay.layout.sizeString = it },
            onSubmit = {},
            numericInput = database.sceneNumericInput,
            onNumericInputChange = { database.sceneNumericInput = it },
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = localized("Numeric input"),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = database.sceneNumericInput,
                onCheckedChange = { database.sceneNumericInput = it },
            )
        }
    }

    LaunchedEffect(
        replay.layout.alignment,
        replay.layout.x,
        replay.layout.y,
        replay.layout.size,
    ) {
        model.replayEffect?.setLayout(layout = replay.layout)
    }

    LaunchedEffect(layout.value) {
        replay.layout = layout.value
    }
}

@Composable
fun StreamReplaySettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    replay: SettingsStreamReplay,
) {
    var transitionExpanded by remember { mutableStateOf(false) }
    var postTriggerDelayExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = localized("Replay"),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = localized("Enabled"),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = replay.enabled,
                onCheckedChange = { enabled ->
                    replay.enabled = enabled
                    if (stream.enabled) {
                        Unit
                    }
                },
            )
        }
        if (database.showAllSettings) {
            LayoutView(
                model = model,
                database = database,
                replay = replay,
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = localized("Transition"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Box {
                    TextButton(onClick = { transitionExpanded = true }) {
                        Text(replay.transitionType.toString())
                    }
                    DropdownMenu(
                        expanded = transitionExpanded,
                        onDismissRequest = { transitionExpanded = false },
                    ) {
                        SettingsStreamReplayTransitionType.entries.forEach { transitionType ->
                            DropdownMenuItem(
                                text = { Text(transitionType.toString()) },
                                onClick = {
                                    replay.transitionType = transitionType
                                    transitionExpanded = false
                                },
                            )
                        }
                    }
                }
                when (replay.transitionType) {
                    SettingsStreamReplayTransitionType.fade -> Unit
                    SettingsStreamReplayTransitionType.stingers -> {
                        StingerView(
                            model = model,
                            title = "In video",
                            stinger = replay.inStinger,
                        )
                        StingerView(
                            model = model,
                            title = "Out video",
                            stinger = replay.outStinger,
                        )
                    }
                    SettingsStreamReplayTransitionType.none -> Unit
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = localized("Post trigger delay"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Box {
                    TextButton(onClick = { postTriggerDelayExpanded = true }) {
                        Text(formatShortDuration(seconds = replay.postTriggerDelay))
                    }
                    DropdownMenu(
                        expanded = postTriggerDelayExpanded,
                        onDismissRequest = { postTriggerDelayExpanded = false },
                    ) {
                        listOf(2, 3, 4, 5).forEach { delay ->
                            DropdownMenuItem(
                                text = { Text(formatShortDuration(seconds = delay)) },
                                onClick = {
                                    replay.postTriggerDelay = delay
                                    postTriggerDelayExpanded = false
                                },
                            )
                        }
                    }
                }
                Text(localized("Seconds to record after the Instant replay/Save replay button is pressed."))
            }
        }
    }
}
