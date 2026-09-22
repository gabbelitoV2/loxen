package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsBeauty
import com.moblin.android.various.settings.SettingsBeautySettings

@Composable
private fun SmoothnessView(model: Model, beauty: SettingsBeauty) {
    val smoothnessRadius by beauty.smoothnessRadius.collectAsState()
    val smoothnessStrength by beauty.smoothnessStrength.collectAsState()

    fun setSettings() {
        model.beautyEffect.setSmoothnessSettings(
            radius = beauty.smoothnessRadius.value,
            strength = beauty.smoothnessStrength.value,
        )
    }

    Column {
        EffectSlider(
            title = "RADIUS",
            range = 5.0..20.0,
            value = smoothnessRadius,
            onValueChange = { beauty.smoothnessRadius.value = it },
        )
        EffectSlider(
            title = "STRENGTH",
            range = 0.0..1.0,
            value = smoothnessStrength,
            onValueChange = { beauty.smoothnessStrength.value = it },
        )
    }
    LaunchedEffect(smoothnessRadius) {
        setSettings()
    }
    LaunchedEffect(smoothnessStrength) {
        setSettings()
    }
}

@Composable
private fun ShapeView(model: Model, beauty: SettingsBeauty) {
    val shapePosition by beauty.shapePosition.collectAsState()
    val shapeRadius by beauty.shapeRadius.collectAsState()
    val shapeStrength by beauty.shapeStrength.collectAsState()

    fun setSettings() {
        model.beautyEffect.setShapeSettings(
            position = beauty.shapePosition.value,
            radius = beauty.shapeRadius.value,
            strength = beauty.shapeStrength.value,
        )
    }

    Column {
        EffectSlider(
            title = "POSITION",
            range = 0.0..1.0,
            value = shapePosition,
            onValueChange = { beauty.shapePosition.value = it },
        )
        EffectSlider(
            title = "RADIUS",
            range = 0.0..1.0,
            value = shapeRadius,
            onValueChange = { beauty.shapeRadius.value = it },
        )
        EffectSlider(
            title = "STRENGTH",
            range = 0.0..1.0,
            value = shapeStrength,
            onValueChange = { beauty.shapeStrength.value = it },
        )
    }
    LaunchedEffect(shapePosition) {
        setSettings()
    }
    LaunchedEffect(shapeRadius) {
        setSettings()
    }
    LaunchedEffect(shapeStrength) {
        setSettings()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamOverlayRightBeautyView(model: Model, beauty: SettingsBeauty) {
    val settings by beauty.settings.collectAsState()
    val enabled by beauty.enabled.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Column {
        when (settings) {
            SettingsBeautySettings.smoothness -> SmoothnessView(model = model, beauty = beauty)
            SettingsBeautySettings.shape -> ShapeView(model = model, beauty = beauty)
        }
        Row(
            modifier = Modifier
                .padding(end = 10.dp)
                .height(segmentHeight)
                .clip(RoundedCornerShape(7.dp))
                .background(pickerBackgroundColor)
                .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = settings.toString(),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    SettingsBeautySettings.entries.forEach { setting ->
                        DropdownMenuItem(
                            text = { Text(setting.toString()) },
                            onClick = {
                                beauty.settings.value = setting
                                expanded = false
                            },
                        )
                    }
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = { beauty.enabled.value = it },
            )
        }
    }
    LaunchedEffect(enabled) {
        model.updateBeautyButtonState()
        model.sceneUpdated(updateRemoteScene = false)
        if (enabled) {
            model.makeToast(
                title = localized("Other widgets will not work with Beauty filters enabled"),
                subTitle = localized("Too much work to fix it, sorry."),
            )
        }
    }
}
