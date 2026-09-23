package com.moblin.android.view.settings.streamdeck

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
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

private val streamDeckKeyColors = listOf(
    Color.Black,
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFFCC00),
    Color(0xFFFF9500),
    Color(0xFFFF3B30),
)

@Composable
private fun StreamDeckSettingsKeyView(model: Model = LocalModel.current, key: SettingsStreamDeckKey) {
    val function by key.function.collectAsState()
    val functionData by key.functionData.collectAsState()
    val text by key.text.collectAsState()
    val colorColor by key.colorColor.collectAsState()

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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Color", style = formBodyStyle)
        Spacer(modifier = Modifier.weight(1f))
        streamDeckKeyColors.forEach { swatch ->
            Box(
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(
                        width = if (swatch == colorColor) 2.dp else 1.dp,
                        color = if (swatch == colorColor) Color(0xFF007AFF) else Color(0x33000000),
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        key.colorColor.setFlowValue(swatch)
                    },
            )
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var fontSize by remember(text) { mutableStateOf(12f) }
    Box(
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .size(size.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                onSelectedIndexChange(index)
            }
            .clip(shape)
            .background(colorColor)
            .border(
                width = if (index == selectedIndex) 5.dp else 2.dp,
                color = Color(0xFF007AFF),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = fontSize.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            onTextLayout = { layout ->
                if (layout.hasVisualOverflow && fontSize > 6f) {
                    fontSize -= 1f
                }
            },
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
    NavigationLink(
        destination = {
            StreamDeckLayoutSettingsDetailView(model = model, layout = layout)
        },
    ) {
        Text(text = layout.name, style = formBodyStyle)
    }
}

@Composable
fun StreamDeckLayoutSettingsDetailView(
    model: Model = LocalModel.current,
    layout: SettingsStreamDeckLayout,
) {
    val deckModel by layout.model.collectAsState()
    val keys by layout.keys.collectAsState()
    var selectedIndex by remember { mutableStateOf(0) }

    Form(title = "Stream deck") {
        Section {
            NameEditView(
                name = layout.name,
                onNameChange = { layout.name = it },
            )
        }
        Section {
            Picker(
                title = "Model",
                selection = deckModel,
                options = SettingsStreamDeckModel.entries,
                text = { it.toString() },
                onChange = {
                    layout.model.setFlowValue(it)
                    selectedIndex = 0
                },
            )
        }
        Section {
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
        Section {
            keys.forEach { deckKey ->
                if (keys.indexOfFirst { it === deckKey } == selectedIndex) {
                    key(deckKey.id) {
                        StreamDeckSettingsKeyView(model = model, key = deckKey)
                    }
                }
            }
        }
    }
}

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
    val selectedLayout = layouts.firstOrNull { it.id == selectedId }

    Form(title = "Stream deck") {
        Section {
            HCenter {
                IntegrationImageView(imageName = "StreamDeck")
            }
        }
        if (!isDeviceDriverInstalled) {
            Section {
                Text(
                    text = "⚠️ Download and install Stream Deck Connect from the App Store, " +
                        "and then enable the Stream Deck Device Driver in its settings.",
                    style = formBodyStyle,
                )
            }
        }
        Section {
            Picker(
                title = "Current",
                selection = selectedLayout,
                options = listOf<SettingsStreamDeckLayout?>(null) + layouts,
                text = { it?.name ?: "-- None --" },
                onChange = {
                    streamDecks.selectedId.setFlowValue(it?.id)
                    model.setSelectedStreamDeck()
                },
            )
        }
        Section(
            header = "Layouts",
            footerContent = {
                SwipeLeftToDeleteHelpView(kind = "a layout")
            },
        ) {
            layouts.forEachIndexed { index, streamDeckLayout ->
                key(streamDeckLayout.id) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StreamDeckLayoutSettingsView(
                                model = model,
                                layout = streamDeckLayout,
                                onNavigate = onNavigate,
                            )
                        }
                        SystemImage(
                            name = "arrow.up",
                            fontSize = 17.sp,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = index > 0,
                                ) {
                                    if (index > 0) {
                                        val reordered = layouts.toMutableList()
                                        val moved = reordered.removeAt(index)
                                        reordered.add(index - 1, moved)
                                        streamDecks.layouts.setFlowValue(reordered)
                                    }
                                },
                        )
                        SystemImage(
                            name = "arrow.down",
                            fontSize = 17.sp,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = index < layouts.size - 1,
                                ) {
                                    if (index < layouts.size - 1) {
                                        val reordered = layouts.toMutableList()
                                        val moved = reordered.removeAt(index)
                                        reordered.add(index + 1, moved)
                                        streamDecks.layouts.setFlowValue(reordered)
                                    }
                                },
                        )
                        SystemImage(
                            name = "trash",
                            fontSize = 17.sp,
                            modifier = Modifier
                                .padding(start = 10.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    streamDecks.layouts.setFlowValue(
                                        layouts.filterIndexed { i, _ -> i != index },
                                    )
                                    model.setSelectedStreamDeck()
                                },
                        )
                    }
                }
            }
            CreateButtonView {
                val streamDeckLayout = SettingsStreamDeckLayout()
                streamDeckLayout.name =
                    makeUniqueName(
                        name = SettingsStreamDeckLayout.baseName,
                        existingNames = layouts,
                    )
                streamDecks.layouts.setFlowValue(layouts + streamDeckLayout)
            }
        }
    }
}
