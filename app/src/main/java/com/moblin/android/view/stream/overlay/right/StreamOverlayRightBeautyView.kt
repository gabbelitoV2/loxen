package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.IosSwitch
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.sceneUpdated
import com.moblin.android.various.settings.SettingsBeauty
import com.moblin.android.various.settings.SettingsBeautySettings
import com.moblin.android.LocalModel

@Composable
private fun SmoothnessView(model: Model = LocalModel.current, beauty: SettingsBeauty) {
    fun setSettings() {
        model.beautyEffect.setSmoothnessSettings(
            radius = beauty.smoothnessRadius,
            strength = beauty.smoothnessStrength,
        )
    }

    var smoothnessRadius by binding({ beauty.smoothnessRadius }) { beauty.smoothnessRadius = it }
    var smoothnessStrength by binding({ beauty.smoothnessStrength }) { beauty.smoothnessStrength = it }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        EffectSlider(
            title = "RADIUS",
            range = 5f..20f,
            value = smoothnessRadius,
            onValueChange = {
                if (it != smoothnessRadius) {
                    smoothnessRadius = it
                    setSettings()
                }
            },
        )
        EffectSlider(
            title = "STRENGTH",
            range = 0f..1f,
            value = smoothnessStrength,
            onValueChange = {
                if (it != smoothnessStrength) {
                    smoothnessStrength = it
                    setSettings()
                }
            },
        )
    }
}

@Composable
private fun ShapeView(model: Model = LocalModel.current, beauty: SettingsBeauty) {
    fun setSettings() {
        model.beautyEffect.setShapeSettings(
            position = beauty.shapePosition,
            radius = beauty.shapeRadius,
            strength = beauty.shapeStrength,
        )
    }

    var shapePosition by binding({ beauty.shapePosition }) { beauty.shapePosition = it }
    var shapeRadius by binding({ beauty.shapeRadius }) { beauty.shapeRadius = it }
    var shapeStrength by binding({ beauty.shapeStrength }) { beauty.shapeStrength = it }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        EffectSlider(
            title = "POSITION",
            range = 0f..1f,
            value = shapePosition,
            onValueChange = {
                if (it != shapePosition) {
                    shapePosition = it
                    setSettings()
                }
            },
        )
        EffectSlider(
            title = "RADIUS",
            range = 0f..1f,
            value = shapeRadius,
            onValueChange = {
                if (it != shapeRadius) {
                    shapeRadius = it
                    setSettings()
                }
            },
        )
        EffectSlider(
            title = "STRENGTH",
            range = 0f..1f,
            value = shapeStrength,
            onValueChange = {
                if (it != shapeStrength) {
                    shapeStrength = it
                    setSettings()
                }
            },
        )
    }
}

@Composable
fun StreamOverlayRightBeautyView(model: Model = LocalModel.current, beauty: SettingsBeauty) {
    var settings by binding({ beauty.settings }) { beauty.settings = it }
    var enabled by binding({ beauty.enabled }) { beauty.enabled = it }
    val shape = RoundedCornerShape(7.dp)

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        when (settings) {
            SettingsBeautySettings.smoothness -> SmoothnessView(model = model, beauty = beauty)
            SettingsBeautySettings.shape -> ShapeView(model = model, beauty = beauty)
        }
        Row(
            modifier = Modifier
                .height(segmentHeight.dp)
                .clip(shape)
                .background(pickerBackgroundColor)
                .border(1.dp, pickerBorderColor, shape)
                .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalTint provides Color.White) {
                Picker(
                    "",
                    selection = settings,
                    options = SettingsBeautySettings.entries,
                    text = { it.toString() },
                    pickerStyle = PickerStyle.menu,
                ) {
                    settings = it
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IosSwitch(
                checked = enabled,
                onCheckedChange = {
                    if (it != enabled) {
                        enabled = it
                        model.updateBeautyButtonState()
                        model.sceneUpdated(updateRemoteScene = false)
                        if (it) {
                            model.makeToast(
                                title = localized("Other widgets will not work with Beauty filters enabled"),
                                subTitle = localized("Too much work to fix it, sorry."),
                            )
                        }
                    }
                },
            )
        }
    }
}
