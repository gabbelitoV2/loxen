package com.moblin.android.view.settings.keyboard

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsKeyboard
import com.moblin.android.various.settings.SettingsKeyboardKey
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardSettingsView(
    model: Model = LocalModel.current,
    keyboard: SettingsKeyboard,
) {
    val keys by keyboard.keys.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Keyboard") })
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Text("Use a keyboard to zoom, set scene, and more.")
            }
            items(items = keys, key = { it.id }) { key ->
                var menuExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(key.id) {
                            detectTapGestures(onLongPress = { menuExpanded = true })
                        },
                ) {
                    KeyboardKeySettingsView(model = model, key = key)
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(localized("Delete")) },
                            onClick = {
                                menuExpanded = false
                                keyboard.keys.value =
                                    keys.filterNot { it.id == key.id }.toMutableList()
                            },
                        )
                    }
                }
            }
            item {
                CreateButtonView {
                    keyboard.keys.value = (keys + SettingsKeyboardKey()).toMutableList()
                }
            }
            item {
                SwipeLeftToDeleteHelpView(kind = localized("a key"))
            }
        }
    }
}
