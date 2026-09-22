package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

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
    onDismiss: () -> Unit = {},
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

@OptIn(ExperimentalMaterial3Api::class)
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
    onDismiss: () -> Unit = {},
) {
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title) })
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                OutlinedTextField(
                    value = value,
                    onValueChange = { newValue -> currentOnValueChange(newValue) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(placeholder) },
                    singleLine = true,
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
                )
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    errorMessage?.let { message ->
                        Text(message, fontWeight = FontWeight.Bold, color = Color.Red)
                        Text("")
                    }
                }
            }
            items(footers) { footer ->
                Text(footer)
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                if (changed && !submitted) {
                    submit()
                }
            }
        }
    }
}
