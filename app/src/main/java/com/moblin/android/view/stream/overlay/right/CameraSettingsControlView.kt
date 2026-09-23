package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.various.model.CameraShow
import com.moblin.android.various.model.CameraShowType
import com.moblin.android.various.model.CameraState
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getExposureFactorStep
import com.moblin.android.various.model.isCameraSupportingManualExposureAndIso
import com.moblin.android.various.model.isCameraSupportingManualFocus
import com.moblin.android.various.model.isCameraSupportingManualWhiteBalance
import com.moblin.android.various.model.setAutoExposureAndIso
import com.moblin.android.various.model.setAutoFocus
import com.moblin.android.various.model.setAutoWhiteBalance
import com.moblin.android.various.model.setExposureBias
import com.moblin.android.various.model.setManualExposure
import com.moblin.android.various.model.setManualFocus
import com.moblin.android.various.model.setManualIso
import com.moblin.android.various.model.setManualWhiteBalance
import com.moblin.android.various.model.startObservingExposure
import com.moblin.android.various.model.startObservingFocus
import com.moblin.android.various.model.startObservingIso
import com.moblin.android.various.model.startObservingWhiteBalance
import com.moblin.android.various.model.stopObservingExposure
import com.moblin.android.various.model.stopObservingFocus
import com.moblin.android.various.model.stopObservingIso
import com.moblin.android.various.model.stopObservingWhiteBalance
import com.moblin.android.various.settings.Database
import com.moblin.android.platform.avfoundation.AVCaptureDevice
import com.moblin.android.various.utils.factorToIso
import com.moblin.android.various.utils.formatExposure
import com.moblin.android.various.utils.maximumWhiteBalanceTemperature
import com.moblin.android.various.utils.minimumWhiteBalanceTemperature
import kotlin.math.roundToInt

@Composable
private fun <T> OnChange(value: T, action: (T) -> Unit) {
    val currentAction by rememberUpdatedState(action)
    var previous by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value != previous) {
            previous = value
            currentAction(value)
        }
    }
}

@Composable
private fun PlainButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .alpha(if (pressed) 0.2f else 1f)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun sliderSteps(range: ClosedFloatingPointRange<Float>, step: Float): Int =
    ((range.endInclusive - range.start) / step).roundToInt().minus(1).coerceAtLeast(0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val minimumTrackColor = if (dark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val maximumTrackColor = Color(red = 120, green = 120, blue = 128).copy(alpha = if (dark) 0.36f else 0.2f)
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        onValueChangeFinished = onValueChangeFinished,
        steps = sliderSteps(valueRange, step),
        thumb = {
            Box(
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = CircleShape)
                    .size(28.dp)
                    .background(Color.White, CircleShape),
            )
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = if (span > 0f) {
                ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            } else {
                0f
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
            ) {
                val inset = 14.dp.toPx()
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(
                    color = maximumTrackColor,
                    topLeft = Offset(-inset, 0f),
                    size = Size(size.width + 2 * inset, size.height),
                    cornerRadius = radius,
                )
                drawRoundRect(
                    color = minimumTrackColor,
                    topLeft = Offset(-inset, 0f),
                    size = Size(size.width * fraction + inset, size.height),
                    cornerRadius = radius,
                )
            }
        },
        valueRange = valueRange,
    )
}

@Composable
private fun CameraSettingButtonView(
    title: String,
    value: String,
    locked: Boolean,
    on: Boolean,
    height: Dp,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .sizeIn(maxWidth = cameraButtonWidth.dp, maxHeight = height)
            .fillMaxSize()
            .clip(RoundedCornerShape(7.dp))
            .background(pickerBackgroundColor)
            .border(
                1.dp,
                if (on) Color.White else pickerBorderColor,
                RoundedCornerShape(7.dp),
            ),
    ) {
        Text(
            text = localized(title),
            fontSize = 15.sp,
            color = Color.White,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 7.dp),
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = Color.White,
            )
            if (locked) {
                SystemImage(name = "lock", fontSize = 13.sp, tint = Color.White)
            }
        }
    }
}

@Composable
private fun NotSupportedForThisCameraView() {
    Box(
        modifier = Modifier
            .padding(bottom = 5.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor)
            .height(sliderHeight.dp)
            .padding(vertical = 5.dp, horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = localized("Not supported for this camera"),
            fontSize = 17.sp,
            color = Color.White,
        )
    }
}

@Composable
private fun TitleView(title: String) {
    Text(
        text = localized(title),
        fontSize = 13.sp,
        color = Color.White,
        modifier = Modifier.padding(end = 7.dp),
    )
}

@Composable
private fun SliderAndLockView(
    model: Model = LocalModel.current,
    value: Float,
    onValueChange: (Float) -> Unit,
    locked: Boolean,
    editingLocked: Boolean,
    onEditingChanged: (Boolean) -> Unit,
    setManual: (Float) -> Unit,
    setAuto: () -> Unit,
    step: Float = 0.01f,
) {
    OnChange(value) { newValue ->
        if (editingLocked) {
            setManual(newValue)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(bottom = 5.dp)
            .size(width = sliderWidth.dp, height = sliderHeight.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor)
            .padding(vertical = 5.dp, horizontal = 7.dp),
    ) {
        CameraSlider(
            value = value,
            onValueChange = {
                onEditingChanged(true)
                onValueChange(it)
            },
            onValueChangeFinished = {
                onEditingChanged(false)
            },
            valueRange = 0f..1f,
            step = step,
            modifier = Modifier.weight(1f),
        )
        PlainButton(
            onClick = {
                if (locked) {
                    setAuto()
                } else {
                    setManual(value)
                }
                model.updateImageButtonState()
            },
        ) {
            SystemImage(name = if (locked) "lock" else "lock.open", fontSize = 22.sp, tint = Color.White)
        }
    }
}

@Composable
private fun ExposureBiasView(model: Model = LocalModel.current, camera: CameraState) {
    val bias by camera.bias.collectAsState()

    OnChange(bias) {
        model.setExposureBias(bias = camera.bias.value)
        model.updateImageButtonState()
    }
    TitleView(title = "EXPOSURE BIAS")
    Box(
        modifier = Modifier
            .padding(bottom = 5.dp)
            .size(width = sliderWidth.dp, height = sliderHeight.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(backgroundColor)
            .padding(vertical = 5.dp, horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        CameraSlider(
            value = bias,
            onValueChange = { camera.setBias(value = it) },
            onValueChangeFinished = {
                model.setExposureBias(bias = camera.bias.value)
                model.updateImageButtonState()
            },
            valueRange = -2f..2f,
            step = 0.1f,
        )
    }
}

@Composable
private fun WhiteBalanceView(model: Model = LocalModel.current, camera: CameraState) {
    val lockedWhiteBalance by camera.lockedWhiteBalance.collectAsState()
    val isWhiteBalanceLocked by camera.isWhiteBalanceLocked.collectAsState()

    TitleView(title = "WHITE BALANCE")
    if (model.isCameraSupportingManualWhiteBalance()) {
        SliderAndLockView(
            model = model,
            value = lockedWhiteBalance,
            onValueChange = { camera.setLockedWhiteBalance(value = it) },
            locked = isWhiteBalanceLocked,
            editingLocked = camera.editingLockedWhiteBalance,
            onEditingChanged = { begin ->
                camera.editingLockedWhiteBalance = begin
                if (!begin) {
                    model.setManualWhiteBalance(factor = camera.lockedWhiteBalance.value)
                }
            },
            setManual = { model.setManualWhiteBalance(factor = it) },
            setAuto = { model.setAutoWhiteBalance() },
        )
    } else {
        NotSupportedForThisCameraView()
    }
}

@Composable
private fun IsoView(model: Model = LocalModel.current, camera: CameraState) {
    val lockedIso by camera.lockedIso.collectAsState()
    val isExposureAndIsoLocked by camera.isExposureAndIsoLocked.collectAsState()

    TitleView(title = "ISO")
    if (model.isCameraSupportingManualExposureAndIso()) {
        SliderAndLockView(
            model = model,
            value = lockedIso,
            onValueChange = { camera.setLockedIso(value = it) },
            locked = isExposureAndIsoLocked,
            editingLocked = camera.editingLockedIso,
            onEditingChanged = { begin ->
                camera.editingLockedIso = begin
                if (!begin) {
                    model.setManualIso(factor = camera.lockedIso.value)
                }
            },
            setManual = { model.setManualIso(factor = it) },
            setAuto = { model.setAutoExposureAndIso() },
        )
    } else {
        NotSupportedForThisCameraView()
    }
}

@Composable
private fun ExposureView(model: Model = LocalModel.current, camera: CameraState) {
    val lockedExposure by camera.lockedExposure.collectAsState()
    val isExposureAndIsoLocked by camera.isExposureAndIsoLocked.collectAsState()

    TitleView(title = "EXPOSURE")
    if (model.isCameraSupportingManualExposureAndIso()) {
        SliderAndLockView(
            model = model,
            value = lockedExposure,
            onValueChange = { camera.setLockedExposure(value = it) },
            locked = isExposureAndIsoLocked,
            editingLocked = camera.editingLockedExposure,
            onEditingChanged = { begin ->
                camera.editingLockedExposure = begin
                if (!begin) {
                    model.setManualExposure(factor = camera.lockedExposure.value)
                }
            },
            setManual = { model.setManualExposure(factor = it) },
            setAuto = { model.setAutoExposureAndIso() },
            step = model.getExposureFactorStep(),
        )
    } else {
        NotSupportedForThisCameraView()
    }
}

@Composable
fun FocusView(model: Model = LocalModel.current, camera: CameraState) {
    val lockedFocus by camera.lockedFocus.collectAsState()
    val isFocusLocked by camera.isFocusLocked.collectAsState()

    TitleView(title = "FOCUS")
    if (model.isCameraSupportingManualFocus()) {
        SliderAndLockView(
            model = model,
            value = lockedFocus,
            onValueChange = { camera.setLockedFocus(value = it) },
            locked = isFocusLocked,
            editingLocked = camera.editingLockedFocus,
            onEditingChanged = { begin ->
                camera.editingLockedFocus = begin
                if (!begin) {
                    model.setManualFocus(lensPosition = camera.lockedFocus.value)
                }
            },
            setManual = { model.setManualFocus(lensPosition = it) },
            setAuto = { model.setAutoFocus() },
        )
    } else {
        NotSupportedForThisCameraView()
    }
}

@Composable
private fun ButtonsView(
    model: Model = LocalModel.current,
    database: Database,
    camera: CameraState,
    show: CameraShow,
    modifier: Modifier = Modifier,
) {
    val showType by show.type.collectAsState()
    val bias by camera.bias.collectAsState()
    val lockedWhiteBalance by camera.lockedWhiteBalance.collectAsState()
    val lockedIso by camera.lockedIso.collectAsState()
    val lockedFocus by camera.lockedFocus.collectAsState()
    val exposure by camera.exposure.collectAsState()
    val isWhiteBalanceLocked by camera.isWhiteBalanceLocked.collectAsState()
    val isExposureAndIsoLocked by camera.isExposureAndIsoLocked.collectAsState()
    val isFocusLocked by camera.isFocusLocked.collectAsState()

    fun formatExposureBias(): String {
        var value = formatOneDecimal(bias)
        if (bias >= 0.0f) {
            value = "+$value"
        }
        return value
    }

    fun formatWhiteBalance(): String {
        return (
            minimumWhiteBalanceTemperature +
                (maximumWhiteBalanceTemperature - minimumWhiteBalanceTemperature) * lockedWhiteBalance
            ).toInt().toString()
    }

    fun formatIso(): String {
        val device = model.cameraDevice?.device as? AVCaptureDevice ?: return ""
        return factorToIso(device = device, factor = lockedIso).toInt().toString()
    }

    fun formatFocus(): String {
        return (lockedFocus * 100).toInt().toString()
    }

    fun height(): Dp {
        return if (database.bigButtons) {
            segmentHeightBig.dp
        } else {
            segmentHeight.dp
        }
    }

    val buttonHeight = height()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlainButton(onClick = { show.toggle(buttonType = CameraShowType.bias) }) {
            CameraSettingButtonView(
                title = "EXB",
                value = formatExposureBias(),
                locked = true,
                on = showType == CameraShowType.bias,
                height = buttonHeight,
            )
        }
        PlainButton(onClick = { show.toggle(buttonType = CameraShowType.whiteBalance) }) {
            CameraSettingButtonView(
                title = "WB",
                value = formatWhiteBalance(),
                locked = isWhiteBalanceLocked,
                on = showType == CameraShowType.whiteBalance,
                height = buttonHeight,
            )
        }
        PlainButton(onClick = { show.toggle(buttonType = CameraShowType.iso) }) {
            CameraSettingButtonView(
                title = "ISO",
                value = formatIso(),
                locked = isExposureAndIsoLocked,
                on = showType == CameraShowType.iso,
                height = buttonHeight,
            )
        }
        PlainButton(onClick = { show.toggle(buttonType = CameraShowType.exposure) }) {
            CameraSettingButtonView(
                title = "EXP",
                value = formatExposure(exposure),
                locked = isExposureAndIsoLocked,
                on = showType == CameraShowType.exposure,
                height = buttonHeight,
            )
        }
        PlainButton(onClick = { show.toggle(buttonType = CameraShowType.focus) }) {
            CameraSettingButtonView(
                title = "FOC",
                value = formatFocus(),
                locked = isFocusLocked,
                on = showType == CameraShowType.focus,
                height = buttonHeight,
            )
        }
    }
}

@Composable
fun StreamOverlayRightCameraSettingsControlView(
    model: Model = LocalModel.current,
    camera: CameraState,
    show: CameraShow,
) {
    val showType by show.type.collectAsState()

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        when (showType) {
            CameraShowType.bias -> ExposureBiasView(model = model, camera = camera)
            CameraShowType.whiteBalance -> WhiteBalanceView(model = model, camera = camera)
            CameraShowType.iso -> IsoView(model = model, camera = camera)
            CameraShowType.exposure -> ExposureView(model = model, camera = camera)
            CameraShowType.focus -> FocusView(model = model, camera = camera)
            null -> {}
        }
        DisposableEffect(Unit) {
            model.startObservingFocus()
            model.startObservingIso()
            model.startObservingExposure()
            model.startObservingWhiteBalance()
            onDispose {
                model.stopObservingFocus()
                model.stopObservingIso()
                model.stopObservingExposure()
                model.stopObservingWhiteBalance()
            }
        }
        ButtonsView(
            model = model,
            database = model.database,
            camera = camera,
            show = show,
            modifier = Modifier.padding(bottom = 5.dp),
        )
    }
}
