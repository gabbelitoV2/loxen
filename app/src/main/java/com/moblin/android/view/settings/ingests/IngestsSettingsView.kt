package com.moblin.android.view.settings.ingests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
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
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngestsSettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = {},
) {
    val softwareVideoDecoding by database.ingestsSoftwareVideoDecoding.collectAsState()
    val showWiFiAware = false

    LaunchedEffect(softwareVideoDecoding) {
        model.reloadIngests()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Ingests")) })
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            item {
                RtmpServerSettingsView(rtmpServer = database.rtmpServer)
            }
            item {
                SrtlaServerSettingsView(srtlaServer = database.srtlaServer)
            }
            item {
                SrtClientSettingsView(srtClient = database.srtClient)
            }
            item {
                RistServerSettingsView(ristServer = database.ristServer)
            }
            item {
                RtspClientSettingsView(rtspClient = database.rtspClient)
            }
            item {
                WhipServerSettingsView(whipServer = database.whipServer)
            }
            item {
                WhepClientSettingsView(whepClient = database.whepClient)
            }
            if (showWiFiAware) {
                item {
                    Text(
                        text = "WiFi Aware",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("WiFiAware") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                item {
                    WiFiAwareSettingsView(
                        model = model,
                        wiFiAware = database.wiFiAware,
                        onNavigate = onNavigate,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Software video decoding",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = softwareVideoDecoding,
                        onCheckedChange = { value ->
                            database.ingestsSoftwareVideoDecoding.value = value
                        },
                    )
                }
            }
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Decode ingested video on the CPU instead of using the hardware video decoder. " +
                            "Uses more CPU, battery and generates more heat, but can decode video the hardware " +
                            "decoder does not support and allows decoding more streams at the same time.",
                    )
                    Text("")
                    Text("Only enable this if hardware video decoding does not work.")
                }
            }
        }
    }
}
