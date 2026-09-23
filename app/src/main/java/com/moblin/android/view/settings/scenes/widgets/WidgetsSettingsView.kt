package com.moblin.android.view.settings.scenes.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.HorizontalEdge
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.NavigationStack
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.SwipeActions
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.move
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSettingsView
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.view.utils.SwipeLeftToDeleteButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.various.model.isCaptureDeviceWidget
import com.moblin.android.various.model.removeDeadWidgetsFromScenes
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.sceneUpdated

@Composable
private fun WidgetsSettingsItemView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val enabled = binding(
        get = { widget.enabled },
        set = { value ->
            widget.enabled = value
            model.sceneUpdated(attachCamera = model.isCaptureDeviceWidget(widget = widget))
        },
    )
    ContextMenuDeleteButton(
        action = {
            database.widgets.removeAll { it === widget }
            model.removeDeadWidgetsFromScenes()
            model.resetSelectedScene()
        },
    ) {
        SwipeActions(
            edge = HorizontalEdge.trailing,
            allowsFullSwipe = false,
            actions = {
                SwipeLeftToDeleteButtonView {
                    database.widgets.removeAll { it === widget }
                    model.removeDeadWidgetsFromScenes()
                    model.resetSelectedScene()
                }
            },
        ) {
            NavigationLink(
                destination = {
                    WidgetSettingsView(model = model, database = database, widget = widget)
                },
            ) {
                Toggle(isOn = enabled.value, onChange = { enabled.value = it }) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
        }
    }
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
        ForEach(
            database.widgets,
            id = { it.id },
            onMove = { froms, to ->
                database.widgets.move(fromOffsets = froms, toOffset = to)
            },
        ) { widget ->
            WidgetsSettingsItemView(
                model = model,
                database = database,
                widget = widget,
                onNavigate = onNavigate,
            )
        }
        CreateButtonView {
            presentingCreateWizard = true
            model.createWidgetWizard.reset()
        }
    }
    Sheet(isPresented = presentingCreateWizard, onDismissRequest = { presentingCreateWizard = false }) {
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
