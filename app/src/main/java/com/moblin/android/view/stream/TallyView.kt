package com.moblin.android.view.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.color
import com.moblin.android.various.model.*

@Composable
fun TallyView(tally: Tally) {
    val state by tally.state.collectAsState()
    state?.let { state ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 4.dp, color = state.background.color()),
            )
            if (state.message.isNotEmpty()) {
                Text(
                    text = state.message,
                    modifier = Modifier
                        .background(state.background.color())
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    color = state.foreground.color(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
