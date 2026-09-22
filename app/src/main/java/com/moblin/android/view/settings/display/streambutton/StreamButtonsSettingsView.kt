package com.moblin.android.view.settings.display.streambutton

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.moblin.android.localized
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.color
import com.moblin.android.various.settings.defaultStreamButtonColor
import com.moblin.android.view.utils.RgbColorPickerView
import com.moblin.android.view.utils.TextButtonView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamButtonsSettingsView(database: Database) {
    val streamButtonColorColor by database.streamButtonColorColor.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Stream button")) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = localized("Color"),
                style = MaterialTheme.typography.titleMedium,
            )
            RgbColorPickerView(
                title = localized("Background"),
                color = streamButtonColorColor,
                onChange = { color ->
                    database.streamButtonColor.value = color
                },
            )
            TextButtonView(
                text = localized("Reset"),
                onClick = {
                    database.streamButtonColor.value = defaultStreamButtonColor
                    database.streamButtonColorColor.value = database.streamButtonColor.value.color()
                },
            )
        }
    }
}
