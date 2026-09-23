package com.moblin.android.view.settings.streams.stream.replay

import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.streamReplayEnabledUpdated
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAlignment
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamReplay
import com.moblin.android.various.settings.SettingsStreamReplayStinger
import com.moblin.android.various.settings.SettingsStreamReplayTransitionType
import com.moblin.android.view.settings.scenes.widgets.widget.AlignmentOptionView
import com.moblin.android.view.settings.scenes.widgets.widget.SaveLoadLayoutView
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.CommandCopyView
import com.moblin.android.view.utils.PositionEditView
import com.moblin.android.view.utils.SizeEditView
import com.moblin.android.view.utils.TextButtonView
import java.io.File

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
    Form(
        title = "Help",
        toolbar = {
            CloseToolbar(
                presenting = presentingHelp,
                onPresentingChange = onPresentingHelpChange,
            )
        },
    ) {
        Section(
            header = "How to convert `.webm` (VP9) to `.mov` (HEVC) with alpha channel",
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(localized("NOTE: Only works on Mac as `hevc_videotoolbox` uses Apple’s encoder."))
                Text("")
                CommandCopyView(command = ffmpegCommand)
            }
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

    fun onUrl(url: String) {
        stinger.name = url.substringAfterLast('/')
        val filename = stinger.makeFilename()
        if (filename != null) {
            model.replayTransitionsStorage.remove(filename = filename)
            model.replayTransitionsStorage.add(filename = filename, url = File(url))
        }
    }

    NavigationLink(
        destination = {
            Form(title = title) {
                Section(
                    footer = "Use the HEVC/H.265 codec with alpha channel for transparent background.",
                ) {
                    FormButton(
                        title = if (stinger.name.isEmpty()) "Select video" else stinger.name,
                        centered = true,
                    ) {
                        showPicker = true
                        model.onDocumentPickerUrl = { url -> onUrl(url) }
                    }
                }
                Section {
                    TextButtonView(
                        title = localized("Help"),
                        action = { presentingHelp = true },
                    )
                }
            }
            if (showPicker) {
                Sheet(onDismissRequest = { showPicker = false }) {
                    VideoPickerView(
                        model = model,
                        onDismiss = { showPicker = false },
                    )
                }
            }
            if (presentingHelp) {
                Sheet(onDismissRequest = { presentingHelp = false }) {
                    HelpView(
                        presentingHelp = presentingHelp,
                        onPresentingHelpChange = { presentingHelp = it },
                    )
                }
            }
        },
    ) {
        Text(localized(title))
        Spacer(modifier = Modifier.weight(1f))
        GrayTextView(text = stinger.name)
    }
}

@Composable
private fun LayoutView(
    model: Model = LocalModel.current,
    database: Database,
    replay: SettingsStreamReplay,
) {
    val layout = binding(get = { replay.layout }, set = { replay.layout = it })

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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                SaveLoadLayoutView(
                    layout = layout,
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(formPalette().separator),
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
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
    Section(header = "Layout") {
        generalAndAlignmentPicker()
        if (!alignment.isHorizontalCenter() && !alignment.isVerticalCenter()) {
            val lockInteractionSource = remember { MutableInteractionSource() }
            val lockPressed by lockInteractionSource.collectIsPressedAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.height(18.dp))
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
                Box(
                    modifier = Modifier
                        .width(35.dp)
                        .alpha(if (lockPressed) 0.2f else 1f)
                        .clickable(
                            interactionSource = lockInteractionSource,
                            indication = null,
                        ) {
                            replay.layout.positioningLock = !replay.layout.positioningLock
                            setYBasedOnXIfLocked()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    SystemImage(
                        name = if (replay.layout.positioningLock) "lock" else "lock.open",
                        fontSize = 28.sp,
                    )
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
        Toggle(
            title = "Numeric input",
            isOn = binding(
                get = { database.sceneNumericInput },
                set = { database.sceneNumericInput = it },
            ),
        )
    }

    LaunchedEffect(
        replay.layout.alignment,
        replay.layout.x,
        replay.layout.y,
        replay.layout.size,
    ) {
        model.replayEffect?.setLayout(layout = replay.layout)
    }
}

@Composable
fun StreamReplaySettingsView(
    model: Model = LocalModel.current,
    database: Database,
    stream: SettingsStream,
    replay: SettingsStreamReplay,
) {
    Form(title = "Replay") {
        Section {
            Toggle(
                title = "Enabled",
                isOn = replay.enabled,
                onChange = { enabled ->
                    replay.enabled = enabled
                    if (stream.enabled) {
                        model.streamReplayEnabledUpdated()
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
            Section {
                Picker(
                    title = "Transition",
                    selection = replay.transitionType,
                    options = SettingsStreamReplayTransitionType.entries,
                    text = { it.toString() },
                ) { transitionType ->
                    replay.transitionType = transitionType
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
            Section(
                footer = "Seconds to record after the Instant replay/Save replay button is pressed.",
            ) {
                Picker(
                    title = "Post trigger delay",
                    selection = replay.postTriggerDelay,
                    options = listOf(2, 3, 4, 5),
                    text = { formatShortDuration(seconds = it) },
                ) { delay ->
                    replay.postTriggerDelay = delay
                }
            }
        }
    }
}
