package com.moblin.android.view.settings.streamdeck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.StreamDeck
import com.moblin.android.various.model.setSelectedStreamDeck
import com.moblin.android.various.settings.SettingsControllerFunction
import com.moblin.android.various.settings.SettingsStreamDeckKey
import com.moblin.android.various.settings.SettingsStreamDeckLayout
import com.moblin.android.various.settings.SettingsStreamDeckModel
import com.moblin.android.various.settings.SettingsStreamDecks
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.catprinters.IntegrationImageView
import com.moblin.android.view.settings.gamecontrollers.ControllerButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.NameEditView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("UNCHECKED_CAST")
private fun <T> StateFlow<T>.setFlowValue(value: T) {
    (this as? MutableStateFlow<T>)?.value = value
}

private fun functions(): List<SettingsControllerFunction> {
    return SettingsControllerFunction.entries.filter {
        it.rawValue != "zoomIn" && it.rawValue != "zoomOut"
    }
}

@Composable
private fun StreamDeckSettingsKeyView(model: Model = LocalModel.current, key: SettingsStreamDeckKey) {
    val text by key.text.collectAsState()
    val colorColor by key.colorColor.collectAsState()
    val function by key.function.collectAsState()
    val functionData by key.functionData.collectAsState()
    Column {
        ControllerButtonView(
            model = model,
            functions = functions(),
            function = function,
            onFunctionChange = { key.function.setFlowValue(it) },
            functionData = functionData,
            onFunctionDataChange = { key.functionData.setFlowValue(it) },
        )
        TextEditNavigationView(
            title = "Text",
            value = text,
            onSubmit = { key.text.setFlowValue(it) },
        )
        Unit
        LaunchedEffect(colorColor) {
            Unit
        }
    }
}

@Composable
private fun StreamDeckKeyView(
    key: SettingsStreamDeckKey,
    index: Int,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    size: Float,
) {
    val colorColor by key.colorColor.collectAsState()
    val text by key.text.collectAsState()
    val shape = RoundedCornerShape((size / 10).dp)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(shape)
            .background(colorColor)
            .border(
                width = if (index == selectedIndex) 5.dp else 2.dp,
                color = Color.Blue,
                shape = shape,
            )
            .clickable { onSelectedIndexChange(index) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private const val streamDeckMiniViewSize = 55.0f
private const val streamDeckClassicViewSize = 45.0f
private const val streamDeckXlViewSize = 30.0f

@Composable
private fun streamDeckKeyView(
    keys: List<SettingsStreamDeckKey>,
    index: Int,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    size: Float,
) {
    StreamDeckKeyView(
        key = keys[index],
        index = index,
        selectedIndex = selectedIndex,
        onSelectedIndexChange = onSelectedIndexChange,
        size = size,
    )
}

@Composable
private fun StreamDeckMiniView(
    keys: List<SettingsStreamDeckKey>,
    onKeysChange: (List<SettingsStreamDeckKey>) -> Unit,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    Column {
        HCenter {
            streamDeckKeyView(keys, 0, selectedIndex, onSelectedIndexChange, streamDeckMiniViewSize)
            streamDeckKeyView(keys, 1, selectedIndex, onSelectedIndexChange, streamDeckMiniViewSize)
            streamDeckKeyView(keys, 2, selectedIndex, onSelectedIndexChange, streamDeckMiniViewSize)
        }
        HCenter {
            streamDeckKeyView(keys, 3, selectedIndex, onSelectedIndexChange, streamDeckMiniViewSize)
            streamDeckKeyView(keys, 4, selectedIndex, onSelectedIndexChange, streamDeckMiniViewSize)
            streamDeckKeyView(keys, 5, selectedIndex, onSelectedIndexChange, streamDeckMiniViewSize)
        }
    }
}

@Composable
private fun StreamDeckClassicView(
    keys: List<SettingsStreamDeckKey>,
    onKeysChange: (List<SettingsStreamDeckKey>) -> Unit,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    Column {
        HCenter {
            streamDeckKeyView(keys, 0, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 1, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 2, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 3, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 4, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
        }
        HCenter {
            streamDeckKeyView(keys, 5, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 6, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 7, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 8, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 9, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
        }
        HCenter {
            streamDeckKeyView(keys, 10, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 11, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 12, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 13, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
            streamDeckKeyView(keys, 14, selectedIndex, onSelectedIndexChange, streamDeckClassicViewSize)
        }
    }
}

@Composable
private fun StreamDeckXlView(
    keys: List<SettingsStreamDeckKey>,
    onKeysChange: (List<SettingsStreamDeckKey>) -> Unit,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
) {
    Column {
        HCenter {
            streamDeckKeyView(keys, 0, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 1, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 2, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 3, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 4, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 5, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 6, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 7, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
        }
        HCenter {
            streamDeckKeyView(keys, 8, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 9, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 10, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 11, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 12, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 13, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 14, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 15, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
        }
        HCenter {
            streamDeckKeyView(keys, 16, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 17, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 18, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 19, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 20, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 21, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 22, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 23, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
        }
        HCenter {
            streamDeckKeyView(keys, 24, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 25, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 26, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 27, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 28, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 29, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 30, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
            streamDeckKeyView(keys, 31, selectedIndex, onSelectedIndexChange, streamDeckXlViewSize)
        }
    }
}

@Composable
private fun StreamDeckLayoutSettingsView(
    model: Model = LocalModel.current,
    layout: SettingsStreamDeckLayout,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val name = layout.name
    Text(
        text = name,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate("streamDeckLayout") },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamDeckLayoutSettingsDetailView(model: Model = LocalModel.current, layout: SettingsStreamDeckLayout) {
    val name = layout.name
    val deckModel by layout.model.collectAsState()
    val keys by layout.keys.collectAsState()
    var selectedIndex by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(deckModel) {
        selectedIndex = 0
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stream deck") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                NameEditView(
                    name = name,
                    onNameChange = { layout.name = it },
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = deckModel.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Model") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        SettingsStreamDeckModel.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.toString()) },
                                onClick = {
                                    layout.model.setFlowValue(entry)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                when (deckModel) {
                    SettingsStreamDeckModel.mini -> StreamDeckMiniView(
                        keys = keys,
                        onKeysChange = { layout.keys.setFlowValue(it) },
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { selectedIndex = it },
                    )
                    SettingsStreamDeckModel.classic -> StreamDeckClassicView(
                        keys = keys,
                        onKeysChange = { layout.keys.setFlowValue(it) },
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { selectedIndex = it },
                    )
                    SettingsStreamDeckModel.xl -> StreamDeckXlView(
                        keys = keys,
                        onKeysChange = { layout.keys.setFlowValue(it) },
                        selectedIndex = selectedIndex,
                        onSelectedIndexChange = { selectedIndex = it },
                    )
                }
            }
            itemsIndexed(keys) { _, key ->
                if (keys.indexOfFirst { it === key } == selectedIndex) {
                    StreamDeckSettingsKeyView(model = model, key = key)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamDecksSettingsView(
    model: Model = LocalModel.current,
    streamDeck: StreamDeck,
    streamDecks: SettingsStreamDecks,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isDeviceDriverInstalled by streamDeck.isDeviceDriverInstalled.collectAsState()
    val selectedId by streamDecks.selectedId.collectAsState()
    val layouts by streamDecks.layouts.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Stream deck") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                HCenter {
                    IntegrationImageView(imageName = "StreamDeck")
                }
            }
            if (!isDeviceDriverInstalled) {
                item {
                    Text(
                        "⚠️ Download and install Stream Deck Connect from the App Store, and then enable the Stream Deck Device Driver in its settings.",
                    )
                }
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                val selectedLayout = layouts.firstOrNull { it.id == selectedId }
                val currentName = if (selectedLayout == null) {
                    "-- None --"
                } else {
                    selectedLayout.name
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = currentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Current") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("-- None --") },
                            onClick = {
                                streamDecks.selectedId.setFlowValue(null)
                                model.setSelectedStreamDeck()
                                expanded = false
                            },
                        )
                        layouts.forEach { layout ->
                            DropdownMenuItem(
                                text = { Text(layout.name) },
                                onClick = {
                                    streamDecks.selectedId.setFlowValue(layout.id)
                                    model.setSelectedStreamDeck()
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Layouts",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            itemsIndexed(layouts, key = { _, layout -> layout.id }) { index, layout ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StreamDeckLayoutSettingsView(
                        model = model,
                        layout = layout,
                        onNavigate = onNavigate,
                    )
                    IconButton(
                        onClick = {
                            if (index > 0) {
                                val reordered = layouts.toMutableList()
                                val moved = reordered.removeAt(index)
                                reordered.add(index - 1, moved)
                                streamDecks.layouts.setFlowValue(reordered)
                            }
                        },
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }
                    IconButton(
                        onClick = {
                            if (index < layouts.size - 1) {
                                val reordered = layouts.toMutableList()
                                val moved = reordered.removeAt(index)
                                reordered.add(index + 1, moved)
                                streamDecks.layouts.setFlowValue(reordered)
                            }
                        },
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    IconButton(
                        onClick = {
                            streamDecks.layouts.setFlowValue(
                                layouts.filterIndexed { i, _ -> i != index },
                            )
                            model.setSelectedStreamDeck()
                        },
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
            item {
                CreateButtonView {
                    val streamDeck = SettingsStreamDeckLayout()
                    streamDeck.name =
                        makeUniqueName(
                            name = SettingsStreamDeckLayout.baseName,
                            existingNames = layouts,
                        )
                    streamDecks.layouts.setFlowValue(layouts + streamDeck)
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = "a layout")
            }
        }
    }
}
