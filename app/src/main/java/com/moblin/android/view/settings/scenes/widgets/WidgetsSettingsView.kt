package com.moblin.android.view.settings.scenes.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.NavigationStack
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSettingsView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.view.utils.SwipeLeftToDeleteButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.various.model.isCaptureDeviceWidget
import com.moblin.android.various.model.removeDeadWidgetsFromScenes
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.sceneUpdated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetsSettingsItemView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val deleteWidget: () -> Unit = {
        database.widgets.removeAll { it === widget }
        model.removeDeadWidgetsFromScenes()
        model.resetSelectedScene()
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                deleteWidget()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                SwipeLeftToDeleteButtonView(action = {
                    deleteWidget()
                })
            }
        },
        content = {
            NavigationLink(
                destination = {
                    WidgetSettingsView(model = model, database = database, widget = widget)
                },
            ) {
                Toggle(
                    isOn = widget.enabled,
                    onChange = { enabled ->
                        widget.enabled = enabled
                        model.sceneUpdated(
                            attachCamera = model.isCaptureDeviceWidget(widget = widget),
                        )
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DraggableItemPrefixView()
                        IconAndTextView(
                            image = widget.image(),
                            text = widget.name,
                            longDivider = true,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        },
    )
}

@Composable
fun WidgetsSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingCreateWizard by remember { mutableStateOf(false) }
    Section(
        header = localized("Widgets"),
        footerContent = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(text = localized("A widget can be used in zero or more scenes."))
                Text(text = "")
                SwipeLeftToDeleteHelpView(kind = localized("a widget"))
            }
        },
    ) {
        database.widgets.forEach { widget ->
            key(widget.id) {
                WidgetsSettingsItemView(
                    model = model,
                    database = database,
                    widget = widget,
                    onNavigate = onNavigate,
                )
            }
        }
        CreateButtonView {
            presentingCreateWizard = true
            model.createWidgetWizard.reset()
        }
    }
    if (presentingCreateWizard) {
        Sheet(onDismissRequest = { presentingCreateWizard = false }) {
            NavigationStack {
                WidgetWizardSettingsView(
                    model = model,
                    database = database,
                    createWidgetWizard = model.createWidgetWizard,
                    presentingCreateWizard = presentingCreateWizard,
                    onPresentingCreateWizardChange = { presentingCreateWizard = it },
                )
            }
        }
    }
}
