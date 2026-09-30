package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.DeleteDisabled
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.moving
import com.moblin.android.platform.swiftui.removing
import com.moblin.android.various.model.Mic
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.manualSelectMicById
import com.moblin.android.various.settings.SettingsMics
import com.moblin.android.various.settings.SettingsMicsMic
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

@Composable
private fun QuickButtonMicMicView(
    model: Model = LocalModel.current,
    mic: SettingsMicsMic,
    modelMic: Mic,
) {
    val currentMic by modelMic.current.collectAsState()
    val micConnected by mic.connected.collectAsState()

    FormRow(onClick = null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (micConnected) {
                            model.manualSelectMicById(mic.id)
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
                tint = LocalContentColor.current,
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

    Form(title = "Mic") {
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localized("Highest priority mic at the top of the list."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = localized("a mic"))
                }
            },
        ) {
            ForEach(
                micsList,
                id = { it.id },
                onDelete = { offsets ->
                    mics._mics.value = mics._mics.value.removing(atOffsets = offsets)
                },
                onMove = { froms, to ->
                    mics._mics.value = mics._mics.value.moving(fromOffsets = froms, toOffset = to)
                },
            ) { mic ->
                ContextMenuDeleteButton(
                    disabled = mic == currentMic,
                    action = {
                        mics._mics.value = mics._mics.value.filterNot { it.id == mic.id }
                    },
                ) {
                    DeleteDisabled(mic == currentMic) {
                        QuickButtonMicMicView(model = model, mic = mic, modelMic = modelMic)
                    }
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
