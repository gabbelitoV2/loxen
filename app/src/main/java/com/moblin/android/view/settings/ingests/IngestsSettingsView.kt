package com.moblin.android.view.settings.ingests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.localized
import com.moblin.android.platform.coregraphics.CGPoint
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
import com.moblin.android.view.utils.TradingCardView
import com.moblin.android.view.utils.strokeTradingCardDash

private fun drawIngestsCable(
    context: DrawScope,
    y: Double,
    endOffset: Double,
    endX: Double,
    time: Double,
) {
    val path = Path()
    path.moveTo(34.0f, y.toFloat())
    path.cubicTo(
        80.0f, y.toFloat(),
        95.0f, (112.0 + endOffset).toFloat(),
        endX.toFloat(), (112.0 + endOffset).toFloat(),
    )
    strokeTradingCardDash(
        context = context,
        path = path,
        color = Color.White.copy(alpha = 0.7f),
        lineWidth = 1.5f,
        dash = 6f,
        gap = 10f,
        time = time,
    )
}

private fun drawIngestsCard(context: DrawScope, time: Double) {
    drawIngestsCable(context = context, y = 24.0, endOffset = -8.0, endX = 150.0, time = time)
    drawIngestsCable(context = context, y = 54.0, endOffset = -2.0, endX = 150.0, time = time)
    drawIngestsCable(context = context, y = 84.0, endOffset = 4.0, endX = 150.0, time = time)
    drawIngestsCable(context = context, y = 114.0, endOffset = 10.0, endX = 155.0, time = time)
}

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
            TradingCardView(
                name = "Ingests",
                background = Color(
                    red = 0xE4 / 255f,
                    green = 0xE1 / 255f,
                    blue = 0xD3 / 255f,
                ),
                draw = ::drawIngestsCard,
            )
        }
        Section {
            RtmpServerSettingsView(rtmpServer = database.rtmpServer)
            SrtlaServerSettingsView(srtlaServer = database.srtlaServer)
            SrtClientSettingsView(srtClient = database.srtClient)
            RistServerSettingsView(model = model, ristServer = database.ristServer)
            RtspClientSettingsView(model = model, rtspClient = database.rtspClient)
            WhipServerSettingsView(whipServer = database.whipServer)
            WhepClientSettingsView(model = model, whepClient = database.whepClient)
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
