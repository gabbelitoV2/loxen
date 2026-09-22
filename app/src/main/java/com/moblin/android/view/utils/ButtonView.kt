package com.moblin.android.view.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.moblin.android.various.utils.openUrl

@Composable
fun TextButtonView(title: String, action: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { action() }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("")
                Spacer(Modifier.weight(1f))
                Text(title)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ExternalButtonView(action: () -> Unit, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { action() }) {
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun ExternalUrlButtonView(url: String, content: @Composable () -> Unit) {
    ExternalButtonView(
        action = { openUrl(url) },
        content = { content() }
    )
}

private fun Int.dp() = androidx.compose.ui.unit.Dp(this.toFloat())
