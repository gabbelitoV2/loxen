package com.moblin.android.view.stream.overlay.right

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.backgroundColor
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.localized
import com.moblin.android.various.model.CameraShow
import com.moblin.android.various.model.CameraShowType
import com.moblin.android.various.model.CameraState
import com.moblin.android.various.model.Database
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.factorToIso
import com.moblin.android.various.utils.formatExposure
import com.moblin.android.various.utils.maximumWhiteBalanceTemperature
import com.moblin.android.various.utils.minimumWhiteBalanceTemperature
import com.moblin.android.LocalModel

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
        modifier = Modifier
            .widthIn(max = cameraButtonWidth)
            .heightIn(max = height)
            .background(pickerBackgroundColor, RoundedCornerShape(7.dp))
            .border(
                1.dp,
                if (on) Color.White else pickerBorderColor,
                RoundedCornerShape(7.dp),
            ),
    ) {
        Text(
            text = localized(title),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 7.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
            if (locked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun NotSupportedForThisCameraView() {
    Text(
        text = localized("Not supported for this camera"),
        color = Color.White,
        modifier = Modifier
            .padding(vertical = 5.dp, horizontal = 7.dp)
            .height(sliderHeight)
            .background(backgroundColor, RoundedCornerShape(7.dp))
            .padding(bottom = 5.dp),
    )
}

@Composable
private fun TitleView(title: String) {
    Text(
        text = localized(title),
        style = MaterialTheme.typography.bodySmall,
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 5.dp, horizontal = 7.dp)
            .width(sliderWidth)
            .height(sliderHeight)
            .background(backgroundColor, RoundedCornerShape(7.dp))
            .padding(bottom = 5.dp),
    ) {
        Slider(
            value = value,
            onValueChange = {
                onEditingChanged(true)
                onValueChange(it)
            },
            onValueChangeFinished = {
                onEditingChanged(false)
            },
            valueRange = 0f..1f,
            steps = (1f / step).toInt() - 1,
            modifier = Modifier.weight(1f),
        )
        LaunchedEffect(value) {
            if (editingLocked) {
                setManual(value)
            }
        }
        IconButton(
            onClick = {
                if (locked) {
                    setAuto()
                } else {
                    setManual(value)
                }
                model.updateImageButtonState()
            },
        ) {
            Icon(
                imageVector = if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ExposureBiasView(model: Model = LocalModel.current, camera: CameraState) {
    val bias by camera.bias.collectAsState()

    TitleView(title = "EXPOSURE BIAS")
    Slider(
        value = bias,
        onValueChange = {
            camera.bias.value = it
            model.setExposureBias(bias = it)
            model.updateImageButtonState()
        },
        onValueChangeFinished = {
            model.setExposureBias(bias = camera.bias.value)
            model.updateImageButtonState()
        },
        valueRange = -2f..2f,
        steps = 39,
        modifier = Modifier
            .padding(vertical = 5.dp, horizontal = 7.dp)
            .width(sliderWidth)
            .height(sliderHeight)
            .background(backgroundColor, RoundedCornerShape(7.dp))
            .padding(bottom = 5.dp),
    )
}

@Composable
private fun WhiteBalanceView(model: Model = LocalModel.current, camera: CameraState) {
    val lockedWhiteBalance by camera.lockedWhiteBalance.collectAsState()
    val isWhiteBalanceLocked by camera.isWhiteBalanceLocked.collectAsState()
    val editingLockedWhiteBalance by camera.editingLockedWhiteBalance.collectAsState()

    TitleView(title = "WHITE BALANCE")
    if (model.isCameraSupportingManualWhiteBalance()) {
        SliderAndLockView(
            model = model,
            value = lockedWhiteBalance,
            onValueChange = { camera.lockedWhiteBalance.value = it },
            locked = isWhiteBalanceLocked,
            editingLocked = editingLockedWhiteBalance,
            onEditingChanged = { begin ->
                camera.editingLockedWhiteBalance.value = begin
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
    val editingLockedIso by camera.editingLockedIso.collectAsState()

    TitleView(title = "ISO")
    if (model.isCameraSupportingManualExposureAndIso()) {
        SliderAndLockView(
            model = model,
            value = lockedIso,
            onValueChange = { camera.lockedIso.value = it },
            locked = isExposureAndIsoLocked,
            editingLocked = editingLockedIso,
            onEditingChanged = { begin ->
                camera.editingLockedIso.value = begin
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
    val editingLockedExposure by camera.editingLockedExposure.collectAsState()

    TitleView(title = "EXPOSURE")
    if (model.isCameraSupportingManualExposureAndIso()) {
        SliderAndLockView(
            model = model,
            value = lockedExposure,
            onValueChange = { camera.lockedExposure.value = it },
            locked = isExposureAndIsoLocked,
            editingLocked = editingLockedExposure,
            onEditingChanged = { begin ->
                camera.editingLockedExposure.value = begin
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
    val editingLockedFocus by camera.editingLockedFocus.collectAsState()

    TitleView(title = "FOCUS")
    if (model.isCameraSupportingManualFocus()) {
        SliderAndLockView(
            model = model,
            value = lockedFocus,
            onValueChange = { camera.lockedFocus.value = it },
            locked = isFocusLocked,
            editingLocked = editingLockedFocus,
            onEditingChanged = { begin ->
                camera.editingLockedFocus.value = begin
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
    val bigButtons by database.bigButtons.collectAsState()

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
        val device = model.cameraDevice ?: return ""
        return factorToIso(device, lockedIso).toInt().toString()
    }

    fun formatFocus(): String {
        return (lockedFocus * 100).toInt().toString()
    }

    fun height(): Dp {
        return if (bigButtons) {
            segmentHeightBig
        } else {
            segmentHeight
        }
    }

    Row(modifier = modifier) {
        IconButton(
            onClick = { show.toggle(buttonType = CameraShowType.BIAS) },
        ) {
            CameraSettingButtonView(
                title = "EXB",
                value = formatExposureBias(),
                locked = true,
                on = showType == CameraShowType.BIAS,
                height = height(),
            )
        }
        IconButton(
            onClick = { show.toggle(buttonType = CameraShowType.WHITE_BALANCE) },
        ) {
            CameraSettingButtonView(
                title = "WB",
                value = formatWhiteBalance(),
                locked = isWhiteBalanceLocked,
                on = showType == CameraShowType.WHITE_BALANCE,
                height = height(),
            )
        }
        IconButton(
            onClick = { show.toggle(buttonType = CameraShowType.ISO) },
        ) {
            CameraSettingButtonView(
                title = "ISO",
                value = formatIso(),
                locked = isExposureAndIsoLocked,
                on = showType == CameraShowType.ISO,
                height = height(),
            )
        }
        IconButton(
            onClick = { show.toggle(buttonType = CameraShowType.EXPOSURE) },
        ) {
            CameraSettingButtonView(
                title = "EXP",
                value = formatExposure(exposure),
                locked = isExposureAndIsoLocked,
                on = showType == CameraShowType.EXPOSURE,
                height = height(),
            )
        }
        IconButton(
            onClick = { show.toggle(buttonType = CameraShowType.FOCUS) },
        ) {
            CameraSettingButtonView(
                title = "FOC",
                value = formatFocus(),
                locked = isFocusLocked,
                on = showType == CameraShowType.FOCUS,
                height = height(),
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
            CameraShowType.BIAS -> ExposureBiasView(model = model, camera = camera)
            CameraShowType.WHITE_BALANCE -> WhiteBalanceView(model = model, camera = camera)
            CameraShowType.ISO -> IsoView(model = model, camera = camera)
            CameraShowType.EXPOSURE -> ExposureView(model = model, camera = camera)
            CameraShowType.FOCUS -> FocusView(model = model, camera = camera)
            null -> Unit
        }
        ButtonsView(
            model = model,
            database = model.database,
            camera = camera,
            show = show,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        LaunchedEffect(Unit) {
            model.startObservingFocus()
            model.startObservingIso()
            model.startObservingExposure()
            model.startObservingWhiteBalance()
        }
        DisposableEffect(Unit) {
            onDispose {
                model.stopObservingFocus()
                model.stopObservingIso()
                model.stopObservingExposure()
                model.stopObservingWhiteBalance()
            }
        }
    }
}
