package com.moblin.android.view.utils

import androidx.compose.runtime.Composable

@Composable
fun SizeEditView(
    number: Double,
    onNumberChange: (Double) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    numericInput: Boolean,
    onNumericInputChange: (Boolean) -> Unit,
) {
    fun submit(value: String): String {
        val parsed = value.toDoubleOrNull()
        if (parsed != null) {
            val clamped = parsed.coerceIn(1.0, 100.0)
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
        minimum = 1.0,
        maximum = 100.0,
        onSubmit = { submit(it) },
        numericInput = numericInput,
        onNumericInputChange = onNumericInputChange,
        incrementImageName = "square.resize.up",
        decrementImageName = "square.resize.down",
        mirror = false,
        increment = 1.0 / 15.0,
    )
}
