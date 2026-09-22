package com.moblin.android.view.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Snapshot

@Composable
fun SnapshotCountdownView(snapshot: Snapshot) {
    val snapshotJob by snapshot.currentJob.collectAsState()
    val countdown by snapshot.countdown.collectAsState()
    val job = snapshotJob ?: return
    if (countdown <= 0) {
        return
    }
    Column(
        modifier = Modifier
            .widthIn(max = 300.dp)
            .padding(10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.75f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Taking snapshot in",
            color = Color.White
        )
        Text(
            text = countdown.toString(),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = job.message,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
