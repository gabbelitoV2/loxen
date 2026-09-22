package com.moblin.android.view.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlinePickerView(
    title: String,
    onChange: (String) -> Unit,
    footers: List<String> = emptyList(),
    items: List<InlinePickerItem>,
    initialSelectedId: String,
    onDismiss: () -> Unit = {},
) {
    var selectedId by remember(initialSelectedId) { mutableStateOf(initialSelectedId) }
    val isKnownSelection = items.any { it.id == selectedId }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized(title)) })
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            items(items, key = { it.id }) { item ->
                val isSelected = item.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSelected) {
                            selectedId = item.id
                            onChange(item.id)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = isSelected, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (!isKnownSelection) {
                item(key = "unknown") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = true, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = localized("Unknown 😢"),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            if (footers.isNotEmpty()) {
                item(key = "footers") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        footers.forEach { footer ->
                            Text(
                                text = footer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
