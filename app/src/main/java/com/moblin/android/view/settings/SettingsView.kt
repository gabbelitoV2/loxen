package com.moblin.android.view.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.moblinwatch.shared.WatchSettings
import com.moblin.android.platform.SystemImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.appModeChanged
import com.moblin.android.various.model.sendInitToWatch
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsAppMode
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.isPad
import com.moblin.android.various.utils.isPhone
import com.moblin.android.view.settings.about.AboutSettingsView
import com.moblin.android.view.settings.applemusic.AppleMusicSettingsView
import com.moblin.android.view.settings.audio.AudioSettingsView
import com.moblin.android.view.settings.blacksharkcoolers.BlackSharkCoolerDevicesSettingsView
import com.moblin.android.view.settings.camera.CameraSettingsView
import com.moblin.android.view.settings.catprinters.CatPrintersSettingsView
import com.moblin.android.view.settings.chat.ChatSettingsView
import com.moblin.android.view.settings.debug.DebugSettingsView
import com.moblin.android.view.settings.deeplinkcreator.DeepLinkCreatorSettingsView
import com.moblin.android.view.settings.display.DisplaySettingsView
import com.moblin.android.view.settings.djidevices.DjiDevicesSettingsView
import com.moblin.android.view.settings.gamecontrollers.GameControllersSettingsView
import com.moblin.android.view.settings.gimbal.GimbalSettingsView
import com.moblin.android.view.settings.gopro.GoProSettingsView
import com.moblin.android.view.settings.helpandsupport.HelpAndSupportSettingsView
import com.moblin.android.view.settings.importexport.ImportExportSettingsView
import com.moblin.android.view.settings.ingests.IngestsSettingsView
import com.moblin.android.view.settings.keyboard.KeyboardSettingsView
import com.moblin.android.view.settings.location.LocationSettingsView
import com.moblin.android.view.settings.macros.MacrosSettingsView
import com.moblin.android.view.settings.mediaplayer.MediaPlayersSettingsView
import com.moblin.android.view.settings.moblink.MoblinkSettingsView
import com.moblin.android.view.settings.recordings.RecordingsSettingsView
import com.moblin.android.view.settings.remotecontrol.RemoteControlSettingsView
import com.moblin.android.view.settings.savereset.SettingsResetView
import com.moblin.android.view.settings.savereset.SettingsSaveView
import com.moblin.android.view.settings.scenes.ScenesSettingsView
import com.moblin.android.view.settings.selfiestick.SelfieStickSettingsView
import com.moblin.android.view.settings.store.StoreSettingsView
import com.moblin.android.view.settings.streamdeck.StreamDecksSettingsView
import com.moblin.android.view.settings.streaminghistory.StreamingHistorySettingsView
import com.moblin.android.view.settings.streams.StreamsSettingsView
import com.moblin.android.view.settings.talkback.TalkbackSettingsView
import com.moblin.android.view.settings.tesla.TeslaSettingsView
import com.moblin.android.view.settings.watch.WatchSettingsView
import com.moblin.android.view.settings.workoutdevices.WorkoutDevicesSettingsView
import com.moblin.android.view.utils.InfoBannerView
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.moblin.android.platform.swiftui.*

val settingsHalfWidth = 350.0


@Composable
private fun AppModeView(
    model: Model = LocalModel.current,
    database: Database,
) {
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val appMode = binding({ database.appMode }) {
        database.appMode = it
        model.appModeChanged()
    }
    Picker(
        title = "App mode",
        selection = appMode.value,
        options = SettingsAppMode.entries,
        enabled = !(isLive || isRecording),
    ) {
        appMode.value = it
    }
}

private val watchSettingsJson = Json {
    ignoreUnknownKeys = true
}

private fun decodeWatchSettings(watch: JsonObject): WatchSettings {
    return runCatching {
        watchSettingsJson.decodeFromJsonElement(WatchSettings.Serializer, watch)
    }.getOrElse { WatchSettings() }
}

@Composable
private fun AppleWatchSettingsDestination(model: Model = LocalModel.current, database: Database) {
    var watch by remember { mutableStateOf(decodeWatchSettings(database.watch)) }
    WatchSettingsView(
        model = model,
        watch = watch,
        onViaRemoteControlChange = { viaRemoteControl ->
            database.watch = JsonObject(database.watch + ("viaRemoteControl" to JsonPrimitive(viaRemoteControl)))
            watch = decodeWatchSettings(database.watch)
            model.sendInitToWatch()
        },
    )
}

@Composable
fun SettingsView(
    model: Model = LocalModel.current,
    database: Database,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val isLive by model.isLive.collectAsState()
    val showAllSettings = binding({ database.showAllSettings }) { database.showAllSettings = it }
    NavigationStack {
        Form(title = "Settings") {
            if (isLive) {
                InfoBannerView(text = "Settings that would stop the stream are disabled when live.")
            }
            Section {
                NavigationLink(destination = {
                    StreamsSettingsView(
                        model = model,
                        createStreamWizard = model.createStreamWizard,
                        database = database,
                    )
                }) {
                    Label("Streams", systemImage = "dot.radiowaves.left.and.right")
                }
                NavigationLink(destination = {
                    ScenesSettingsView(model = model, database = database)
                }) {
                    Label("Scenes", systemImage = "photo.on.rectangle")
                }
                NavigationLink(destination = {
                    val stream by model.stream.collectAsState()
                    ChatSettingsView(model = model, database = database, chat = database.chat, stream = stream)
                }) {
                    Label("Chat", systemImage = "message")
                }
                NavigationLink(destination = {
                    DisplaySettingsView(model = model, database = database)
                }) {
                    Label("Display", systemImage = "rectangle.inset.topright.fill")
                }
                NavigationLink(destination = {
                    val stream by model.stream.collectAsState()
                    CameraSettingsView(model = model, database = database, stream = stream, color = database.color)
                }) {
                    Label("Camera", systemImage = "camera")
                }
                if (showAllSettings.value) {
                    NavigationLink(destination = {
                        val stream by model.stream.collectAsState()
                        AudioSettingsView(
                            model = model,
                            database = database,
                            stream = stream,
                            mic = model.mic,
                            debug = database.debug,
                            audio = database.audio,
                        )
                    }) {
                        Label("Audio", systemImage = "waveform")
                    }
                    NavigationLink(destination = {
                        MacrosSettingsView(model = model, database = database, macros = database.macros)
                    }) {
                        Label("Macros", systemImage = "increase.indent")
                    }
                }
                NavigationLink(destination = {
                    val stream by model.stream.collectAsState()
                    LocationSettingsView(
                        model = model,
                        database = database,
                        location = database.location,
                        locationManager = model.locationManager,
                        stream = stream,
                    )
                }) {
                    Label("Location", systemImage = "location")
                }
            }
            Section {
                NavigationLink(destination = {
                    StoreSettingsView(model = model, store = model.store)
                }) {
                    Label(com.moblin.android.platform.loxen.Loxen.appName, systemImage = "pawprint")
                }
            }
            Section {
                if (showAllSettings.value) {
                    NavigationLink(destination = {
                        IngestsSettingsView(model = model, database = database)
                    }) {
                        Label("Ingests", systemImage = "server.rack")
                    }
                    NavigationLink(destination = {
                        TalkbackSettingsView(model = model, mics = database.mics, talkback = database.talkback)
                    }) {
                        Label("Talkback", systemImage = "speaker.wave.2.bubble.left")
                    }
                }
                NavigationLink(destination = {
                    MoblinkSettingsView(model = model, status = model.statusOther, streamer = database.moblink.streamer)
                }) {
                    Label("Moblink", systemImage = "app.connected.to.app.below.fill")
                }
                if (showAllSettings.value) {
                    NavigationLink(destination = {
                        MediaPlayersSettingsView(model = model, mediaPlayers = database.mediaPlayers)
                    }) {
                        Label("Media players", systemImage = "play.rectangle.on.rectangle")
                    }
                    NavigationLink(destination = {
                        AppleMusicSettingsView(model = model)
                    }) {
                        Label("Apple Music", systemImage = "music.note")
                    }
                }
            }
            if (showAllSettings.value) {
                Section {
                    NavigationLink(destination = {
                        GimbalSettingsView(model = model, gimbal = database.gimbal)
                    }) {
                        Label("Gimbal", systemImage = "iphone.dock.motorized.viewfinder")
                    }
                    NavigationLink(destination = {
                        SelfieStickSettingsView(model = model, selfieStick = database.selfieStick)
                    }) {
                        Label("Selfie stick", systemImage = "line.diagonal")
                    }
                    NavigationLink(destination = {
                        GameControllersSettingsView(model = model, database = database)
                    }) {
                        Label("Game controllers", systemImage = "gamecontroller")
                    }
                    NavigationLink(destination = {
                        KeyboardSettingsView(model = model, keyboard = database.keyboard)
                    }) {
                        Label("Keyboard", systemImage = "keyboard")
                    }
                    if (isPad() && !com.moblin.android.platform.loxen.Loxen.hidesStreamDecks) {
                        NavigationLink(destination = {
                            StreamDecksSettingsView(
                                model = model,
                                streamDeck = model.streamDeck,
                                streamDecks = database.streamDecks,
                            )
                        }) {
                            Label("Stream decks", systemImage = "square.grid.3x3.square")
                        }
                    }
                    NavigationLink(destination = {
                        val stream by model.stream.collectAsState()
                        RemoteControlSettingsView(
                            model = model,
                            database = database,
                            stream = stream,
                            onStreamChange = { model.stream.value = it },
                        )
                    }) {
                        Label("Remote control", systemImage = "appletvremote.gen1")
                    }
                }
                Section {
                    NavigationLink(destination = {
                        DjiDevicesSettingsView(model = model, djiDevices = database.djiDevices)
                    }) {
                        Label("DJI devices", systemImage = "appletvremote.gen1")
                    }
                    NavigationLink(destination = {
                        GoProSettingsView(model = model)
                    }) {
                        Label("GoPro", systemImage = "appletvremote.gen1")
                    }
                    NavigationLink(destination = {
                        CatPrintersSettingsView(model = model, catPrinters = database.catPrinters)
                    }) {
                        Label("Cat printers", systemImage = "pawprint")
                    }
                    NavigationLink(destination = {
                        TeslaSettingsView(model = model, tesla = model.tesla)
                    }) {
                        Label("Tesla", systemImage = "car.side")
                    }
                    NavigationLink(destination = {
                        WorkoutDevicesSettingsView(model = model, workoutDevices = database.workoutDevices)
                    }) {
                        Label("Workout devices", systemImage = "figure.walk.motion")
                    }
                    NavigationLink(destination = {
                        BlackSharkCoolerDevicesSettingsView(
                            model = model,
                            blackSharkCoolerDevices = database.blackSharkCoolerDevices,
                        )
                    }) {
                        Label("Black Shark coolers", systemImage = "fan")
                    }
                }
            }
            Section {
                NavigationLink(destination = {
                    RecordingsSettingsView(model = model)
                }) {
                    Label("Recordings", systemImage = "photo.on.rectangle.angled")
                }
                if (showAllSettings.value) {
                    NavigationLink(destination = {
                        StreamingHistorySettingsView(model = model)
                    }) {
                        Label("Streaming history", systemImage = "text.book.closed")
                    }
                }
            }
            if (showAllSettings.value && isPhone()) {
                Section {
                    NavigationLink(destination = {
                        AppleWatchSettingsDestination(model = model, database = database)
                    }) {
                        Label("Apple Watch", systemImage = "applewatch")
                    }
                }
            }
            Section {
                NavigationLink(destination = {
                    HelpAndSupportSettingsView()
                }) {
                    Label("Help and support", systemImage = "questionmark.circle")
                }
                if (showAllSettings.value) {
                    NavigationLink(destination = {
                        AboutSettingsView()
                    }) {
                        Label("About", systemImage = "info.circle")
                    }
                    NavigationLink(destination = {
                        DebugSettingsView(model = model, debug = database.debug)
                    }) {
                        Label("Debug", systemImage = "ladybug")
                    }
                }
            }
            if (showAllSettings.value) {
                Section {
                    NavigationLink(destination = {
                        ImportExportSettingsView(model = model)
                    }) {
                        Label("Import and export settings", systemImage = "gearshape")
                    }
                    NavigationLink(destination = {
                        DeepLinkCreatorSettingsView(deepLinkCreator = database.deepLinkCreator)
                    }) {
                        Label("Deep link creator", systemImage = "link.badge.plus")
                    }
                }
            }
            Section {
                if (showAllSettings.value && !isMac()) {
                    AppModeView(model = model, database = database)
                }
                Toggle("Show all settings", isOn = showAllSettings)
            }
            Section {
                SettingsSaveView(model = model)
            }
            Section {
                SettingsResetView(model = model)
            }
        }
    }
}
