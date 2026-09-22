package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement

@Composable
fun DraggableItemPrefixView(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Menu,
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun DraggableItemTextView(
    name: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        DraggableItemPrefixView()
        Text(text = name)
        Spacer(modifier = Modifier.weight(1f))
    }
}
