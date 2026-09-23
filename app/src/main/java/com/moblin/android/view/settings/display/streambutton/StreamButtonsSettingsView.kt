package com.moblin.android.view.settings.display.streambutton

import androidx.compose.runtime.Composable
import com.moblin.android.common.various.color
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.defaultStreamButtonColor
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextButtonView

@Composable
fun StreamButtonsSettingsView(database: Database) {
    Form(title = localized("Stream button")) {
        Section(header = localized("Color")) {
            RgbColorPickerView(
                title = localized("Background"),
                color = database.streamButtonColorColor,
                onColorChanged = { color ->
                    database.streamButtonColorColor = color
                },
                onChange = { color ->
                    database.streamButtonColor = color
                },
            )
            TextButtonView(
                title = localized("Reset"),
                action = {
                    database.streamButtonColor = defaultStreamButtonColor
                    database.streamButtonColorColor = defaultStreamButtonColor.color()
                },
            )
        }
    }
}
