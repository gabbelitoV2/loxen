package com.moblin.android.view.settings.display.quickbuttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsQuickButton
import com.moblin.android.various.settings.SettingsQuickButtons
import com.moblin.android.view.controlbar.controlBarPages
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun AppearanceSettingsView(database: Database, quickButtons: SettingsQuickButtons) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = localized("Appearance"),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
        )
        if (database.showAllSettings.collectAsState().value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = localized("Scroll"), modifier = Modifier.weight(1f))
                Switch(
                    checked = quickButtons.enableScroll.collectAsState().value,
                    onCheckedChange = { quickButtons.enableScroll.value = it },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = localized("Two columns"), modifier = Modifier.weight(1f))
                Switch(
                    checked = quickButtons.twoColumns.collectAsState().value,
                    onCheckedChange = { quickButtons.twoColumns.value = it },
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = localized("Big buttons"), modifier = Modifier.weight(1f))
            Switch(
                checked = quickButtons.bigButtons.collectAsState().value,
                onCheckedChange = { quickButtons.bigButtons.value = it },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = localized("Show name"), modifier = Modifier.weight(1f))
            Switch(
                checked = quickButtons.showName.collectAsState().value,
                onCheckedChange = { quickButtons.showName.value = it },
            )
        }
        Text(
            text = localized("Names are not shown in portrait mode."),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun ButtonSettingsView(
    model: Model = LocalModel.current,
    button: SettingsQuickButton,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val enabled by button.enabled.collectAsState()
    LaunchedEffect(enabled) {
        model.updateQuickButtonPairs()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                model.quickButtons.selectedButtonType.value = button.type
                model.quickButtons.page.value = button.page
                model.quickButtons.activePage.value = button.page
                onNavigate("QuickButtonsButtonSettingsView")
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconAndTextView(
            image = button.imageOff,
            text = button.name,
            longDivider = true,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enabled,
            onCheckedChange = { button.enabled.value = it },
            enabled = !(button.isOn && enabled),
        )
    }
}

@Composable
private fun ButtonsSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val quickButtons by database.quickButtons.collectAsState()
    for (page in 1..controlBarPages) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Page $page",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            quickButtons.reversed().filter { it.page == page }.forEach { button ->
                ButtonSettingsView(
                    model = model,
                    button = button,
                    onNavigate = onNavigate,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonsSettingsView(
    model: Model = LocalModel.current,
    showAll: Boolean,
    onNavigate: (String) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = localized("Quick buttons")) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                AppearanceSettingsView(
                    database = model.database,
                    quickButtons = model.database.quickButtonsGeneral,
                )
            }
            if (showAll) {
                item {
                    ButtonsSettingsView(
                        model = model,
                        database = model.database,
                        onNavigate = onNavigate,
                    )
                }
            }
        }
    }
}
