package com.moblin.android.view.settings.debug

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.formatTwoDecimals
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.*
import com.moblin.android.various.model.LogEntry
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.clearLog
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.view.settings.httpproxy.HttpProxySettingsView
import com.moblin.android.view.settings.recordings.FilesLocationView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.updateDebugOverlay

@Composable
fun DebugSettingsView(
    model: Model = LocalModel.current,
    debug: SettingsDebug,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingLog by remember { mutableStateOf(false) }
    val log = remember { mutableStateOf<List<LogEntry>>(emptyList()) }

    fun changeLogLines(value: String): String? {
        val lines = value.toIntOrNull() ?: return localized("Not a number")
        if (lines < 1) {
            return localized("Too small")
        }
        if (lines > 100_000) {
            return localized("Too big")
        }
        return null
    }

    fun submitLogLines(value: String) {
        val lines = value.toIntOrNull() ?: return
        debug.maximumLogLines = lines
    }

    fun reloadLog() {
        log.value = model.log
    }

    val maximumLogLines = debug.maximumLogLines
    val debugLogging by debug.debugLogging.collectAsState()
    val debugOverlay by debug.debugOverlay.collectAsState()
    val bitrateDropFix by debug.bitrateDropFix.collectAsState()
    val dataRateLimitFactor by debug.dataRateLimitFactor.collectAsState()
    val relaxedBitrate by debug.relaxedBitrate.collectAsState()
    val twitchRewards by debug.twitchRewards.collectAsState()
    val builtinAudioAndVideoDelay by debug.builtinAudioAndVideoDelay.collectAsState()
    val enhancedMoblinSrt by debug.enhancedMoblinSrt.collectAsState()
    val packetPadding by debug.packetPadding.collectAsState()
    val cameraManMoveVertically by debug.cameraManMoveVertically.collectAsState()
    val cameraManAlwaysMove by debug.cameraManAlwaysMove.collectAsState()
    val cameraManSpeed by debug.cameraManSpeed.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Form(title = "Debug") {
            Section {
                TextButtonView(title = "Log", action = { presentingLog = true })
            }
            FilesLocationView(
                model = model,
                text = localized("Logs directory"),
                path = model.logsStorage.storageDirectory().toURI(),
            )
            Section {
                Toggle(
                    "Debug logging",
                    isOn = debugLogging,
                    onChange = {
                        debug.debugLogging.value = it
                        model.setDebugLogging(on = it)
                    },
                )
                TextEditNavigationView(
                    title = "Maximum log lines",
                    value = maximumLogLines.toString(),
                    onChange = { changeLogLines(it) },
                    onSubmit = { submitLogLines(it) },
                )
                Toggle(
                    "Debug overlay",
                    isOn = debugOverlay,
                    onChange = {
                        debug.debugOverlay.value = it
                        model.updateDebugOverlay()
                    },
                )
            }
            Section(header = "Experimental") {
                NavigationLink(destination = {
                    DebugVideoSettingsView(model = model, debug = debug)
                }) {
                    Text(localized("Video"))
                }
                Toggle(
                    "Bitrate drop fix",
                    isOn = bitrateDropFix,
                    onChange = {
                        debug.bitrateDropFix.value = it
                        model.setBitrateDropFix()
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Data rate limit"))
                    FormSlider(
                        value = dataRateLimitFactor.toFloat(),
                        onValueChange = { debug.dataRateLimitFactor.value = it },
                        modifier = Modifier.weight(1f),
                        valueRange = 1.2f..2.5f,
                        onValueChangeFinished = { model.setBitrateDropFix() },
                    )
                    Text(
                        formatOneDecimal(dataRateLimitFactor),
                        modifier = Modifier.width(40.dp),
                    )
                }
                Toggle(
                    "Relaxed bitrate decrement after scene switch",
                    isOn = relaxedBitrate,
                    onChange = { debug.relaxedBitrate.value = it },
                )
                Toggle(
                    "Twitch rewards",
                    isOn = twitchRewards,
                    onChange = { debug.twitchRewards.value = it },
                )
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("Builtin audio and video delay"))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FormSlider(
                            value = builtinAudioAndVideoDelay.toFloat(),
                            onValueChange = {
                                debug.builtinAudioAndVideoDelay.value = it.toDouble()
                            },
                            modifier = Modifier.weight(1f),
                            valueRange = 0.0f..4.0f,
                        )
                        Text(
                            formatTwoDecimals(builtinAudioAndVideoDelay),
                            modifier = Modifier.width(40.dp),
                        )
                    }
                }
                Toggle(
                    "Enhanced Moblin SRT",
                    isOn = enhancedMoblinSrt,
                    onChange = { debug.enhancedMoblinSrt.value = it },
                )
                NavigationLink(destination = {
                    HttpProxySettingsView(
                        model = model,
                        status = model.statusOther,
                        httpProxy = model.database.httpProxy,
                    )
                }) {
                    Text(localized("HTTP proxy"))
                }
                Toggle(
                    "SRT(LA) packet padding",
                    isOn = packetPadding,
                    onChange = { debug.packetPadding.value = it },
                )
            }
            Section(header = "Camera man") {
                Toggle(
                    "Vertical movement",
                    isOn = cameraManMoveVertically,
                    onChange = {
                        debug.cameraManMoveVertically.value = it
                        model.cameraManEffect.setSettings(
                            moveVertically = it,
                            speed = cameraManSpeed,
                            alwaysMove = cameraManAlwaysMove,
                        )
                    },
                )
                Toggle(
                    "Always move",
                    isOn = cameraManAlwaysMove,
                    onChange = {
                        debug.cameraManAlwaysMove.value = it
                        model.cameraManEffect.setSettings(
                            moveVertically = cameraManMoveVertically,
                            speed = cameraManSpeed,
                            alwaysMove = it,
                        )
                    },
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Speed"))
                    FormSlider(
                        value = cameraManSpeed.toFloat(),
                        onValueChange = { debug.cameraManSpeed.value = it.toDouble() },
                        modifier = Modifier.weight(1f),
                        valueRange = 0.2f..4.0f,
                        onValueChangeFinished = {
                            model.cameraManEffect.setSettings(
                                moveVertically = cameraManMoveVertically,
                                speed = cameraManSpeed,
                                alwaysMove = cameraManAlwaysMove,
                            )
                        },
                    )
                }
            }
        }
        if (presentingLog) {
            DebugLogSettingsView(
                model = model,
                debug = debug,
                log = log.value,
                onLogChange = { log.value = it },
                presentingLog = presentingLog,
                onPresentingLogChange = { presentingLog = it },
                reloadLog = { reloadLog() },
                clearLog = { model.clearLog() },
            )
            LaunchedEffect(Unit) {
                reloadLog()
            }
        }
    }
}
