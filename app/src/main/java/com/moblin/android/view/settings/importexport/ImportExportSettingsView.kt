package com.moblin.android.view.settings.importexport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.various.model.Model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportSettingsView(model: Model) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(localized("Import and export settings"))
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            ImportSettingsView(model = model)
            ExportSettingsView(model = model)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(localized(""))
                Text(
                    localized(
                        "Do not share your settings with anyone as they may contain " +
                            "sensitive data (stream keys, etc.)!",
                    ),
                    fontWeight = FontWeight.Bold,
                )
                Text(localized(""))
                Text(
                    localized(
                        "It is not recommended to export settings from one device and import them " +
                            "in another. Some settings will not work on other devices. Deep links, on " +
                            "the other hand, can be imported on any device.",
                    ),
                )
                Text(localized(""))
                Text(
                    localized(
                        "moblin:// deep links can be used to import some settings, often " +
                            "using QR codes or a browser. See https://github.com/eerimoq/moblin " +
                            "for details.",
                    ),
                )
            }
        }
    }
}
