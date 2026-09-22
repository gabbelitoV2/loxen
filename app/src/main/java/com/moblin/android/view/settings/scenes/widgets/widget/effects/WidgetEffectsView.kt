package com.moblin.android.view.settings.scenes.widgets.widget.effects

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsVideoEffect
import com.moblin.android.various.settings.SettingsVideoEffectType
import com.moblin.android.various.settings.SettingsWidget
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun EffectLabelView(model: Model = LocalModel.current, effect: SettingsVideoEffect) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DraggableItemPrefixView()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = effect.type.toString(),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = effect.enabled,
                onCheckedChange = { enabled ->
                    effect.enabled = enabled
                    Unit
                },
            )
        }
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
            Box(
                modifier = Modifier.clickable {
                    onNavigate(effect.type.toString())
                },
            ) {
                EffectLabelView(model = model, effect = effect)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
        Unit
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
        Unit
    }

    Text(
        text = "Effects",
        style = MaterialTheme.typography.titleMedium,
    )
    widget.effects.forEach { effect ->
        Box(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = {
                    Unit
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
    CreateButtonView {
        newEffect = SettingsVideoEffect()
        presentingCreateWizard = true
    }
    SwipeLeftToDeleteHelpView(kind = localized("an effect"))
    if (presentingCreateWizard) {
        ModalBottomSheet(onDismissRequest = { presentingCreateWizard = false }) {
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
