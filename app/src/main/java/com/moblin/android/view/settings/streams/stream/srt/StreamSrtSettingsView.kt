package com.moblin.android.view.settings.streams.stream.srt

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDnsLookupStrategy
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsStreamSrt
import com.moblin.android.various.settings.SettingsStreamSrtImplementation
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.reloadStreamIfEnabled

@Composable
fun StreamSrtSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    srt: SettingsStreamSrt,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val disabled = stream.enabled && isLive

    fun changeLatency(value: String): String? {
        val parsed = value.toIntOrNull() ?: return localized("Not a number")
        if (parsed < 0) {
            return localized("Too small")
        }
        if (parsed > 65535) {
            return localized("Too big")
        }
        return null
    }

    fun submitLatency(value: String) {
        val parsed = value.toIntOrNull() ?: return
        srt.latency = parsed
        model.reloadStreamIfEnabled(stream)
    }

    fun changeOverheadBandwidth(value: String): String? {
        val parsed = value.toIntOrNull() ?: return localized("Not a number")
        if (parsed < 5) {
            return localized("Too small")
        }
        if (parsed > 100) {
            return localized("Too big")
        }
        return null
    }

    fun submitOverheadBandwidth(value: String) {
        val parsed = value.toIntOrNull() ?: return
        srt.overheadBandwidth = parsed
        model.reloadStreamIfEnabled(stream)
    }

    Form(title = localized("SRT(LA)")) {
        Section(
            footerContent = {
                Text(
                    localized(
                        "Big packets means 7 MPEG-TS packets per SRT packet, 6 otherwise. " +
                            "Sometimes Android hotspots does not work with big packets.",
                    ),
                )
            },
        ) {
            TextEditNavigationView(
                title = localized("Latency"),
                value = srt.latency.toString(),
                onChange = { changeLatency(it) },
                onSubmit = { submitLatency(it) },
                valueFormat = { "$it ms" },
            )
            if (srt.implementation == SettingsStreamSrtImplementation.moblin && srt.latency < 1000) {
                Text(
                    localized(
                        "⚠️ The \"Moblin\" implementation does not perform well with low " +
                            "latency. Select the \"Official\" implementation at the bottom " +
                            "of this page.",
                    ),
                )
            }
            NavigationLink(
                destination = {
                    StreamSrtAdaptiveBitrateSettingsView(
                        model = model,
                        stream = stream,
                        srt = srt,
                        adaptiveBitrate = srt.adaptiveBitrate,
                    )
                },
            ) {
                Toggle(
                    title = localized("Adaptive bitrate"),
                    isOn = srt.adaptiveBitrateEnabled,
                    enabled = !disabled,
                    onChange = {
                        srt.adaptiveBitrateEnabled = it
                        model.reloadStreamIfEnabled(stream)
                    },
                )
            }
            NavigationLink(
                destination = {
                    StreamSrtConnectionPriorityView(stream = stream)
                },
            ) {
                Text(localized("Connection priorities"))
            }
            when (srt.implementation) {
                SettingsStreamSrtImplementation.official -> {
                    Toggle(
                        title = localized("Max bandwidth follows input"),
                        isOn = srt.maximumBandwidthFollowInput,
                        enabled = !disabled,
                        onChange = {
                            srt.maximumBandwidthFollowInput = it
                            model.reloadStreamIfEnabled(stream)
                        },
                    )
                    TextEditNavigationView(
                        title = localized("Overhead bandwidth"),
                        value = srt.overheadBandwidth.toString(),
                        onChange = { changeOverheadBandwidth(it) },
                        onSubmit = { submitOverheadBandwidth(it) },
                        valueFormat = { "$it%" },
                    )
                }
                SettingsStreamSrtImplementation.moblin -> Unit
            }
            Toggle(
                title = localized("Big packets"),
                isOn = srt.bigPackets,
                enabled = !disabled,
                onChange = {
                    srt.bigPackets = it
                    model.reloadStreamIfEnabled(stream)
                },
            )
        }
        Section(
            footerContent = {
                Text(
                    localized(
                        "System seems to work best for TMobile. IPv4 probably best for " +
                            "IRLToolkit.",
                    ),
                )
            },
        ) {
            Picker(
                title = localized("DNS lookup strategy"),
                selection = srt.dnsLookupStrategy,
                options = SettingsDnsLookupStrategy.entries,
                enabled = !disabled,
                text = { it.rawValue },
            ) {
                srt.dnsLookupStrategy = it
            }
        }
        Section(
            footerContent = {
                Text(
                    localized(
                        "\"Official\" uses the widely supported libSRT (version 1.5.3) and " +
                            "\"Moblin\" uses a more energy efficient custom implementation.",
                    ),
                )
            },
        ) {
            Picker(
                title = localized("Implementation"),
                selection = srt.implementation,
                options = SettingsStreamSrtImplementation.entries,
                enabled = !disabled,
                text = { it.toString() },
            ) {
                srt.implementation = it
                model.reloadStreamIfEnabled(stream)
            }
        }
    }
}
