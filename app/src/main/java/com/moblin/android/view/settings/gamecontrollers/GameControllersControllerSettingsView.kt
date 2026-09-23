package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsGameController

@Composable
fun GameControllersControllerSettingsView(model: Model = LocalModel.current, gameController: SettingsGameController) {
    val buttons by gameController.buttons.collectAsState()
    val leftThumbStickFunction by gameController.leftThumbStickFunction.collectAsState()
    val rightThumbStickFunction by gameController.rightThumbStickFunction.collectAsState()
    Form(title = "Controller") {
        Section(header = "Buttons") {
            for (button in buttons) {
                key(button.id) {
                    GameControllersControllerButtonSettingsView(model = model, button = button)
                }
            }
        }
        Section(header = "Thumb sticks") {
            GameControllersControllerThumbStickSettingsView(
                image = "l.joystick",
                name = "Left",
                function = leftThumbStickFunction,
                onFunctionChange = { gameController.leftThumbStickFunction.value = it },
            )
            GameControllersControllerThumbStickSettingsView(
                image = "r.joystick",
                name = "Right",
                function = rightThumbStickFunction,
                onFunctionChange = { gameController.rightThumbStickFunction.value = it },
            )
        }
    }
}
