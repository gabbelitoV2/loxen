package com.moblin.android.view.utils

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.FormSlider
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
fun ValueEditView(
    title: String,
    number: Float,
    value: String,
    minimum: Float,
    maximum: Float,
    onSubmit: (String) -> String,
    increment: Float = 1f,
    unit: String? = null,
) {
    val palette = formPalette()
    var numberState by remember { mutableStateOf(number) }
    var valueState by remember { mutableStateOf(value) }
    var initialised by remember { mutableStateOf(false) }

    fun add(offset: Float) {
        val parsed = valueState.toFloatOrNull()
        if (parsed != null) {
            numberState = (parsed + offset).coerceIn(minimum, maximum)
            valueState = numberState.toString()
        }
    }

    fun commit() {
        valueState = onSubmit(valueState.trim())
        add(0f)
    }

    fun snap(raw: Float): Float {
        if (increment <= 0f) {
            return raw.coerceIn(minimum, maximum)
        }
        val steps = ((raw - minimum) / increment).roundToInt()
        return (minimum + steps * increment).coerceIn(minimum, maximum)
    }

    val minusInteractionSource = remember { MutableInteractionSource() }
    val minusPressed by minusInteractionSource.collectIsPressedAsState()
    val plusInteractionSource = remember { MutableInteractionSource() }
    val plusPressed by plusInteractionSource.collectIsPressedAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.width(70.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = title, color = palette.label)
                Spacer(Modifier.weight(1f))
            }
            BasicTextField(
                value = valueState,
                onValueChange = { valueState = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            commit()
                        }
                    },
                textStyle = formBodyStyle.copy(color = palette.label),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    commit()
                }),
                cursorBrush = SolidColor(palette.accent),
            )
            unit?.let { unitName ->
                Text(text = unitName, color = palette.label)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(palette.separator),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(40.dp)
                    .alpha(if (minusPressed) 0.2f else 1f)
                    .clickable(
                        interactionSource = minusInteractionSource,
                        indication = null,
                    ) {
                        add(-increment)
                        commit()
                    },
            ) {
                Text(text = "-", fontSize = 25.sp, color = palette.accent)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(palette.separator),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(40.dp)
                    .alpha(if (plusPressed) 0.2f else 1f)
                    .clickable(
                        interactionSource = plusInteractionSource,
                        indication = null,
                    ) {
                        add(increment)
                        commit()
                    },
            ) {
                Text(text = "+", fontSize = 25.sp, color = palette.accent)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(palette.separator),
            )
        }
        FormSlider(
            value = numberState,
            onValueChange = { raw -> numberState = snap(raw) },
            valueRange = minimum..maximum,
        )
    }

    LaunchedEffect(numberState) {
        if (initialised) {
            valueState = onSubmit(numberState.toString())
        } else {
            initialised = true
        }
    }
}

@Composable
fun ValueEditCompactView(
    number: Double,
    onNumberChange: (Double) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    minimum: Double,
    maximum: Double,
    onSubmit: (String) -> String,
    numericInput: Boolean,
    onNumericInputChange: (Boolean) -> Unit,
    incrementImageName: String,
    decrementImageName: String,
    mirror: Boolean,
    increment: Double = 1.0,
) {
    val palette = formPalette()

    fun addTo(text: String, offset: Double): String {
        val parsed = text.toDoubleOrNull() ?: return text
        val newNumber = (parsed + offset).coerceIn(minimum, maximum)
        onNumberChange(newNumber)
        val newValue = newNumber.toString()
        onValueChange(newValue)
        return newValue
    }

    fun commit() {
        addTo(onSubmit(value.trim()), 0.0)
    }

    val step = 15 * increment

    fun snap(raw: Double): Double {
        if (step <= 0.0) {
            return raw.coerceIn(minimum, maximum)
        }
        val steps = ((raw - minimum) / step).roundToLong()
        return (minimum + steps * step).coerceIn(minimum, maximum)
    }

    val decrementInteractionSource = remember { MutableInteractionSource() }
    val decrementPressed by decrementInteractionSource.collectIsPressedAsState()
    val incrementInteractionSource = remember { MutableInteractionSource() }
    val incrementPressed by incrementInteractionSource.collectIsPressedAsState()
    val decrementEnabled = number != (if (mirror) maximum else minimum)
    val incrementEnabled = number != (if (mirror) minimum else maximum)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (numericInput) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            commit()
                        }
                    },
                textStyle = formBodyStyle.copy(color = palette.label),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    commit()
                }),
                cursorBrush = SolidColor(palette.accent),
            )
        } else {
            var initialised by remember { mutableStateOf(false) }

            LaunchedEffect(number) {
                if (initialised) {
                    onValueChange(onSubmit(number.toString()))
                } else {
                    initialised = true
                }
            }

            FormSlider(
                value = number.toFloat(),
                onValueChange = { raw -> onNumberChange(snap(raw.toDouble())) },
                modifier = Modifier
                    .weight(1f)
                    .rotate(if (mirror) 180f else 0f),
                valueRange = minimum.toFloat()..maximum.toFloat(),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .alpha(if (decrementPressed) 0.2f else 1f)
                .clickable(
                    interactionSource = decrementInteractionSource,
                    indication = null,
                    enabled = decrementEnabled,
                ) {
                    addTo(onSubmit(addTo(value, if (mirror) increment else -increment).trim()), 0.0)
                },
        ) {
            SystemImage(
                name = decrementImageName,
                fontSize = 28.sp,
                tint = if (decrementEnabled) palette.accent else palette.gray,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .alpha(if (incrementPressed) 0.2f else 1f)
                .clickable(
                    interactionSource = incrementInteractionSource,
                    indication = null,
                    enabled = incrementEnabled,
                ) {
                    addTo(onSubmit(addTo(value, if (mirror) -increment else increment).trim()), 0.0)
                },
        ) {
            SystemImage(
                name = incrementImageName,
                fontSize = 28.sp,
                tint = if (incrementEnabled) palette.accent else palette.gray,
            )
        }
    }
}
