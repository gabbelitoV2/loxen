package com.moblin.android.view.settings.streams.stream.srt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoConnectionPrioritiesView(protocolName: String) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(localized("Connection priorities")) })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                localized(
                    "Connection priorities are not supported by $protocolName. Only SRTLA " +
                        "supports connection priorities.",
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityItemView(
    model: Model,
    priority: SettingsStreamSrtConnectionPriority,
    initialPrio: Float,
) {
    var prio by remember(priority) { mutableStateOf(initialPrio) }

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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            text = makeName(),
            modifier = Modifier.width(90.dp),
        )
        Slider(
            value = prio,
            onValueChange = { value -> prio = value },
            valueRange = minimumSrtConnectionPriority.toFloat()..maximumSrtConnectionPriority.toFloat(),
            steps = 8,
            onValueChangeFinished = {
                priority.priority = clampConnectionPriority(prio.toInt())
                model.updateSrtlaPriorities()
            },
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = priority.enabled,
            onCheckedChange = { value ->
                priority.enabled = value
                model.updateSrtlaPriorities()
            },
        )
    }
}

private fun deletePriority(
    model: Model,
    stream: SettingsStream,
    offsets: List<Int>,
) {
    offsets.sortedDescending().forEach { index ->
        stream.srt.connectionPriorities.priorities.removeAt(index)
    }
    model.updateSrtlaPriorities()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SrtlaConnectionPriorityView(
    model: Model,
    stream: SettingsStream,
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(localized("Connection priorities")) })
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = localized("Enabled"),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = stream.srt.connectionPriorities.enabled,
                    onCheckedChange = { value ->
                        stream.srt.connectionPriorities.enabled = value
                        model.updateSrtlaPriorities()
                    },
                )
            }
            stream.srt.connectionPriorities.priorities.forEach { priority ->
                val deleteDisabled = priority.name == "Cellular" || priority.name == "WiFi"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PriorityItemView(
                            model = model,
                            priority = priority,
                            initialPrio = priority.priority.toFloat(),
                        )
                    }
                    IconButton(
                        enabled = !deleteDisabled,
                        onClick = {
                            makeOffsets(
                                stream.srt.connectionPriorities.priorities,
                                priority.id,
                            )?.let { offsets ->
                                deletePriority(model, stream, offsets.toList())
                            }
                        },
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Text(
                text = localized(
                    "A connection with high priority will be used more than a connection " +
                        "with low priority if the high priority connection is stable. " +
                        "Unstable connections will get lowest priority regardless of " +
                        "configured priority until they are stable again.",
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = localized("Disabled connections will not be used."),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            SwipeLeftToDeleteHelpView(
                kind = localized("a connection"),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
fun StreamSrtConnectionPriorityView(
    model: Model,
    stream: SettingsStream,
) {
    when (stream.getDetailedProtocol()) {
        SettingsStreamDetailedProtocol.SRTLA -> SrtlaConnectionPriorityView(
            model = model,
            stream = stream,
        )
        else -> NoConnectionPrioritiesView(protocolName = stream.protocolString())
    }
}
