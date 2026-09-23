package com.moblin.android.view.controlbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Show
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.model.isGoLiveNotificationConfigured
import com.moblin.android.various.model.isStreamConfigured
import com.moblin.android.various.model.resetWizard
import com.moblin.android.various.model.sendGoLiveNotification
import com.moblin.android.various.model.startStream
import com.moblin.android.various.model.stopStream
import com.moblin.android.various.settings.Database
import com.moblin.android.view.settings.streams.stream.StreamWizardSettingsView
import kotlinx.coroutines.delay

@Composable
private fun MinimumScaleText(
    text: String,
    fontSize: TextUnit,
    minimumScaleFactor: Float,
    maxLines: Int,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    var scale by remember(text, fontSize) { mutableFloatStateOf(1f) }
    var ready by remember(text, fontSize) { mutableStateOf(false) }
    Text(
        text = text,
        color = color,
        fontSize = fontSize * scale,
        maxLines = maxLines,
        softWrap = maxLines > 1,
        textAlign = textAlign,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scale > minimumScaleFactor) {
                scale = maxOf(minimumScaleFactor, scale - 0.05f)
            } else {
                ready = true
            }
        },
        modifier = modifier.drawWithContent {
            if (ready) {
                drawContent()
            }
        },
    )
}

@Composable
private fun ConfirmationDialog(
    title: String?,
    onDismiss: () -> Unit,
    buttons: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = if (title != null) {
            { Text(title) }
        } else {
            null
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                buttons()
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(localized("Cancel"))
            }
        },
    )
}

@Composable
private fun StreamButtonText(database: Database, text: String) {
    MinimumScaleText(
        text = text,
        fontSize = 17.sp,
        minimumScaleFactor = 0.5f,
        maxLines = 1,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(database.streamButtonColorColor)
            .padding(5.dp)
            .widthIn(min = 60.dp),
    )
}

@Composable
private fun EndButtonView(
    model: Model = LocalModel.current,
    presentingGoLiveNotificationConfirm: Boolean,
    onPresentingGoLiveNotificationConfirmChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val database = model.database
    val stream by model.stream.collectAsState()
    var presentingStopConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .border(1.dp, Color.White, RoundedCornerShape(10.dp))
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                presentingStopConfirm = true
                            },
                            onLongPress = {
                                model.toggleShowingPanel(
                                    type = null,
                                    panel = ShowingPanel.streamingButtonSettings,
                                )
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        StreamButtonText(database = database, text = localized("End"))
    }

    if (presentingGoLiveNotificationConfirm) {
        ConfirmationDialog(
            title = null,
            onDismiss = {
                onPresentingGoLiveNotificationConfirmChange(false)
            },
        ) {
            TextButton(
                onClick = {
                    onPresentingGoLiveNotificationConfirmChange(false)
                    model.sendGoLiveNotification()
                },
            ) {
                Text(localized("Send Go live notification"))
            }
        }
    }

    if (presentingStopConfirm) {
        ConfirmationDialog(
            title = null,
            onDismiss = {
                presentingStopConfirm = false
            },
        ) {
            if (stream.obsAutoStopStream && stream.obsAutoStopRecording) {
                TextButton(
                    onClick = {
                        presentingStopConfirm = false
                        model.stopStream(
                            stopObsStreamIfEnabled = false,
                            stopObsRecordingIfEnabled = false,
                        )
                    },
                ) {
                    Text(localized("End but leave OBS streaming and recording"))
                }
            } else if (stream.obsAutoStopStream) {
                TextButton(
                    onClick = {
                        presentingStopConfirm = false
                        model.stopStream(stopObsStreamIfEnabled = false)
                    },
                ) {
                    Text(localized("End but leave OBS streaming"))
                }
            } else if (stream.obsAutoStopRecording) {
                TextButton(
                    onClick = {
                        presentingStopConfirm = false
                        model.stopStream(stopObsRecordingIfEnabled = false)
                    },
                ) {
                    Text(localized("End but leave OBS recording"))
                }
            }
            TextButton(
                onClick = {
                    presentingStopConfirm = false
                    model.stopStream()
                },
            ) {
                Text(localized("End"))
            }
        }
    }
}

@Composable
private fun GoLiveButtonView(
    model: Model = LocalModel.current,
    presentingGoLiveNotificationConfirm: Boolean,
    onPresentingGoLiveNotificationConfirmChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val database = model.database
    val stream by model.stream.collectAsState()
    var presentingGoLiveConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.then(
            if (enabled) {
                Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            presentingGoLiveConfirm = true
                        },
                        onLongPress = {
                            model.toggleShowingPanel(
                                type = null,
                                panel = ShowingPanel.streamingButtonSettings,
                            )
                        },
                    )
                }
            } else {
                Modifier
            },
        ),
    ) {
        StreamButtonText(database = database, text = localized("Go Live"))
    }

    if (presentingGoLiveConfirm) {
        ConfirmationDialog(
            title = localized("You are about to go live to '${stream.name}'!"),
            onDismiss = {
                presentingGoLiveConfirm = false
            },
        ) {
            TextButton(
                onClick = {
                    presentingGoLiveConfirm = false
                    model.startStream()
                    if (model.isGoLiveNotificationConfigured()) {
                        onPresentingGoLiveNotificationConfirmChange(true)
                    }
                },
            ) {
                Text(localized("Go Live"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupButtonView(model: Model = LocalModel.current, createStreamWizard: CreateStreamWizard) {
    val database = model.database
    var presentingSetup by remember { mutableStateOf(createStreamWizard.presentingSetup) }

    LaunchedEffect(presentingSetup) {
        while (presentingSetup) {
            delay(100)
            if (!createStreamWizard.presentingSetup) {
                presentingSetup = false
            }
        }
    }

    Box(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    model.resetWizard()
                    createStreamWizard.presentingSetup = true
                    presentingSetup = true
                },
                onLongPress = {
                    model.toggleShowingPanel(
                        type = null,
                        panel = ShowingPanel.streamingButtonSettings,
                    )
                },
            )
        },
    ) {
        StreamButtonText(database = database, text = localized("Setup"))
    }

    if (presentingSetup) {
        ModalBottomSheet(
            onDismissRequest = {
                createStreamWizard.presentingSetup = false
                presentingSetup = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            StreamWizardSettingsView(model = model, createStreamWizard = createStreamWizard)
        }
    }
}

@Composable
fun StreamButton(model: Model = LocalModel.current, show: Show) {
    val isLive by model.isLive.collectAsState()
    val stream by model.stream.collectAsState()
    val chatPhone by show.chatPhone.collectAsState()
    val streamConfigured = remember(stream) { model.isStreamConfigured() }
    var presentingGoLiveNotificationConfirm by remember { mutableStateOf(false) }

    if (isLive) {
        Box(modifier = Modifier.alpha(if (chatPhone) 0.5f else 1f)) {
            EndButtonView(
                model = model,
                presentingGoLiveNotificationConfirm = presentingGoLiveNotificationConfirm,
                onPresentingGoLiveNotificationConfirmChange = {
                    presentingGoLiveNotificationConfirm = it
                },
                enabled = !chatPhone,
            )
        }
    } else if (streamConfigured) {
        Box(modifier = Modifier.alpha(if (chatPhone) 0.5f else 1f)) {
            GoLiveButtonView(
                model = model,
                presentingGoLiveNotificationConfirm = presentingGoLiveNotificationConfirm,
                onPresentingGoLiveNotificationConfirmChange = {
                    presentingGoLiveNotificationConfirm = it
                },
                enabled = !chatPhone,
            )
        }
    } else {
        SetupButtonView(
            model = model,
            createStreamWizard = model.createStreamWizard,
        )
    }
}
