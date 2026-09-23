package com.moblin.android.view.settings.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.various.model.Model

@Composable
fun ImportExportSettingsView(model: Model = LocalModel.current) {
    Form(title = "Import and export settings") {
        ImportSettingsView(model = model)
        Section(
            footerContent = {
                CompositionLocalProvider(LocalTextStyle provides formFootnoteStyle) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                                "It is not recommended to export settings from one device and import " +
                                    "them in another. Some settings will not work on other devices. Deep " +
                                    "links, on the other hand, can be imported on any device.",
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
            },
        ) {
            ExportSettingsView(model = model)
        }
    }
}
