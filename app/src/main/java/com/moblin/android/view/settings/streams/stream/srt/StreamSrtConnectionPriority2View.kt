package com.moblin.android.view.settings.streams.stream.srt

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamDetailedProtocol
import com.moblin.android.various.settings.SettingsStreamSrtConnectionPriority
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView

val minimumSrtConnectionPriority = 1
val maximumSrtConnectionPriority = 10

fun clampConnectionPriority(value: Int): Int {
    return value.coerceIn(minimumSrtConnectionPriority, maximumSrtConnectionPriority)
}

private fun Model.updateSrtlaPriorities() {
    Unit
}

@Composable
private fun NoConnectionPrioritiesView(protocolName: String) {
    Form(title = "Connection priorities") {
        Section {
            Text(
                localized(
                    "Connection priorities are not supported by $protocolName. Only SRTLA " +
                        "supports connection priorities.",
                ),
            )
        }
    }
}

@Composable
private fun PriorityItemView(
    model: Model = LocalModel.current,
    priority: SettingsStreamSrtConnectionPriority,
    initialPrio: Float,
) {
    var prio by remember(priority) { mutableStateOf(initialPrio) }
    val enabled = binding(
        get = { priority.enabled },
        set = { value ->
            priority.enabled = value
            model.updateSrtlaPriorities()
        },
    )

    fun makeName(): String {
        if (priority.relayId != null) {
            return priority.name
        }
        val name = model.database.networkInterfaceNames
            .firstOrNull { it.interfaceName == priority.name }
            ?.name
        return if (name != null && name.isNotEmpty()) {
            name
        } else {
            priority.name
        }
    }

    Toggle(
        isOn = enabled.value,
        onChange = { value -> enabled.value = value },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.width(90.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(makeName())
            }
            FormSlider(
                value = prio,
                onValueChange = { value -> prio = value },
                modifier = Modifier.weight(1f),
                valueRange = minimumSrtConnectionPriority.toFloat()..
                    maximumSrtConnectionPriority.toFloat(),
                onValueChangeFinished = {
                    priority.priority = clampConnectionPriority(prio.toInt())
                    model.updateSrtlaPriorities()
                },
            )
        }
    }
}

private fun deletePriority(
    model: Model,
    stream: SettingsStream,
    offsets: List<Int>,
) {
    offsets.sortedDescending().forEach { index ->
        if (index in stream.srt.connectionPriorities.priorities.indices) {
            stream.srt.connectionPriorities.priorities.removeAt(index)
        }
    }
    model.updateSrtlaPriorities()
}

@Composable
private fun SrtlaConnectionPriorityView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    Form(title = "Connection priorities") {
        Section {
            Toggle(
                "Enabled",
                isOn = binding(
                    get = { stream.srt.connectionPriorities.enabled },
                    set = { value ->
                        stream.srt.connectionPriorities.enabled = value
                        model.updateSrtlaPriorities()
                    },
                ),
            )
        }
        Section(footerContent = {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    localized(
                        "A connection with high priority will be used more than a connection " +
                            "with low priority if the high priority connection is stable. " +
                            "Unstable connections will get lowest priority regardless of " +
                            "configured priority until they are stable again.",
                    ),
                )
                Text("")
                Text(
                    localized("Disabled connections will not be used."),
                )
                Text("")
                SwipeLeftToDeleteHelpView(kind = localized("a connection"))
            }
        }) {
            stream.srt.connectionPriorities.priorities.forEach { priority ->
                val deleteDisabled = priority.name == "Cellular" || priority.name == "WiFi"
                key(priority.id) {
                    Box(
                        modifier = Modifier.pointerInput(priority.id, deleteDisabled) {
                            detectTapGestures(
                                onLongPress = {
                                    if (!deleteDisabled) {
                                        val index = stream.srt.connectionPriorities.priorities
                                            .indexOfFirst { it.id == priority.id }
                                        if (index != -1) {
                                            deletePriority(model, stream, listOf(index))
                                        }
                                    }
                                },
                            )
                        },
                    ) {
                        PriorityItemView(
                            model = model,
                            priority = priority,
                            initialPrio = priority.priority.toFloat(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StreamSrtConnectionPriorityView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    when (stream.getDetailedProtocol()) {
        SettingsStreamDetailedProtocol.srtla -> SrtlaConnectionPriorityView(
            model = model,
            stream = stream,
        )
        else -> NoConnectionPrioritiesView(protocolName = stream.protocolString())
    }
}
