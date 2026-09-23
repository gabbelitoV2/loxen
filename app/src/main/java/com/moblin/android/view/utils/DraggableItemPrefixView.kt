package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.platform.SystemImage

@Composable
fun DraggableItemPrefixView(modifier: Modifier = Modifier) {
    SystemImage(name = "line.3.horizontal", fontSize = 17.sp, modifier = modifier)
}

@Composable
fun DraggableItemTextView(
    name: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DraggableItemPrefixView()
        Text(text = name)
        Spacer(modifier = Modifier.weight(1f))
    }
}
