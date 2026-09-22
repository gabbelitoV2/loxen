package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.various.utils.isPhone

@Composable
fun MultiLineTextFieldView(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopEnd,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 30.dp)
                .focusRequester(focusRequester),
        )
        if (value.isNotEmpty()) {
            IconButton(
                onClick = {
                    onValueChange("")
                    runCatching { focusRequester.requestFocus() }
                },
                modifier = Modifier.padding(top = 1.dp, end = 5.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
fun MultiLineTextFieldNavigationView(
    title: String,
    placeholder: String,
    value: String,
    onSubmit: (String) -> Unit,
    footers: List<String> = emptyList(),
    color: Color = Color.Gray,
    onValueChange: (String) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Box(
        modifier = Modifier.clickable {
            onNavigate("MultiLineTextFieldBindingView")
        },
    ) {
        TextItemView(name = title, value = value, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MultiLineTextFieldBindingView(
    title: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    footers: List<String> = emptyList(),
    onBack: () -> Unit = {},
) {
    var changed by remember { mutableStateOf(false) }
    val latestValue by rememberUpdatedState(value)
    val latestChanged by rememberUpdatedState(changed)

    DisposableEffect(Unit) {
        onDispose {
            if (latestChanged) {
                val trimmed = latestValue.trim()
                onValueChange(trimmed)
                onSubmit(trimmed)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item {
                MultiLineTextFieldView(
                    value = value,
                    placeholder = placeholder,
                    onValueChange = {
                        onValueChange(it)
                        changed = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(footers) { footer ->
                Text(
                    text = footer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
fun MultiLineTextFieldDoneButtonView(
    editingText: Boolean,
    onEditingTextChange: (Boolean) -> Unit,
) {
    if (isPhone()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = { onEditingTextChange(false) },
                enabled = editingText,
            ) {
                Text("Done")
            }
        }
    }
}
