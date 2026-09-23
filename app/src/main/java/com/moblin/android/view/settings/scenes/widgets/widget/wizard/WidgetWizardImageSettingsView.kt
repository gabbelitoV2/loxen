package com.moblin.android.view.settings.scenes.widgets.widget.wizard

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSelectScenesNavigationView
import com.moblin.android.view.settings.scenes.widgets.widget.basicWidgetSettingsTitle
import com.moblin.android.view.settings.scenes.widgets.widget.image.WidgetImagePickerView
import com.moblin.android.view.utils.CloseToolbar

@Composable
fun WidgetWizardImageSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onChangePresentingCreateWizard: (Boolean) -> Unit,
) {
    var image by remember { mutableStateOf<Bitmap?>(null) }

    Form(
        title = basicWidgetSettingsTitle(createWidgetWizard),
        toolbar = {
            CloseToolbar(
                presenting = presentingCreateWizard,
                onPresentingChange = onChangePresentingCreateWizard,
            )
        },
    ) {
        WidgetImagePickerView(
            model = model,
            widget = widget,
            image = image,
            onImageChange = { image = it },
            sizeScale = 5.0,
        )
        Box(
            modifier = Modifier
                .alpha(if (image == null) 0.4f else 1f)
                .pointerInput(image == null) {
                    if (image == null) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                            }
                        }
                    }
                },
        ) {
            WidgetWizardSelectScenesNavigationView(
                model = model,
                database = database,
                createWidgetWizard = createWidgetWizard,
                presentingCreateWizard = presentingCreateWizard,
                onPresentingCreateWizardChange = onChangePresentingCreateWizard,
            )
        }
    }
}
