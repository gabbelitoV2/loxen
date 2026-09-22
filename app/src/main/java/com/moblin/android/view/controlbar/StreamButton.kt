package com.moblin.android.view.controlbar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.Show
import com.moblin.android.various.model.ShowingPanel
import com.moblin.android.various.settings.Database
import com.moblin.android.view.settings.streams.stream.StreamWizardSettingsView

@Composable
private fun StreamButtonText(database: Database, text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = Color.White,
        modifier = Modifier
            .widthIn(min = 60.dp)
            .padding(5.dp)
            .background(database.streamButtonColorColor)
            .clip(RoundedCornerShape(10.dp)),
    )
}

@Composable
private fun EndButtonView(
    model: Model,
    presentingGoLiveNotificationConfirm: Boolean,
    onPresentingGoLiveNotificationConfirmChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val database by model.database.collectAsState()
    val stream by model.stream.collectAsState()
    var presentingStopConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .border(BorderStroke(1.dp, Color.White), RoundedCornerShape(10.dp))
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
        AlertDialog(
            onDismissRequest = {
                onPresentingGoLiveNotificationConfirmChange(false)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPresentingGoLiveNotificationConfirmChange(false)
                        model.sendGoLiveNotification()
                    },
                ) {
                    Text(localized("Send Go live notification"))
                }
            },
        )
    }

    if (presentingStopConfirm) {
        AlertDialog(
            onDismissRequest = {
                presentingStopConfirm = false
            },
            confirmButton = {
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
            },
        )
    }
}

@Composable
private fun GoLiveButtonView(
    model: Model,
    presentingGoLiveNotificationConfirm: Boolean,
    onPresentingGoLiveNotificationConfirmChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val database by model.database.collectAsState()
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
        AlertDialog(
            onDismissRequest = {
                presentingGoLiveConfirm = false
            },
            title = {
                Text(localized("You are about to go live to '${stream.name}'!"))
            },
            confirmButton = {
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
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupButtonView(model: Model, createStreamWizard: CreateStreamWizard) {
    val database by model.database.collectAsState()
    val presentingSetup by createStreamWizard.presentingSetup.collectAsState()

    Box(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    model.resetWizard()
                    createStreamWizard.presentingSetup.value = true
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
                createStreamWizard.presentingSetup.value = false
            },
        ) {
            StreamWizardSettingsView(model = model, createStreamWizard = createStreamWizard)
        }
    }
}

@Composable
fun StreamButton(model: Model, show: Show) {
    val isLive by model.isLive.collectAsState()
    val chatPhone by show.chatPhone.collectAsState()
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
    } else if (model.isStreamConfigured()) {
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
