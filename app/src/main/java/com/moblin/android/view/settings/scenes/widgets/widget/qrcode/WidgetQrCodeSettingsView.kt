package com.moblin.android.view.settings.scenes.widgets.widget.qrcode

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.TextEditNavigationView

private fun submitMessage(model: Model, widget: SettingsWidget, value: String) {
    widget.qrCode.message = value
    Unit
}

@Composable
fun WidgetQrCodeSettingsView(model: Model = LocalModel.current, widget: SettingsWidget) {
    Column {
        Section {
            TextEditNavigationView(
                title = "Message",
                value = widget.qrCode.message,
                onSubmit = { value -> submitMessage(model, widget, value) }
            )
        }
        WidgetEffectsView(model = model, widget = widget)
    }
}
