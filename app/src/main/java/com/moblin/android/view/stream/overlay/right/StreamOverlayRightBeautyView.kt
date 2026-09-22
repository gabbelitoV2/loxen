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
import com.moblin.android.LocalModel

@Composable
private fun SmoothnessView(model: Model = LocalModel.current, beauty: SettingsBeauty) {
    val smoothnessRadius = beauty.smoothnessRadius
    val smoothnessStrength = beauty.smoothnessStrength

    fun setSettings() {
        model.beautyEffect.setSmoothnessSettings(
            radius = beauty.smoothnessRadius,
            strength = beauty.smoothnessStrength,
        )
    }

    Column {
        EffectSlider(
            title = "RADIUS",
            range = 5f..20f,
            value = smoothnessRadius,
            onValueChange = { beauty.smoothnessRadius = it },
        )
        EffectSlider(
            title = "STRENGTH",
            range = 0f..1f,
            value = smoothnessStrength,
            onValueChange = { beauty.smoothnessStrength = it },
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
private fun ShapeView(model: Model = LocalModel.current, beauty: SettingsBeauty) {
    val shapePosition = beauty.shapePosition
    val shapeRadius = beauty.shapeRadius
    val shapeStrength = beauty.shapeStrength

    fun setSettings() {
        model.beautyEffect.setShapeSettings(
            position = beauty.shapePosition,
            radius = beauty.shapeRadius,
            strength = beauty.shapeStrength,
        )
    }

    Column {
        EffectSlider(
            title = "POSITION",
            range = 0f..1f,
            value = shapePosition,
            onValueChange = { beauty.shapePosition = it },
        )
        EffectSlider(
            title = "RADIUS",
            range = 0f..1f,
            value = shapeRadius,
            onValueChange = { beauty.shapeRadius = it },
        )
        EffectSlider(
            title = "STRENGTH",
            range = 0f..1f,
            value = shapeStrength,
            onValueChange = { beauty.shapeStrength = it },
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
fun StreamOverlayRightBeautyView(model: Model = LocalModel.current, beauty: SettingsBeauty) {
    val settings = beauty.settings
    val enabled = beauty.enabled
    var expanded by remember { mutableStateOf(false) }

    Column {
        when (settings) {
            SettingsBeautySettings.smoothness -> SmoothnessView(model = model, beauty = beauty)
            SettingsBeautySettings.shape -> ShapeView(model = model, beauty = beauty)
        }
        Row(
            modifier = Modifier
                .padding(end = 10.dp)
                .height(segmentHeight.dp)
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
                                beauty.settings = setting
                                expanded = false
                            },
                        )
                    }
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = { beauty.enabled = it },
            )
        }
    }
    LaunchedEffect(enabled) {
        model.updateBeautyButtonState()
        TODO("sceneUpdated")
        if (enabled) {
            model.makeToast(
                title = localized("Other widgets will not work with Beauty filters enabled"),
                subTitle = localized("Too much work to fix it, sorry."),
            )
        }
    }
}
