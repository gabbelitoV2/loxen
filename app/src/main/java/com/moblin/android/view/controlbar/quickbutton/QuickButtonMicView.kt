package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Mic
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.manualSelectMicById
import com.moblin.android.various.model.updateMicsListAsync
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsMicsMic
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickButtonMicMicView(
    model: Model = LocalModel.current,
    mic: SettingsMicsMic,
    modelMic: Mic,
    deleteEnabled: Boolean,
    onDelete: () -> Unit,
) {
    val currentMic by modelMic.current.collectAsState()
    val micConnected by mic.connected.collectAsState()

    FormRow(onClick = null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        model.updateMicsListAsync {
                            if (micConnected) {
                                model.manualSelectMicById(mic.id)
                            }
                        }
                    },
                    onLongClick = {
                        if (deleteEnabled) {
                            onDelete()
                        }
                    },
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DraggableItemPrefixView()
            SystemImage(
                name = if (micConnected) "cable.connector" else "cable.connector.slash",
                fontSize = 17.sp,
            )
            Text(
                text = mic.name,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            SystemImage(
                name = "checkmark",
                fontSize = 17.sp,
                tint = if (mic == currentMic) formPalette().accent else Color.Transparent,
            )
        }
    }
}

@Composable
fun QuickButtonMicView(
    model: Model = LocalModel.current,
    mics: SettingsMics,
    modelMic: Mic,
) {
    val micsList by mics.mics.collectAsState()
    val autoSwitch by mics.autoSwitch.collectAsState()
    val currentMic by modelMic.current.collectAsState()

    LaunchedEffect(Unit) {
        model.updateMicsListAsync()
    }

    Form(title = "Mic") {
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("Highest priority mic at the top of the list."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a mic"))
                }
            },
        ) {
            micsList.forEach { mic ->
                key(mic.id) {
                    QuickButtonMicMicView(
                        model = model,
                        mic = mic,
                        modelMic = modelMic,
                        deleteEnabled = mic != currentMic,
                        onDelete = {
                            mics._mics.value = mics._mics.value.filterNot { it.id == mic.id }
                        },
                    )
                }
            }
        }
        if (false) {
            Section(
                footerContent = {
                    Text(localized("Automatically switch to highest priority mic when plugged in."))
                },
            ) {
                Toggle(
                    title = "Auto switch",
                    isOn = autoSwitch,
                    onChange = { mics._autoSwitch.value = it },
                )
            }
        }
    }
}
