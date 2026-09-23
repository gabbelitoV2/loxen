package com.moblin.android.view.settings.streams.stream.srt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.formatBytesPerSecond
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamSrt
import com.moblin.android.various.settings.SettingsStreamSrtAdaptiveBitrate
import com.moblin.android.various.settings.SettingsStreamSrtAdaptiveBitrateAlgorithm
import com.moblin.android.view.utils.SliderView
import kotlin.math.pow

@Composable
fun StreamSrtAdaptiveBitrateSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    srt: SettingsStreamSrt,
    adaptiveBitrate: SettingsStreamSrtAdaptiveBitrate,
) {
    fun updateAdaptiveBitrate() {
        if (!stream.enabled) {
            return
        }
        model.updateAdaptiveBitrateSrt(srt)
    }

    fun submitFastIrlPacketsInFlight(value: Float) {
        adaptiveBitrate.fastIrlSettings.packetsInFlight = value.toInt()
        updateAdaptiveBitrate()
    }

    fun submitFastMinimumBitrate(value: Float) {
        adaptiveBitrate.fastIrlSettings.minimumBitrate = value / 1000f
        updateAdaptiveBitrate()
    }

    fun submitBitrateIncreaseSpeed(value: Float) {
        adaptiveBitrate.customSettings.pifDiffIncreaseFactor = value
        updateAdaptiveBitrate()
    }

    fun formatBitrateIncreaseSpeed(value: Float): String {
        return "${formatBytesPerSecond((value * 1000f).toLong())}/sec"
    }

    fun submitBitrateDecreaseSpeed(value: Float) {
        adaptiveBitrate.customSettings.rttDiffHighDecreaseFactor = (1f - (value / 100f)).pow(0.2f)
        updateAdaptiveBitrate()
    }

    fun formatBitrateDecreaseSpeed(value: Float): String {
        return "${value.toInt()} %/sec"
    }

    fun submitMinimumBitrateDecreaseSpeed(value: Float) {
        adaptiveBitrate.customSettings.rttDiffHighMinimumDecrease = value / 5f / 1000f
        updateAdaptiveBitrate()
    }

    fun formatMinimumBitrateDecreaseSpeed(value: Float): String {
        return "${formatBytesPerSecond(value.toLong())}/sec"
    }

    fun submitMinimumBitrate(value: Float) {
        adaptiveBitrate.customSettings.minimumBitrate = value / 1000f
        updateAdaptiveBitrate()
    }

    fun formatMinimumBitrate(value: Float): String {
        return formatBytesPerSecond(value.toLong())
    }

    fun submitPacketsInFlight(value: Float) {
        adaptiveBitrate.customSettings.packetsInFlight = value.toInt()
        updateAdaptiveBitrate()
    }

    fun formatPacketsInFlight(value: Float): String {
        return "${value.toInt()}"
    }

    fun submitAllowedRttSpike(value: Float) {
        adaptiveBitrate.customSettings.rttDiffHighAllowedSpike = value
        updateAdaptiveBitrate()
    }

    fun formatAllowedRttSpike(value: Float): String {
        return "${value.toInt()}"
    }

    fun submitBelaboxMinimumBitrate(value: Float) {
        adaptiveBitrate.belaboxSettings.minimumBitrate = value / 1000f
        updateAdaptiveBitrate()
    }

    Form(title = "Adaptive bitrate") {
        Section(
            footer = "BELABOX and Fast IRL are the safest options. Choose the others only if " +
                "you know what you are doing!",
        ) {
            Picker(
                title = "Algorithm",
                selection = adaptiveBitrate.algorithm,
                options = SettingsStreamSrtAdaptiveBitrateAlgorithm.entries,
                text = { it.toString() },
            ) { algorithm ->
                adaptiveBitrate.algorithm = algorithm
                if (stream.enabled && srt.adaptiveBitrateEnabled) {
                    model.setAdaptiveBitrateSrtAlgorithm(stream)
                }
                updateAdaptiveBitrate()
            }
        }
        if (adaptiveBitrate.algorithm == SettingsStreamSrtAdaptiveBitrateAlgorithm.fastIrl) {
            Section(
                header = "Packets in flight decrease threshold",
                footerContent = {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "The bitrate will decrease quickly when the number of packets " +
                                "in flight are above this value.",
                        )
                        Text("200 by default.")
                    }
                },
            ) {
                SliderView(
                    value = adaptiveBitrate.fastIrlSettings.packetsInFlight.toFloat(),
                    minimum = 200f,
                    maximum = 700f,
                    step = 10f,
                    onChange = {},
                    onSubmit = { submitFastIrlPacketsInFlight(it) },
                    width = 70f,
                    format = { formatPacketsInFlight(it) },
                )
            }
            Section(
                header = "Minimum bitrate",
                footerContent = {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("The minimum encoder bitrate.")
                        Text("250 Kbps by default.")
                    }
                },
            ) {
                SliderView(
                    value = 1000f * adaptiveBitrate.fastIrlSettings.minimumBitrate,
                    minimum = 50000f,
                    maximum = 2_000_000f,
                    step = 50000f,
                    onChange = {},
                    onSubmit = { submitFastMinimumBitrate(it) },
                    width = 80f,
                    format = { formatMinimumBitrate(it) },
                )
            }
        } else if (adaptiveBitrate.algorithm == SettingsStreamSrtAdaptiveBitrateAlgorithm.customIrl) {
            Section {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("⚠️")
                    Text("Finding good parameters is hard. You are on you own! =)")
                }
            }
            Section(header = "Bitrate increase speed") {
                SliderView(
                    value = adaptiveBitrate.customSettings.pifDiffIncreaseFactor,
                    minimum = 5f,
                    maximum = 500f,
                    step = 5f,
                    onChange = {},
                    onSubmit = { submitBitrateIncreaseSpeed(it) },
                    width = 120f,
                    format = { formatBitrateIncreaseSpeed(it) },
                )
            }
            Section(
                header = "Bitrate decrease speed",
                footer = "The bitrate decrease speed when RTT is too high.",
            ) {
                SliderView(
                    value = 100f *
                        (1f - adaptiveBitrate.customSettings.rttDiffHighDecreaseFactor.pow(5f)),
                    minimum = 10f,
                    maximum = 50f,
                    step = 1f,
                    onChange = {},
                    onSubmit = { submitBitrateDecreaseSpeed(it) },
                    width = 80f,
                    format = { formatBitrateDecreaseSpeed(it) },
                )
            }
            Section(
                header = "Minimum bitrate decrease speed",
                footer = "The minimum bitrate decrease speed when RTT is too high.",
            ) {
                SliderView(
                    value = 5f * 1000f * adaptiveBitrate.customSettings.rttDiffHighMinimumDecrease,
                    minimum = 25000f,
                    maximum = 2_000_000f,
                    step = 5000f,
                    onChange = {},
                    onSubmit = { submitMinimumBitrateDecreaseSpeed(it) },
                    width = 120f,
                    format = { formatMinimumBitrateDecreaseSpeed(it) },
                )
            }
            Section(
                header = "Packets in flight decrease threshold",
                footer = "The bitrate will decrease quickly when the number of packets " +
                    "in flight are above this value.",
            ) {
                SliderView(
                    value = adaptiveBitrate.customSettings.packetsInFlight.toFloat(),
                    minimum = 50f,
                    maximum = 5000f,
                    step = 5f,
                    onChange = {},
                    onSubmit = { submitPacketsInFlight(it) },
                    width = 100f,
                    format = { formatPacketsInFlight(it) },
                )
            }
            Section(
                header = "Allowed RTT spike",
                footer = "The maximum allowed RTT spike before decreasing the bitrate",
            ) {
                SliderView(
                    value = adaptiveBitrate.customSettings.rttDiffHighAllowedSpike,
                    minimum = 25f,
                    maximum = 1000f,
                    step = 5f,
                    onChange = {},
                    onSubmit = { submitAllowedRttSpike(it) },
                    width = 80f,
                    format = { formatAllowedRttSpike(it) },
                )
            }
            Section(
                header = "Minimum bitrate",
                footer = "The minimum encoder bitrate.",
            ) {
                SliderView(
                    value = 1000f * adaptiveBitrate.customSettings.minimumBitrate,
                    minimum = 50000f,
                    maximum = 2_000_000f,
                    step = 50000f,
                    onChange = {},
                    onSubmit = { submitMinimumBitrate(it) },
                    width = 80f,
                    format = { formatMinimumBitrate(it) },
                )
            }
        } else if (adaptiveBitrate.algorithm == SettingsStreamSrtAdaptiveBitrateAlgorithm.belabox) {
            Section(
                header = "Minimum bitrate",
                footerContent = {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("The minimum encoder bitrate.")
                        Text("250 Kbps by default.")
                    }
                },
            ) {
                SliderView(
                    value = 1000f * adaptiveBitrate.belaboxSettings.minimumBitrate,
                    minimum = 50000f,
                    maximum = 2_000_000f,
                    step = 50000f,
                    onChange = {},
                    onSubmit = { submitBelaboxMinimumBitrate(it) },
                    width = 80f,
                    format = { formatMinimumBitrate(it) },
                )
            }
        }
    }
}
