package com.moblin.android.view.utils

import androidx.compose.runtime.Composable

@Composable
fun PositionEditView(
    number: Double,
    onNumberChange: (Double) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    numericInput: Boolean,
    onNumericInputChange: (Boolean) -> Unit,
    incrementImageName: String,
    decrementImageName: String,
    mirror: Boolean,
    increment: Double,
) {
    fun submit(value: String): String {
        val parsed = value.toFloatOrNull()
        if (parsed != null) {
            val clamped = parsed.coerceIn(0f, 100f)
            onSubmit()
            return clamped.toString()
        }
        return value
    }

    ValueEditCompactView(
        number = number,
        onNumberChange = onNumberChange,
        value = value,
        onValueChange = onValueChange,
        minimum = 0.0,
        maximum = 100.0,
        onSubmit = { submitted -> submit(submitted) },
        numericInput = numericInput,
        onNumericInputChange = onNumericInputChange,
        incrementImageName = incrementImageName,
        decrementImageName = decrementImageName,
        mirror = mirror,
        increment = increment,
    )
}
