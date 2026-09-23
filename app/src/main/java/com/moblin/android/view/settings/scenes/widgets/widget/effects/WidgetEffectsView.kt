package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectType
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemTextView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.various.model.resetSelectedScene

@Composable
private fun EffectLabelView(model: Model = LocalModel.current, effect: SettingsVideoEffect) {
    val enabled = binding(
        get = { effect.enabled },
        set = { value ->
            effect.enabled = value
            model.resetSelectedScene(changeScene = false)
        },
    )
    Toggle(isOn = enabled.value, onChange = { enabled.value = it }) {
        DraggableItemTextView(name = effect.type.toString())
    }
}

@Composable
private fun EffectView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    effect: SettingsVideoEffect,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    when (effect.type) {
        SettingsVideoEffectType.grayScale,
        SettingsVideoEffectType.sepia,
        SettingsVideoEffectType.whirlpool,
        SettingsVideoEffectType.pinch,
        -> {
            EffectLabelView(model = model, effect = effect)
        }
        else -> {
            NavigationLink(
                destination = {
                    Form(title = effect.type.toString()) {
                        when (effect.type) {
                            SettingsVideoEffectType.shape -> ShapeEffectView(
                                model = model,
                                widget = widget,
                                effect = effect,
                                shape = effect.shape,
                            )
                            SettingsVideoEffectType.removeBackground -> RemoveBackgroundEffectView(
                                model = model,
                                widget = widget,
                                effect = effect,
                                removeBackground = effect.removeBackground,
                            )
                            SettingsVideoEffectType.dewarp360 -> Dewarp360EffectView(
                                model = model,
                                widget = widget,
                                effect = effect,
                                dewarp360 = effect.dewarp360,
                            )
                            SettingsVideoEffectType.anamorphicLens -> AnamorphicLensEffectView(
                                model = model,
                                widget = widget,
                                effect = effect,
                                anamorphicLens = effect.anamorphicLens,
                            )
                            SettingsVideoEffectType.lut -> LutEffectView(
                                model = model,
                                color = model.database.color,
                                widget = widget,
                                effect = effect,
                                lut = effect.lut,
                            )
                            SettingsVideoEffectType.opacity -> OpacityEffectView(
                                model = model,
                                widget = widget,
                                effect = effect,
                                opacity = effect.opacity,
                            )
                            SettingsVideoEffectType.mask -> MaskEffectView(
                                model = model,
                                widget = widget,
                                effect = effect,
                                mask = effect.mask,
                            )
                            else -> {}
                        }
                    }
                },
            ) {
                EffectLabelView(model = model, effect = effect)
            }
        }
    }
}

@Composable
fun WidgetEffectsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingCreateWizard by remember { mutableStateOf(false) }
    var newEffect by remember { mutableStateOf(SettingsVideoEffect()) }

    fun deleteEffect(offsets: IndexSet) {
        widget.effects = widget.effects.removing(atOffsets = offsets)
        model.resetSelectedScene(changeScene = false)
    }

    Section(
        header = localized("Effects"),
        footerContent = {
            SwipeLeftToDeleteHelpView(kind = localized("an effect"))
        },
    ) {
        ForEach(
            widget.effects,
            id = { it.id },
            onDelete = { deleteEffect(it) },
            onMove = { froms, to ->
                widget.effects = widget.effects.moving(fromOffsets = froms, toOffset = to)
                model.resetSelectedScene(changeScene = false)
            },
        ) { effect ->
            ContextMenuDeleteButton(
                action = {
                    val offset = widget.effects.indexOfFirst { it.id == effect.id }
                    if (offset != -1) {
                        deleteEffect(setOf(offset))
                    }
                },
            ) {
                EffectView(
                    model = model,
                    widget = widget,
                    effect = effect,
                    onNavigate = onNavigate,
                )
            }
        }
        CreateButtonView {
            newEffect = SettingsVideoEffect()
            presentingCreateWizard = true
        }
    }
    Sheet(isPresented = presentingCreateWizard, onDismissRequest = { presentingCreateWizard = false }) {
        NavigationStack {
            WidgetEffectWizardSettingsView(
                model = model,
                widget = widget,
                effect = newEffect,
                presentingCreateWizard = presentingCreateWizard,
            ) {
                presentingCreateWizard = it
            }
        }
    }
}
