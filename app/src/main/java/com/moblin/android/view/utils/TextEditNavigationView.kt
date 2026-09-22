package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
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

    fun submit() {
        if (submitted.value) {
            return
        }
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

    LaunchedEffect(value) {
        onErrorMessageChange(onChange?.invoke(value.trim()))
    }

    DisposableEffect(Unit) {
        onDispose {
            submit()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(title = { Text(title) })
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(placeholder) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                        )
                        Text(text = "")
                    }
                    footers.forEach { footer ->
                        Text(text = footer)
                    }
                }
            }
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

    Box(
        modifier = Modifier.clickable {
            onNavigate("TextEditNavigationViewInner")
        },
    ) {
        TextItemView(
            name = title,
            value = valueFormat?.invoke(submittedValue.value) ?: submittedValue.value,
            sensitive = sensitive,
        )
    }
}
