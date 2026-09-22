package com.moblin.android.view.settings.debug

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.moblin.android.common.various.formatOneDecimal
import com.moblin.android.common.various.formatTwoDecimals
import com.moblin.android.localized
import com.moblin.android.various.model.LogEntry
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.view.settings.recordings.FilesLocationView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import kotlin.math.roundToInt
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsView(
    model: Model = LocalModel.current,
    debug: SettingsDebug,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var presentingLog by remember { mutableStateOf(false) }
    val log = remember { mutableStateOf(ArrayDeque<LogEntry>()) }

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
        debug.setMaximumLogLines(lines)
    }

    fun reloadLog() {
        log.value = model.log.value
    }

    val maximumLogLines by debug.maximumLogLines.collectAsState()
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
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Debug") })
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                item {
                    TextButtonView(text = "Log", onClick = { presentingLog = true })
                }
                item {
                    FilesLocationView(
                        model = model,
                        text = "Logs directory",
                        path = model.logsStorage.storageDirectory(),
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Debug logging", modifier = Modifier.weight(1f))
                        Switch(
                            checked = debugLogging,
                            onCheckedChange = {
                                debug.setDebugLogging(it)
                                model.setDebugLogging(on = it)
                            },
                        )
                    }
                }
                item {
                    TextEditNavigationView(
                        title = "Maximum log lines",
                        value = maximumLogLines.toString(),
                        onChange = { changeLogLines(it) },
                        onSubmit = { submitLogLines(it) },
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Debug overlay", modifier = Modifier.weight(1f))
                        Switch(
                            checked = debugOverlay,
                            onCheckedChange = {
                                debug.setDebugOverlay(it)
                                model.updateDebugOverlay()
                            },
                        )
                    }
                }
                item {
                    Text("Experimental", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    TextButtonView(
                        text = "Video",
                        onClick = { onNavigate("DebugVideoSettingsView") },
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Bitrate drop fix", modifier = Modifier.weight(1f))
                        Switch(
                            checked = bitrateDropFix,
                            onCheckedChange = {
                                debug.setBitrateDropFix(it)
                                model.setBitrateDropFix()
                            },
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Data rate limit")
                        Slider(
                            value = dataRateLimitFactor.toFloat(),
                            onValueChange = { debug.setDataRateLimitFactor(it.toDouble()) },
                            modifier = Modifier.weight(1f),
                            valueRange = 1.2f..2.5f,
                            steps = ((2.5 - 1.2) / 0.1).roundToInt() - 1,
                            onValueChangeFinished = { model.setBitrateDropFix() },
                        )
                        Text(
                            formatOneDecimal(dataRateLimitFactor),
                            modifier = Modifier.width(40.dp),
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Relaxed bitrate decrement after scene switch",
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = relaxedBitrate,
                            onCheckedChange = { debug.setRelaxedBitrate(it) },
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Twitch rewards", modifier = Modifier.weight(1f))
                        Switch(
                            checked = twitchRewards,
                            onCheckedChange = { debug.setTwitchRewards(it) },
                        )
                    }
                }
                item {
                    Column {
                        Text("Builtin audio and video delay")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = builtinAudioAndVideoDelay.toFloat(),
                                onValueChange = {
                                    debug.setBuiltinAudioAndVideoDelay(it.toDouble())
                                },
                                modifier = Modifier.weight(1f),
                                valueRange = 0.0f..4.0f,
                                steps = ((4.0 - 0.0) / 0.01).roundToInt() - 1,
                            )
                            Text(
                                formatTwoDecimals(builtinAudioAndVideoDelay),
                                modifier = Modifier.width(40.dp),
                            )
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Enhanced Moblin SRT", modifier = Modifier.weight(1f))
                        Switch(
                            checked = enhancedMoblinSrt,
                            onCheckedChange = { debug.setEnhancedMoblinSrt(it) },
                        )
                    }
                }
                item {
                    TextButtonView(
                        text = "HTTP proxy",
                        onClick = { onNavigate("HttpProxySettingsView") },
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SRT(LA) packet padding", modifier = Modifier.weight(1f))
                        Switch(
                            checked = packetPadding,
                            onCheckedChange = { debug.setPacketPadding(it) },
                        )
                    }
                }
                item {
                    Text("Camera man", style = MaterialTheme.typography.titleSmall)
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Vertical movement", modifier = Modifier.weight(1f))
                        Switch(
                            checked = cameraManMoveVertically,
                            onCheckedChange = {
                                debug.setCameraManMoveVertically(it)
                                model.cameraManEffect.setSettings(
                                    moveVertically = it,
                                    speed = cameraManSpeed,
                                    alwaysMove = cameraManAlwaysMove,
                                )
                            },
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Always move", modifier = Modifier.weight(1f))
                        Switch(
                            checked = cameraManAlwaysMove,
                            onCheckedChange = {
                                debug.setCameraManAlwaysMove(it)
                                model.cameraManEffect.setSettings(
                                    moveVertically = cameraManMoveVertically,
                                    speed = cameraManSpeed,
                                    alwaysMove = it,
                                )
                            },
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Speed")
                        Slider(
                            value = cameraManSpeed.toFloat(),
                            onValueChange = { debug.setCameraManSpeed(it.toDouble()) },
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
