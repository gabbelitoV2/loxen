package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.platform.swiftui.rememberDismiss

@Composable
fun TextEditView(
    title: String,
    value: String,
    footers: List<String> = emptyList(),
    capitalize: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    onChange: ((String) -> String?)? = null,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit = rememberDismiss(),
) {
    var stateValue by remember { mutableStateOf(value) }
    TextEditBindingView(
        title = title,
        value = stateValue,
        onValueChange = { stateValue = it },
        footers = footers,
        capitalize = capitalize,
        keyboardType = keyboardType,
        placeholder = placeholder,
        onChange = onChange,
        onSubmit = onSubmit,
        onDismiss = onDismiss,
    )
}

@Composable
fun TextEditBindingView(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    footers: List<String> = emptyList(),
    capitalize: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    onChange: ((String) -> String?)? = null,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit = rememberDismiss(),
) {
    val palette = formPalette()

    var changed by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var initialized by remember { mutableStateOf(false) }

    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnSubmit by rememberUpdatedState(onSubmit)

    fun submit() {
        if (errorMessage != null) {
            return
        }
        submitted = true
        val trimmed = currentValue.trim()
        currentOnValueChange(trimmed)
        currentOnSubmit(trimmed)
    }

    LaunchedEffect(value) {
        if (!initialized) {
            initialized = true
            return@LaunchedEffect
        }
        changed = true
        errorMessage = onChange?.invoke(value.trim())
    }

    DisposableEffect(Unit) {
        onDispose {
            if (changed && !submitted) {
                submit()
            }
        }
    }

    Form(title = title) {
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    errorMessage?.let { message ->
                        Text(message, color = palette.red, fontWeight = FontWeight.Bold)
                        Text("")
                    }
                    footers.forEach { footer ->
                        Text(footer)
                    }
                }
            },
        ) {
            FormRow {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue -> currentOnValueChange(newValue) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = formBodyStyle.copy(color = palette.label),
                    singleLine = true,
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        capitalization = if (capitalize) {
                            KeyboardCapitalization.Sentences
                        } else {
                            KeyboardCapitalization.None
                        },
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        submit()
                        onDismiss()
                    }),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = formBodyStyle,
                                    color = palette.tertiaryLabel,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        }
    }
}
