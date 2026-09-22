package com.moblin.android.view.controlbar.quickbutton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import kotlinx.coroutines.delay
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonStreamSwitcherView(model: Model = LocalModel.current, database: Database) {
    val currentStreamId by model.currentStreamId.collectAsState()
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(currentStreamId) {
        if (!isFirstComposition) {
            Unit
        }
        isFirstComposition = false
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Switch stream") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(database.streams) { stream ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            model.currentStreamId.value = stream.id
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = stream.id == currentStreamId,
                        onClick = {
                            model.currentStreamId.value = stream.id
                        }
                    )
                    Text(stream.name)
                }
            }
            item {
                Text(
                    "Automatically goes live when switching stream.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
