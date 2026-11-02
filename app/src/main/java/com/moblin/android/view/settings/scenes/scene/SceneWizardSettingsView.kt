package com.moblin.android.view.settings.scenes.scene

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.CreateSceneWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.VideoSources
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsScene
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.settings.scenes.widgets.widget.wizard.WizardVideoSourcePickerView
import com.moblin.android.view.settings.streams.stream.WizardNextButtonView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.IconAndTextView
import com.moblin.android.view.utils.InlinePickerItem
import com.moblin.android.view.utils.TextButtonView
import kotlinx.coroutines.flow.combine
import com.moblin.android.various.model.createScene
import com.moblin.android.various.model.getCameraId
import com.moblin.android.various.model.cameraIdToSettingsCameraId

@Composable
private fun SelectWidgetView(
    createSceneWizard: CreateSceneWizard,
    widget: SettingsWidget,
) {
    val widgetIds by createSceneWizard.widgetIds.collectAsState()
    val palette = formPalette()
    CompositionLocalProvider(LocalTint provides palette.label) {
        Button(action = {
            createSceneWizard.widgetIds.value = if (widgetIds.contains(widget.id)) {
                widgetIds - widget.id
            } else {
                widgetIds + widget.id
            }
        }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconAndTextView(image = widget.image(), text = widget.name)
                Spacer(Modifier.weight(1f))
                if (widgetIds.contains(widget.id)) {
                    SystemImage(name = "checkmark", fontSize = 22.sp, tint = palette.accent)
                }
            }
        }
    }
}

@Composable
private fun SelectWidgetsView(
    database: Database,
    createSceneWizard: CreateSceneWizard,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
    onCreated: (SettingsScene) -> Unit,
    model: Model = LocalModel.current,
) {
    Form(
        title = "Widgets to add to the scene",
        toolbar = {
            CloseToolbar(
                presenting = presentingCreateWizard,
                onPresentingChange = onPresentingCreateWizardChange,
            )
        },
    ) {
        if (database.widgets.isNotEmpty()) {
            Section {
                ForEach(database.widgets, id = { it.id }) { widget ->
                    SelectWidgetView(createSceneWizard = createSceneWizard, widget = widget)
                }
            }
        }
        Section {
            TextButtonView("Create") {
                onPresentingCreateWizardChange(false)
                onCreated(model.createScene())
            }
        }
    }
}

@Composable
private fun SceneWizardSettingsView(
    database: Database,
    createSceneWizard: CreateSceneWizard,
    videoSources: VideoSources,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
    onCreated: (SettingsScene) -> Unit,
    model: Model = LocalModel.current,
) {
    val name by createSceneWizard.name.collectAsState()
    val palette = formPalette()
    val cameras by combine(
        videoSources.back,
        videoSources.front,
        videoSources.external,
        videoSources.rtmp,
        videoSources.srtla,
        videoSources.srtClient,
        videoSources.rist,
        videoSources.rtsp,
        videoSources.whip,
        videoSources.whep,
        videoSources.mediaPlayer,
    ) { videoSources.all() }.collectAsState(initial = videoSources.all())

    fun isValidName(): String? {
        if (database.scenes.any { it.name == name }) {
            return localized("The name '$name' is already in use.")
        }
        return null
    }

    fun onCameraChange(cameraId: String) {
        createSceneWizard.scene.updateCameraId(
            settingsCameraId = model.cameraIdToSettingsCameraId(cameraId = cameraId),
        )
    }

    Form(
        title = "Create scene wizard",
        toolbar = {
            CloseToolbar(
                presenting = presentingCreateWizard,
                onPresentingChange = onPresentingCreateWizardChange,
            )
        },
    ) {
        Section(
            header = "Name",
            footerContent = {
                isValidName()?.let { message ->
                    Text(
                        message,
                        style = formFootnoteStyle,
                        color = palette.red,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
        ) {
            FormRow {
                BasicTextField(
                    value = name,
                    onValueChange = { createSceneWizard.name.value = it },
                    modifier = Modifier.weight(1f),
                    textStyle = formBodyStyle.copy(color = palette.label),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        autoCorrectEnabled = false,
                    ),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (name.isEmpty()) {
                            Text(
                                SettingsScene.baseName,
                                style = formBodyStyle,
                                color = palette.tertiaryLabel,
                            )
                        }
                        innerTextField()
                    },
                )
            }
        }
        Section {
            WizardVideoSourcePickerView(
                onChange = { onCameraChange(it) },
                items = cameras.map { InlinePickerItem(id = it.id, text = it.name) },
                selectedId = model.getCameraId(scene = createSceneWizard.scene),
            )
        }
        Section {
            NavigationLink(
                destination = {
                    SelectWidgetsView(
                        database = database,
                        createSceneWizard = createSceneWizard,
                        presentingCreateWizard = presentingCreateWizard,
                        onPresentingCreateWizardChange = onPresentingCreateWizardChange,
                        onCreated = onCreated,
                        model = model,
                    )
                },
                enabled = isValidName() == null,
            ) {
                WizardNextButtonView()
            }
        }
    }
}

@Composable
fun SceneWizardSheetView(
    database: Database,
    presentingCreateWizard: Boolean,
    onPresentingCreateWizardChange: (Boolean) -> Unit,
    onCreated: (SettingsScene) -> Unit = {},
    model: Model = LocalModel.current,
) {
    SceneWizardSettingsView(
        database = database,
        createSceneWizard = model.createSceneWizard,
        videoSources = model.videoSources,
        presentingCreateWizard = presentingCreateWizard,
        onPresentingCreateWizardChange = onPresentingCreateWizardChange,
        onCreated = onCreated,
        model = model,
    )
}
