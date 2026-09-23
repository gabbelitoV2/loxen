package com.moblin.android.view.settings.ingests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.Database
import com.moblin.android.view.settings.ingests.ristserver.RistServerSettingsView
import com.moblin.android.view.settings.ingests.rtmpserver.RtmpServerSettingsView
import com.moblin.android.view.settings.ingests.rtspclient.RtspClientSettingsView
import com.moblin.android.view.settings.ingests.srtclient.SrtClientSettingsView
import com.moblin.android.view.settings.ingests.srtlaserver.SrtlaServerSettingsView
import com.moblin.android.view.settings.ingests.whepclient.WhepClientSettingsView
import com.moblin.android.view.settings.ingests.whipserver.WhipServerSettingsView
import com.moblin.android.view.settings.wifiaware.WiFiAwareSettingsView

@Composable
fun IngestsSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = {},
) {
    val showWiFiAware = false
    val softwareVideoDecoding = binding(
        get = { database.ingestsSoftwareVideoDecoding },
        set = {
            database.ingestsSoftwareVideoDecoding = it
            model.reloadIngests()
        },
    )

    Form(title = "Ingests") {
        Section {
            RtmpServerSettingsView(rtmpServer = database.rtmpServer)
            SrtlaServerSettingsView(srtlaServer = database.srtlaServer)
            SrtClientSettingsView(srtClient = database.srtClient)
            RistServerSettingsView(ristServer = database.ristServer)
            RtspClientSettingsView(rtspClient = database.rtspClient)
            WhipServerSettingsView(whipServer = database.whipServer)
            WhepClientSettingsView(whepClient = database.whepClient)
            if (showWiFiAware) {
                NavigationLink(
                    destination = {
                        WiFiAwareSettingsView(model = model, wiFiAware = database.wiFiAware)
                    },
                ) {
                    Text(localized("WiFi Aware"))
                }
            }
        }
        Section(
            footerContent = {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        localized(
                            "Decode ingested video on the CPU instead of using the hardware video " +
                                "decoder. Uses more CPU, battery and generates more heat, but can " +
                                "decode video the hardware decoder does not support and allows " +
                                "decoding more streams at the same time.",
                        ),
                    )
                    Text("")
                    Text(localized("Only enable this if hardware video decoding does not work."))
                }
            },
        ) {
            Toggle("Software video decoding", isOn = softwareVideoDecoding)
        }
    }
}
