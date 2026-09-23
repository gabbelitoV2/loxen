package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectType
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.various.model.resetSelectedScene

@Composable
private fun EffectLabelView(model: Model = LocalModel.current, effect: SettingsVideoEffect) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DraggableItemPrefixView()
        Toggle(
            title = effect.type.toString(),
            isOn = effect.enabled,
            onChange = { enabled ->
                effect.enabled = enabled
                model.resetSelectedScene(changeScene = false)
            },
        )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetEffectsView(
    model: Model = LocalModel.current,
    widget: SettingsWidget,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingCreateWizard by remember { mutableStateOf(false) }
    var newEffect by remember { mutableStateOf(SettingsVideoEffect()) }

    fun deleteEffect(offsets: List<Int>) {
        val effects = widget.effects.toMutableList()
        for (offset in offsets.sortedDescending()) {
            if (offset in effects.indices) {
                effects.removeAt(offset)
            }
        }
        widget.effects = effects
        model.resetSelectedScene(changeScene = false)
    }

    fun moveEffects(fromOffsets: List<Int>, toOffset: Int) {
        val effects = widget.effects.toMutableList()
        val moved = fromOffsets.sorted().map { effects[it] }
        for (index in fromOffsets.sortedDescending()) {
            if (index in effects.indices) {
                effects.removeAt(index)
            }
        }
        var destination = toOffset
        for (index in fromOffsets) {
            if (index < toOffset) {
                destination -= 1
            }
        }
        effects.addAll(destination.coerceIn(0, effects.size), moved)
        widget.effects = effects
        model.resetSelectedScene(changeScene = false)
    }

    Section(
        header = localized("Effects"),
        footerContent = {
            SwipeLeftToDeleteHelpView(kind = localized("an effect"))
        },
    ) {
        widget.effects.forEach { effect ->
            key(effect.id) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                val offset = widget.effects.indexOfFirst { it.id == effect.id }
                                if (offset != -1) {
                                    deleteEffect(listOf(offset))
                                }
                            },
                        ),
                ) {
                    EffectView(
                        model = model,
                        widget = widget,
                        effect = effect,
                        onNavigate = onNavigate,
                    )
                }
            }
        }
        CreateButtonView {
            newEffect = SettingsVideoEffect()
            presentingCreateWizard = true
        }
    }
    if (presentingCreateWizard) {
        Sheet(onDismissRequest = { presentingCreateWizard = false }) {
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
}
