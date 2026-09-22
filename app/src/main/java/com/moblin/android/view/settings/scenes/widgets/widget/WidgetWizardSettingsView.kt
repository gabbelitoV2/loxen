package com.moblin.android.view.settings.scenes.widgets.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.TextButtonView
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

fun basicWidgetSettingsTitle(createWidgetWizard: CreateWidgetWizard): String {
    return localized("Basic ${createWidgetWizard.type.value.toString()} widget settings")
}

@Composable
private fun AddWidgetToSceneView(scene: SceneToAddWidgetTo) {
    val enabled by scene.enabled.collectAsState()
    Button(
        onClick = { scene.toggleEnabled() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(scene.scene.name)
            Spacer(Modifier.weight(1f))
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Blue,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

class SceneToAddWidgetTo(val scene: SettingsScene, enabled: Boolean) {
    val id: UUID = UUID.randomUUID()
    val enabled = MutableStateFlow(enabled)

    fun toggleEnabled() {
        enabled.value = !enabled.value
    }
}

@Composable
private fun SelectScenesView(
    model: Model = LocalModel.current,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
) {
    var scenesToAddWidgetTo by remember { mutableStateOf(emptyList<SceneToAddWidgetTo>()) }

    fun create() {
        onPresentingCreateWizardChange(false)
        val name: String = if (createWidgetWizard.name.value.isEmpty()) {
            makeUniqueName(SettingsWidget.baseName, database.widgets.value)
        } else {
            createWidgetWizard.name.value
        }
        val widget = createWidgetWizard.widget
        widget.type = createWidgetWizard.type.value
        widget.name = name
        database.widgets.value = database.widgets.value + widget
        model.fixAlertMedias()
        model.resetSelectedScene(changeScene = false, attachCamera = false)
        for (sceneToAddWidgetTo in scenesToAddWidgetTo) {
            if (sceneToAddWidgetTo.enabled.value) {
                model.appendWidgetToScene(sceneToAddWidgetTo.scene, widget)
            }
        }
        if (widget.type == SettingsWidgetType.text) {
            model.textWidgetTextChanged(widget)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CloseToolbar(
            presenting = presentingCreateWizard,
            onPresentingChange = onPresentingCreateWizardChange,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(scenesToAddWidgetTo, key = { it.id }) { scene ->
                AddWidgetToSceneView(scene = scene)
            }
            item {
                TextButtonView("Create") { create() }
            }
        }
    }

    LaunchedEffect(Unit) {
        scenesToAddWidgetTo = database.scenes.value.map {
            SceneToAddWidgetTo(scene = it, enabled = it === model.getSelectedScene())
        }
    }
}

@Composable
fun WidgetWizardSelectScenesNavigationView(
    model: Model = LocalModel.current,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Column {
        WizardNextButtonView(onClick = { onNavigate("SelectScenesView") })
    }
}

@Composable
fun WidgetWizardSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    createWidgetWizard: CreateWidgetWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val type by createWidgetWizard.type.collectAsState()
    val name by createWidgetWizard.name.collectAsState()

    fun isValidName(): String? {
        if (database.widgets.value.any { it.name == name }) {
            return localized("The name '$name' is already in use.")
        }
        return null
    }

    fun canGoNext(): Boolean {
        return isValidName() == null
    }

    Column(modifier = Modifier.fillMaxSize()) {
        CloseToolbar(
            presenting = presentingCreateWizard,
            onPresentingChange = onPresentingCreateWizardChange,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                var expanded by remember { mutableStateOf(false) }
                Column {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                    ) {
                        OutlinedTextField(
                            value = type.toString(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Type") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            SettingsWidgetType.entries.forEach { widgetType ->
                                DropdownMenuItem(
                                    text = { Text(widgetType.toString()) },
                                    onClick = {
                                        createWidgetWizard.type.value = widgetType
                                        expanded = false
                                    },
                                )
                            }
                        }
                    }
                    Text(
                        text = type.description(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Text("Name", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { createWidgetWizard.name.value = it },
                    label = { Text("My widget") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                isValidName()?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            item {
                WizardNextButtonView(enabled = canGoNext()) {
                    when (type) {
                        SettingsWidgetType.text -> onNavigate("WidgetWizardTextSettingsView")
                        SettingsWidgetType.browser -> onNavigate("WidgetWizardBrowserSettingsView")
                        SettingsWidgetType.videoSource -> onNavigate("WidgetWizardVideoSourceSettingsView")
                        SettingsWidgetType.image -> onNavigate("WidgetWizardImageSettingsView")
                        SettingsWidgetType.slideshow -> onNavigate("WidgetWizardSlideshowSettingsView")
                        SettingsWidgetType.vTuber -> onNavigate("WidgetWizardVTuberSettingsView")
                        SettingsWidgetType.pngTuber -> onNavigate("WidgetWizardPngTuberSettingsView")
                        SettingsWidgetType.wheelOfLuck -> onNavigate("WidgetWizardWheelOfLuckSettingsView")
                        SettingsWidgetType.bingoCard -> onNavigate("WidgetWizardBingoCardSettingsView")
                        SettingsWidgetType.scoreboard -> onNavigate("WidgetWizardScoreboardSettingsView")
                        else -> onNavigate("SelectScenesView")
                    }
                }
            }
        }
    }
}
