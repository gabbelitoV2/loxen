package com.moblin.android.view.settings.importexport

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ShareLink
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.exportToFile
import com.moblin.android.view.utils.HCenter

@Composable
fun ExportSettingsView(model: Model = LocalModel.current) {
    var url by remember { mutableStateOf<String?>(null) }
    HCenter {
        val currentUrl = url
        if (currentUrl != null) {
            ShareLink(item = currentUrl) {
                Text(localized("Export"))
            }
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = formPalette().secondaryLabel,
                strokeWidth = 2.dp,
            )
        }
    }
    DisposableEffect(Unit) {
        model.exportToFile { fileUrl ->
            url = fileUrl
        }
        onDispose {}
    }
}
