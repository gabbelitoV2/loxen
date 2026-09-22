package com.moblin.android.view.settings.scenes.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.WidgetWizardSettingsView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.view.utils.SwipeLeftToDeleteButtonView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun WidgetsSettingsItemView(
    model: Model = LocalModel.current,
    database: Database,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val deleteWidget: () -> Unit = {
        database.widgets.removeAll { it === widget }
        Unit
        Unit
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
    LaunchedEffect(widget.enabled) {
        Unit
    }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onNavigate("WidgetSettingsView") },
                        onLongClick = { TODO("context menus have no Material 3 counterpart") },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DraggableItemPrefixView()
                IconAndTextView(
                    image = widget.image(),
                    text = widget.name,
                    longDivider = true,
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = widget.enabled,
                    onCheckedChange = { widget.enabled = it },
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingCreateWizard by remember { mutableStateOf(false) }
    val onMove: (Int, Int) -> Unit = { from, to ->
        if (from != to) {
            val item = database.widgets.removeAt(from)
            database.widgets.add(if (from < to) to - 1 else to, item)
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Widgets",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        database.widgets.forEach { widget ->
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(text = "A widget can be used in zero or more scenes.")
            Text(text = "")
            SwipeLeftToDeleteHelpView(kind = localized("a widget"))
        }
    }
    if (presentingCreateWizard) {
        ModalBottomSheet(
            onDismissRequest = { presentingCreateWizard = false },
        ) {
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
