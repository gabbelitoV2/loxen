package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsGameController
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameControllersControllerSettingsView(model: Model = LocalModel.current, gameController: SettingsGameController) {
    val buttons by gameController.buttons.collectAsState()
    val leftThumbStickFunction by gameController.leftThumbStickFunction.collectAsState()
    val rightThumbStickFunction by gameController.rightThumbStickFunction.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Controller") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text("Buttons", style = MaterialTheme.typography.titleMedium)
            }
            items(buttons) { button ->
                GameControllersControllerButtonSettingsView(model = model, button = button)
            }
            item {
                Text("Thumb sticks", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Column {
                    GameControllersControllerThumbStickSettingsView(
                        image = "l.joystick",
                        name = "Left",
                        function = leftThumbStickFunction,
                        onFunctionChange = {
                            TODO("update left thumb stick function")
                        }
                    )
                    GameControllersControllerThumbStickSettingsView(
                        image = "r.joystick",
                        name = "Right",
                        function = rightThumbStickFunction,
                        onFunctionChange = {
                            TODO("update right thumb stick function")
                        }
                    )
                }
            }
        }
    }
}
