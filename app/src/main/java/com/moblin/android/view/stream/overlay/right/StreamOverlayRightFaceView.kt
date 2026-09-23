package com.moblin.android.view.stream.overlay.right

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.LocalTint
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.various.logger
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.loadFaceBackgroundImage
import com.moblin.android.various.model.saveFaceBackgroundImage
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
    var current by remember(value) { mutableFloatStateOf(value) }
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = localized(title),
            style = formFootnoteStyle,
            color = Color.White,
            modifier = Modifier.padding(end = 7.dp),
        )
        Row(
            modifier = Modifier
                .padding(bottom = 5.dp)
                .size(width = sliderWidth.dp, height = sliderHeight.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(backgroundColor)
                .padding(horizontal = 7.dp)
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FormSlider(
                value = current,
                onValueChange = {
                    val stepped = (range.start + ((it - range.start) / 0.01f).roundToInt() * 0.01f)
                        .coerceIn(range.start, range.endInclusive)
                    if (stepped != current) {
                        current = stepped
                        onValueChange(stepped)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                valueRange = range,
            )
        }
    }
}

@Composable
fun StreamOverlayRightFaceView(model: Model = LocalModel.current, face: SettingsFace) {
    val context = LocalContext.current
    val blurFaces = face.blurFaces
    val blurText = face.blurText
    val blurBackground = face.blurBackground
    var privacyMode by binding({ face.privacyMode }) { face.privacyMode = it }
    var blurStrength by binding({ face.blurStrength }) { face.blurStrength = it }
    var pixellateStrength by binding({ face.pixellateStrength }) { face.pixellateStrength = it }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val data = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            data.onSuccess {
                if (it != null) {
                    model.saveFaceBackgroundImage(data = it)
                    model.loadFaceBackgroundImage()
                } else {
                    logger.info("face: background image is nil")
                }
            }.onFailure {
                logger.info("face: background image error: $it")
            }
        }
    }
    val shape = RoundedCornerShape(7.dp)

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
                            onValueChange = {
                                if (it != blurStrength) {
                                    blurStrength = it
                                    model.updateFaceFilterSettings()
                                }
                            },
                        )
                    }
                    SettingsFacePrivacyMode.pixellate -> {
                        EffectSlider(
                            title = "PIXELLATE STRENGTH",
                            range = 0f..1f,
                            value = pixellateStrength,
                            onValueChange = {
                                if (it != pixellateStrength) {
                                    pixellateStrength = it
                                    model.updateFaceFilterSettings()
                                }
                            },
                        )
                    }
                    SettingsFacePrivacyMode.backgroundImage -> {
                        val interactionSource = remember { MutableInteractionSource() }
                        val pressed by interactionSource.collectIsPressedAsState()
                        Box(
                            modifier = Modifier
                                .padding(bottom = 5.dp)
                                .alpha(if (pressed) 0.2f else 1f)
                                .clickable(interactionSource = interactionSource, indication = null) {
                                    imagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                }
                                .height(sliderHeight.dp)
                                .clip(shape)
                                .background(backgroundColor)
                                .border(1.dp, pickerBorderColor, shape)
                                .padding(horizontal = 7.dp)
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = localized("Select image"), style = formBodyStyle, color = Color.White)
                        }
                    }
                    SettingsFacePrivacyMode.icon -> {
                    }
                }
                Box(
                    modifier = Modifier
                        .height(segmentHeight.dp)
                        .clip(shape)
                        .background(pickerBackgroundColor)
                        .border(1.dp, pickerBorderColor, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalTint provides Color.White) {
                        Picker(
                            "",
                            selection = privacyMode,
                            options = SettingsFacePrivacyMode.entries,
                            text = { it.toString() },
                            pickerStyle = PickerStyle.menu,
                        ) {
                            privacyMode = it
                            model.updateFaceFilterSettings()
                        }
                    }
                }
            }
        }
    }
}
