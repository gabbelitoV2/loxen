package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.localized
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsFace
import com.moblin.android.various.settings.SettingsFacePrivacyMode
import kotlin.math.roundToInt
import com.moblin.android.LocalModel

@Composable
fun EffectSlider(
    title: String,
    range: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = localized(title),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(end = 7.dp),
        )
        Row(
            modifier = Modifier
                .padding(vertical = 5.dp)
                .padding(horizontal = 7.dp)
                .size(width = sliderWidth.dp, height = sliderHeight.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(backgroundColor)
                .padding(bottom = 5.dp),
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                steps = (((range.endInclusive - range.start) / 0.01f).roundToInt() - 1).coerceAtLeast(0),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamOverlayRightFaceView(model: Model = LocalModel.current, face: SettingsFace) {
    var selectedImageItem by remember { mutableStateOf<ByteArray?>(null) }
    val blurFaces = face.blurFaces
    val blurText = face.blurText
    val blurBackground = face.blurBackground
    val privacyMode = face.privacyMode
    val blurStrength = face.blurStrength
    val pixellateStrength = face.pixellateStrength

    if (blurFaces || blurText || blurBackground) {
        Row(
            modifier = Modifier.padding(bottom = 5.dp),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Spacer(modifier = Modifier.weight(1f))
                when (privacyMode) {
                    SettingsFacePrivacyMode.blur -> {
                        EffectSlider(
                            title = "BLUR STRENGTH",
                            range = 0.1f..1f,
                            value = blurStrength,
                            onValueChange = { value ->
                                model.updateFaceFilterSettings()
                                TODO("set face.blurStrength = $value")
                            },
                        )
                        LaunchedEffect(blurStrength) {
                            model.updateFaceFilterSettings()
                        }
                    }
                    SettingsFacePrivacyMode.pixellate -> {
                        EffectSlider(
                            title = "PIXELLATE STRENGTH",
                            range = 0f..1f,
                            value = pixellateStrength,
                            onValueChange = { value ->
                                model.updateFaceFilterSettings()
                                TODO("set face.pixellateStrength = $value")
                            },
                        )
                        LaunchedEffect(pixellateStrength) {
                            model.updateFaceFilterSettings()
                        }
                    }
                    SettingsFacePrivacyMode.backgroundImage -> {
                        TODO("no Android counterpart for PhotosPicker")
                    }
                    SettingsFacePrivacyMode.icon -> {
                    }
                }
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier
                        .height(segmentHeight.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(pickerBackgroundColor)
                        .border(1.dp, pickerBorderColor, RoundedCornerShape(7.dp)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(segmentHeight.dp)
                            .clickable { expanded = true },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = privacyMode.toString(),
                            color = Color.White,
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        SettingsFacePrivacyMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.toString()) },
                                onClick = {
                                    expanded = false
                                    TODO("set face.privacyMode = $mode")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
