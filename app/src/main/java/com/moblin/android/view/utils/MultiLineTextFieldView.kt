package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalOnNavigate
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.utils.isPhone

@Composable
fun MultiLineTextFieldView(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    val palette = formPalette()
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 30.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = formBodyStyle.copy(color = palette.label),
                cursorBrush = SolidColor(palette.accent),
            )
            if (value.isEmpty()) {
                Text(text = placeholder, style = formBodyStyle, color = palette.tertiaryLabel)
            }
        }
        if (value.isNotEmpty()) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .padding(top = 1.dp, end = 5.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            onValueChange("")
                            runCatching { focusRequester.requestFocus() }
                        },
                    ),
            ) {
                SystemImage(
                    name = "xmark.circle.fill",
                    fontSize = 17.sp,
                    tint = palette.gray.copy(alpha = if (pressed) 0.25f else 0.5f),
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
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            MultiLineTextFieldBindingView(
                title = title,
                placeholder = placeholder,
                value = value,
                onValueChange = onValueChange,
                onSubmit = onSubmit,
                footers = footers,
            )
        },
    ) {
        TextItemView(name = title, value = value, color = color)
    }
}

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
    val palette = formPalette()

    DisposableEffect(Unit) {
        onDispose {
            if (latestChanged) {
                val trimmed = latestValue.trim()
                onValueChange(trimmed)
                onSubmit(trimmed)
            }
        }
    }

    Form(title = title) {
        Section(
            footerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    footers.forEach { footer ->
                        Text(text = footer, style = formFootnoteStyle, color = palette.secondaryLabel)
                    }
                }
            },
        ) {
            MultiLineTextFieldView(
                value = value,
                placeholder = placeholder,
                onValueChange = {
                    onValueChange(it)
                    changed = true
                },
            )
        }
    }
}

@Composable
fun MultiLineTextFieldDoneButtonView(
    editingText: Boolean,
    onEditingTextChange: (Boolean) -> Unit,
) {
    if (isPhone()) {
        val palette = formPalette()
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = "Done",
                style = formBodyStyle,
                color = (if (editingText) palette.accent else palette.gray)
                    .copy(alpha = if (pressed && editingText) 0.2f else 1f),
                modifier = Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = editingText,
                    onClick = { onEditingTextChange(false) },
                ),
            )
        }
    }
}
