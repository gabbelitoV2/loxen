package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.PickerStyle
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.rememberDismiss

data class InlinePickerItem(
    val id: String,
    val text: String,
) {
    companion object {
        fun fromStrings(values: List<String>): List<InlinePickerItem> {
            return values.map { InlinePickerItem(id = it, text = it) }
        }
    }
}

@Composable
fun InlinePickerView(
    title: String,
    onChange: (String) -> Unit,
    footers: List<String> = emptyList(),
    items: List<InlinePickerItem>,
    initialSelectedId: String,
    onDismiss: () -> Unit = rememberDismiss(),
) {
    var selectedId by remember(initialSelectedId) { mutableStateOf(initialSelectedId) }

    val options = if (items.any { it.id == selectedId }) {
        items
    } else {
        items + InlinePickerItem(id = selectedId, text = localized("Unknown 😢"))
    }

    val selectedItem = options.firstOrNull { it.id == selectedId }
        ?: InlinePickerItem(id = selectedId, text = selectedId)

    val footerContent: (@Composable () -> Unit)? = if (footers.isEmpty()) {
        null
    } else {
        {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                footers.forEach { footer ->
                    Text(text = footer, style = formFootnoteStyle)
                }
            }
        }
    }

    Form(title = localized(title)) {
        Section(footerContent = footerContent) {
            Picker(
                title = "",
                selection = selectedItem,
                options = options,
                text = { it.text },
                pickerStyle = PickerStyle.inline,
                onChange = { item ->
                    selectedId = item.id
                    onChange(item.id)
                    onDismiss()
                },
            )
        }
    }
}
