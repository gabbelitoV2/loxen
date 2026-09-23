package com.moblin.android.view.settings.scenes.widgets.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateWidgetWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.appendWidgetToScene
import com.moblin.android.various.model.getSelectedScene
import com.moblin.android.various.model.resetSelectedScene
import com.moblin.android.various.model.textWidgetTextChanged
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.settings.SettingsWidgetType
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardBingoCardSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardBrowserSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardImageSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardPngTuberSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardScoreboardSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardSlideshowSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardTextSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardVTuberSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardVideoSourceSettingsView
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WidgetWizardWheelOfLuckSettingsView
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.TextButtonView
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow

fun basicWidgetSettingsTitle(createWidgetWizard: CreateWidgetWizard): String {
    return localized("Basic ${createWidgetWizard.type.value.toString()} widget settings")
}

@Composable
private fun AddWidgetToSceneView(scene: SceneToAddWidgetTo) {
    val enabled by scene.enabled.collectAsState()
    val palette = formPalette()
    FormRow(onClick = { scene.toggleEnabled() }) {
        Text(scene.scene.name, color = palette.label)
        Spacer(Modifier.weight(1f))
        if (enabled) {
            SystemImage("checkmark", fontSize = 22.sp, tint = palette.accent)
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
            makeUniqueName(SettingsWidget.baseName, database.widgets)
        } else {
            createWidgetWizard.name.value
        }
        val widget = createWidgetWizard.widget
        widget.type = createWidgetWizard.type.value
        widget.name = name
        database.widgets += widget
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

    Form(
        title = localized("Scenes to add the widget to"),
        toolbar = {
            CloseToolbar(
                presenting = presentingCreateWizard,
                onPresentingChange = onPresentingCreateWizardChange,
            )
        },
    ) {
        Section {
            scenesToAddWidgetTo.forEach { scene ->
                key(scene.id) {
                    AddWidgetToSceneView(scene = scene)
                }
            }
        }
        Section {
            TextButtonView(localized("Create")) { create() }
        }
    }

    LaunchedEffect(Unit) {
        scenesToAddWidgetTo = database.scenes.map {
            SceneToAddWidgetTo(
                scene = it,
                enabled = it === model.getSelectedScene(),
            )
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
    Section {
        NavigationLink(
            destination = {
                SelectScenesView(
                    model = model,
                    database = database,
                    createWidgetWizard = createWidgetWizard,
                    presentingCreateWizard = presentingCreateWizard,
                    onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                )
            },
        ) {
            WizardNextButtonView()
        }
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
    val palette = formPalette()

    fun isValidName(): String? {
        if (database.widgets.any { it.name == name }) {
            return localized("The name '$name' is already in use.")
        }
        return null
    }

    fun canGoNext(): Boolean {
        return isValidName() == null
    }

    Form(
        title = localized("Create widget wizard"),
        toolbar = {
            CloseToolbar(
                presenting = presentingCreateWizard,
                onPresentingChange = onPresentingCreateWizardChange,
            )
        },
    ) {
        Section(footer = type.toString()) {
            Picker(
                title = localized("Type"),
                selection = type,
                options = SettingsWidgetType.entries,
            ) { widgetType ->
                createWidgetWizard.type.value = widgetType
            }
        }
        Section(
            header = localized("Name"),
            footerContent = {
                isValidName()?.let { message ->
                    Text(
                        text = message,
                        color = palette.red,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
        ) {
            FormRow {
                Box {
                    if (name.isEmpty()) {
                        Text(
                            text = localized("My widget"),
                            color = palette.secondaryLabel,
                        )
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = { createWidgetWizard.name.value = it },
                        singleLine = true,
                        textStyle = formBodyStyle.copy(color = palette.label),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Section {
            NavigationLink(
                enabled = canGoNext(),
                destination = {
                    val widgetType by createWidgetWizard.type.collectAsState()
                    when (widgetType) {
                        SettingsWidgetType.text -> WidgetWizardTextSettingsView(
                            model = model,
                            database = database,
                            createWidgetWizard = createWidgetWizard,
                            text = createWidgetWizard.widget.text,
                            presentingCreateWizard = presentingCreateWizard,
                            onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.browser -> WidgetWizardBrowserSettingsView(
                            model = model,
                            database = database,
                            createWidgetWizard = createWidgetWizard,
                            browser = createWidgetWizard.widget.browser,
                            presentingCreateWizard = presentingCreateWizard,
                            onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.videoSource -> WidgetWizardVideoSourceSettingsView(
                            model = model,
                            database = database,
                            createWidgetWizard = createWidgetWizard,
                            videoSource = createWidgetWizard.widget.videoSource,
                            presentingCreateWizard = presentingCreateWizard,
                            onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.image -> WidgetWizardImageSettingsView(
                            model = model,
                            database = database,
                            widget = createWidgetWizard.widget,
                            createWidgetWizard = createWidgetWizard,
                            presentingCreateWizard = presentingCreateWizard,
                            onChangePresentingCreateWizard = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.slideshow -> WidgetWizardSlideshowSettingsView(
                            model = model,
                            database = database,
                            createWidgetWizard = createWidgetWizard,
                            slideshow = createWidgetWizard.widget.slideshow,
                            presentingCreateWizard = presentingCreateWizard,
                            onChangePresentingCreateWizard = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.vTuber -> WidgetWizardVTuberSettingsView(
                            model = model,
                            database = database,
                            vTuber = createWidgetWizard.widget.vTuber,
                            createWidgetWizard = createWidgetWizard,
                            presentingCreateWizard = presentingCreateWizard,
                            onChangePresentingCreateWizard = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.pngTuber -> WidgetWizardPngTuberSettingsView(
                            model = model,
                            database = database,
                            pngTuber = createWidgetWizard.widget.pngTuber,
                            createWidgetWizard = createWidgetWizard,
                            presentingCreateWizard = presentingCreateWizard,
                            onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.wheelOfLuck -> WidgetWizardWheelOfLuckSettingsView(
                            model = model,
                            database = database,
                            wheelOfLuck = createWidgetWizard.widget.wheelOfLuck,
                            createWidgetWizard = createWidgetWizard,
                            presentingCreateWizard = presentingCreateWizard,
                            onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.bingoCard -> WidgetWizardBingoCardSettingsView(
                            model = model,
                            database = database,
                            bingoCard = createWidgetWizard.widget.bingoCard,
                            createWidgetWizard = createWidgetWizard,
                            presentingCreateWizard = presentingCreateWizard,
                            onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                        )
                        SettingsWidgetType.scoreboard -> WidgetWizardScoreboardSettingsView(
                            model = model,
                            database = database,
                            scoreboard = createWidgetWizard.widget.scoreboard,
                            createWidgetWizard = createWidgetWizard,
                            presentingCreateWizard = presentingCreateWizard,
                            onChangePresentingCreateWizard = onPresentingCreateWizardChange,
                            onChangeSport = { createWidgetWizard.widget.scoreboard.sport = it },
                        )
                        else -> SelectScenesView(
                            model = model,
                            database = database,
                            createWidgetWizard = createWidgetWizard,
                            presentingCreateWizard = presentingCreateWizard,
                            onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                        )
                    }
                },
            ) {
                WizardNextButtonView()
            }
        }
    }
}
