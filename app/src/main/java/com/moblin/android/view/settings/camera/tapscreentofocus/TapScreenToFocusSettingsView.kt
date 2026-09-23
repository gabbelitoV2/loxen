package com.moblin.android.view.settings.camera.tapscreentofocus

import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.model.setAutoFocus

@Composable
fun TapScreenToFocusSettingsView(model: Model = LocalModel.current, database: Database) {
    Toggle(
        title = localized("Tap screen to focus"),
        isOn = database.tapToFocus,
        onChange = { value ->
            database.tapToFocus = value
            if (!value) {
                model.setAutoFocus()
            }
        },
    )
}
