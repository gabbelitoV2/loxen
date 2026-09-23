package com.moblin.android.view.settings.importexport

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.moblin.android.LocalModel
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.view.utils.HCenter
import java.io.File
import com.moblin.android.localized
import com.moblin.android.various.model.exportToFile

@Composable
fun ExportSettingsView(model: Model = LocalModel.current) {
    var url by remember { mutableStateOf<String?>(null) }

    HCenter {
        val exportUrl = url
        if (exportUrl != null) {
            val context = LocalContext.current
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            Text(
                localized("Export"),
                style = formBodyStyle,
                color = formPalette().accent,
                modifier = Modifier
                    .alpha(if (pressed) 0.2f else 1f)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        val uri = FileProvider.getUriForFile(
                            context,
                            context.packageName + ".fileprovider",
                            File(exportUrl),
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
            )
        } else {
            CircularProgressIndicator()
        }
    }

    LaunchedEffect(Unit) {
        model.exportToFile { path ->
            url = path
        }
    }
}
