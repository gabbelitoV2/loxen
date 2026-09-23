package com.moblin.android.view.settings.gamecontrollers

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.settings.SettingsControllerThumbStickFunction

@Composable
fun ControllerThumbStickView(
    function: SettingsControllerThumbStickFunction,
    onFunctionChange: (SettingsControllerThumbStickFunction) -> Unit,
) {
    Picker(
        title = "Function",
        selection = function,
        options = SettingsControllerThumbStickFunction.entries,
        text = { it.toString() },
        onChange = onFunctionChange,
    )
}

@Composable
fun GameControllersControllerThumbStickSettingsView(
    image: String,
    name: String,
    function: SettingsControllerThumbStickFunction,
    onFunctionChange: (SettingsControllerThumbStickFunction) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            Form(title = "Thumb stick") {
                Section {
                    ControllerThumbStickView(
                        function = function,
                        onFunctionChange = onFunctionChange,
                    )
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SystemImage(image, fontSize = 17.sp)
            Text(localized(name))
            Spacer(Modifier.weight(1f))
            Text(
                text = function.toString(),
                color = function.color(),
            )
        }
    }
}
