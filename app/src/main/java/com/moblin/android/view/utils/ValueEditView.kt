package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val sliderSteps = if (increment > 0f) {
        (((maximum - minimum) / increment).toInt() - 1).coerceAtLeast(0)
    } else {
        0
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.width(70.dp)) {
                Text(title)
                Spacer(Modifier.weight(1f))
            }
            OutlinedTextField(
                value = valueState,
                onValueChange = { valueState = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            valueState = onSubmit(valueState.trim())
                            add(0f)
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    valueState = onSubmit(valueState.trim())
                    add(0f)
                }),
            )
            unit?.let { unitText ->
                Text(unitText)
            }
            HorizontalDivider()
            TextButton(onClick = {
                add(-increment)
                valueState = onSubmit(valueState.trim())
                add(0f)
            }) {
                Text(
                    text = "-",
                    fontSize = 25.sp,
                    modifier = Modifier.width(40.dp),
                )
            }
            HorizontalDivider()
            TextButton(onClick = {
                add(increment)
                valueState = onSubmit(valueState.trim())
                add(0f)
            }) {
                Text(
                    text = "+",
                    fontSize = 25.sp,
                    modifier = Modifier.width(40.dp),
                )
            }
            HorizontalDivider()
        }
        Slider(
            value = numberState,
            onValueChange = { numberState = it },
            valueRange = minimum..maximum,
            steps = sliderSteps,
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
    fun add(offset: Double) {
        val parsed = value.toDoubleOrNull()
        if (parsed != null) {
            val newNumber = (parsed + offset).coerceIn(minimum, maximum)
            onNumberChange(newNumber)
            onValueChange(newNumber.toString())
        }
    }

    val step = 15 * increment
    val sliderSteps = if (step > 0.0) {
        (((maximum - minimum) / step).toInt() - 1).coerceAtLeast(0)
    } else {
        0
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (numericInput) {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            onValueChange(onSubmit(value.trim()))
                            add(0.0)
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    onValueChange(onSubmit(value.trim()))
                    add(0.0)
                }),
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

            Slider(
                value = number.toFloat(),
                onValueChange = { onNumberChange(it.toDouble()) },
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { rotationZ = if (mirror) 180f else 0f },
                valueRange = minimum.toFloat()..maximum.toFloat(),
                steps = sliderSteps,
            )
        }
        IconButton(
            onClick = {
                add(if (mirror) increment else -increment)
                onValueChange(onSubmit(value.trim()))
                add(0.0)
            },
            enabled = number != (if (mirror) maximum else minimum),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = decrementImageName,
                modifier = Modifier.size(28.dp),
            )
        }
        IconButton(
            onClick = {
                add(if (mirror) -increment else increment)
                onValueChange(onSubmit(value.trim()))
                add(0.0)
            },
            enabled = number != (if (mirror) minimum else maximum),
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = incrementImageName,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
