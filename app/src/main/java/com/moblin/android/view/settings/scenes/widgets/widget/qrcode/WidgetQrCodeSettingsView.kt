package com.moblin.android.view.settings.scenes.widgets.widget.qrcode

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.effects.WidgetEffectsView
import com.moblin.android.view.utils.TextEditNavigationView

private fun submitMessage(model: Model, widget: SettingsWidget, value: String) {
    widget.qrCode.message = value
    model.resetSelectedScene(changeScene = false)
}

@Composable
fun WidgetQrCodeSettingsView(model: Model, widget: SettingsWidget) {
    Column {
        TextEditNavigationView(
            title = "Message",
            value = widget.qrCode.message,
            onSubmit = { value -> submitMessage(model, widget, value) }
        )
        WidgetEffectsView(model = model, widget = widget)
    }
}
