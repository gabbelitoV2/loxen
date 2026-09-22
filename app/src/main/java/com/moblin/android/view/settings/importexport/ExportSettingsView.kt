package com.moblin.android.view.settings.importexport

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.moblin.android.various.model.Model
import com.moblin.android.view.utils.HCenter

@Composable
fun ExportSettingsView(model: Model) {
    var url by remember { mutableStateOf<String?>(null) }

    HCenter {
        val exportUrl = url
        if (exportUrl != null) {
            TODO("no Android counterpart for ShareLink")
        } else {
            CircularProgressIndicator()
        }
    }

    LaunchedEffect(Unit) {
        model.exportToFile {
            url = it
        }
    }
}
