package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette

@Composable
private fun TextEditNavigationViewInner(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onChange: ((String) -> String?)?,
    footers: List<String>,
    capitalize: Boolean,
    keyboardType: KeyboardType,
    placeholder: String,
    errorMessage: String?,
    onErrorMessageChange: (String?) -> Unit,
    submittedValue: String,
    onSubmittedValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val submitted = remember { mutableStateOf(false) }
    val palette = formPalette()

    val submit: () -> Unit = {
        if (!submitted.value) {
            if (errorMessage == null) {
                val trimmed = value.trim()
                onValueChange(trimmed)
                onSubmit(trimmed)
                onSubmittedValueChange(trimmed)
            } else {
                onErrorMessageChange(null)
                onValueChange(submittedValue)
            }
            submitted.value = true
        }
    }
    val currentSubmit = rememberUpdatedState(submit)

    val previousValue = remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (previousValue.value != value) {
            previousValue.value = value
            onErrorMessageChange(onChange?.invoke(value.trim()))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentSubmit.value()
        }
    }

    Form(title = title, modifier = modifier) {
        Section(
            footerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = formFootnoteStyle,
                            fontWeight = FontWeight.Bold,
                            color = palette.red,
                        )
                        Text(text = "", style = formFootnoteStyle)
                    }
                    footers.forEach { footer ->
                        Text(text = footer, style = formFootnoteStyle)
                    }
                }
            },
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = formBodyStyle.copy(color = palette.label),
                keyboardOptions = KeyboardOptions(
                    capitalization = if (capitalize) {
                        KeyboardCapitalization.Sentences
                    } else {
                        KeyboardCapitalization.None
                    },
                    autoCorrectEnabled = false,
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        submit()
                        onDismiss()
                    },
                ),
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = formBodyStyle,
                            color = palette.secondaryLabel,
                        )
                    }
                    innerTextField()
                },
            )
        }
    }
}

@Composable
fun TextEditNavigationView(
    title: String,
    value: String,
    onChange: (String) -> String? = { null },
    onSubmit: (String) -> Unit,
    footers: List<String> = emptyList(),
    capitalize: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
    sensitive: Boolean = false,
    valueFormat: ((String) -> String)? = null,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val currentValue = remember { mutableStateOf(value) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val submittedValue = remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        currentValue.value = value
        submittedValue.value = value
    }

    NavigationLink(
        destination = {
            TextEditNavigationViewInner(
                title = title,
                value = currentValue.value,
                onValueChange = { currentValue.value = it },
                onSubmit = onSubmit,
                onChange = onChange,
                footers = footers,
                capitalize = capitalize,
                keyboardType = keyboardType,
                placeholder = placeholder,
                errorMessage = errorMessage.value,
                onErrorMessageChange = { errorMessage.value = it },
                submittedValue = submittedValue.value,
                onSubmittedValueChange = { submittedValue.value = it },
                onDismiss = {},
            )
        },
    ) {
        TextItemView(
            name = title,
            value = valueFormat?.invoke(submittedValue.value) ?: submittedValue.value,
            sensitive = sensitive,
        )
    }
}
