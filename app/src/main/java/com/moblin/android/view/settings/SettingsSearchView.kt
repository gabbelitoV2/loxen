package com.moblin.android.view.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.fallbackStream
import com.moblin.android.various.settings.Database
import com.moblin.android.various.settings.SettingsStreamProtocol
import com.moblin.android.various.utils.Identifiable
import com.moblin.android.various.utils.isMac
import com.moblin.android.various.utils.isPad
import com.moblin.android.various.utils.isPhone
import com.moblin.android.view.controlbar.quickbutton.QuickButtonMicView
import com.moblin.android.view.settings.about.AboutSettingsView
import com.moblin.android.view.settings.applemusic.AppleMusicSettingsView
import com.moblin.android.view.settings.audio.AudioSettingsView
import com.moblin.android.view.settings.audio.MicsDelaySettingsView
import com.moblin.android.view.settings.audio.MicsGainSettingsView
import com.moblin.android.view.settings.bitratepresets.BitratePresetsSettingsView
import com.moblin.android.view.settings.blacksharkcoolers.BlackSharkCoolerDevicesSettingsView
import com.moblin.android.view.settings.camera.CameraSettingsAppleLogLutView
import com.moblin.android.view.settings.camera.CameraSettingsLutsView
import com.moblin.android.view.settings.camera.CameraSettingsView
import com.moblin.android.view.settings.camera.zoom.ZoomSettingsView
import com.moblin.android.view.settings.catprinters.CatPrintersSettingsView
import com.moblin.android.view.settings.chat.ChatBotSettingsView
import com.moblin.android.view.settings.chat.ChatSettingsView
import com.moblin.android.view.settings.chat.ChatTextToSpeechSettingsView
import com.moblin.android.view.settings.debug.DebugSettingsView
import com.moblin.android.view.settings.deeplinkcreator.DeepLinkCreatorSettingsView
import com.moblin.android.view.settings.display.DisplaySettingsView
import com.moblin.android.view.settings.display.localoverlays.LocalOverlaysSettingsView
import com.moblin.android.view.settings.display.networkinterfacenames.LocalOverlaysNetworkInterfaceNamesSettingsView
import com.moblin.android.view.settings.display.quickbuttons.QuickButtonsSettingsView
import com.moblin.android.view.settings.display.streambutton.StreamButtonsSettingsView
import com.moblin.android.view.settings.djidevices.DjiDevicesSettingsView
import com.moblin.android.view.settings.gamecontrollers.GameControllersSettingsView
import com.moblin.android.view.settings.gimbal.GimbalSettingsView
import com.moblin.android.view.settings.gopro.GoProSettingsView
import com.moblin.android.view.settings.helpandsupport.HelpAndSupportSettingsView
import com.moblin.android.view.settings.httpproxy.HttpProxySettingsView
import com.moblin.android.view.settings.importexport.ImportExportSettingsView
import com.moblin.android.view.settings.ingests.IngestsSettingsView
import com.moblin.android.view.settings.keyboard.KeyboardSettingsView
import com.moblin.android.view.settings.location.LocationSettingsView
import com.moblin.android.view.settings.macros.MacrosSettingsView
import com.moblin.android.view.settings.mediaplayer.MediaPlayersSettingsView
import com.moblin.android.view.settings.moblincloud.MoblinCloudSettingsView
import com.moblin.android.view.settings.moblink.MoblinkSettingsView
import com.moblin.android.view.settings.recordings.RecordingsSettingsView
import com.moblin.android.view.settings.remotecontrol.RemoteControlSettingsView
import com.moblin.android.view.settings.scenes.ScenesSettingsView
import com.moblin.android.view.settings.selfiestick.SelfieStickSettingsView
import com.moblin.android.view.settings.store.StoreSettingsView
import com.moblin.android.view.settings.streamdeck.StreamDecksSettingsView
import com.moblin.android.view.settings.streaminghistory.StreamingHistorySettingsView
import com.moblin.android.view.settings.streams.StreamsSettingsView
import com.moblin.android.view.settings.streams.stream.StreamSettingsView
import com.moblin.android.view.settings.streams.stream.audio.StreamAudioSettingsView
import com.moblin.android.view.settings.streams.stream.chat.StreamEmotesSettingsView
import com.moblin.android.view.settings.streams.stream.golivenotification.GoLiveNotificationSettingsView
import com.moblin.android.view.settings.streams.stream.kick.StreamKickSettingsView
import com.moblin.android.view.settings.streams.stream.obsremotecontrol.StreamObsRemoteControlSettingsView
import com.moblin.android.view.settings.streams.stream.recording.StreamRecordingSettingsView
import com.moblin.android.view.settings.streams.stream.replay.StreamReplaySettingsView
import com.moblin.android.view.settings.streams.stream.rist.StreamRistSettingsView
import com.moblin.android.view.settings.streams.stream.rtmp.StreamRtmpSettingsView
import com.moblin.android.view.settings.streams.stream.snapshot.StreamSnapshotSettingsView
import com.moblin.android.view.settings.streams.stream.srt.StreamSrtSettingsView
import com.moblin.android.view.settings.streams.stream.twitch.StreamTwitchSettingsView
import com.moblin.android.view.settings.streams.stream.video.StreamVideoSettingsView
import com.moblin.android.view.settings.streams.stream.whip.StreamWhipSettingsView
import com.moblin.android.view.settings.streams.stream.youtube.StreamYouTubeSettingsView
import com.moblin.android.view.settings.talkback.TalkbackSettingsView
import com.moblin.android.view.settings.tesla.TeslaSettingsView
import com.moblin.android.view.settings.watch.WatchSettingsView
import com.moblin.android.view.settings.watch.chat.WatchChatSettingsView
import com.moblin.android.view.settings.watch.display.WatchDisplaySettingsView
import com.moblin.android.view.settings.workoutdevices.WorkoutDevicesSettingsView
import java.text.Normalizer

data class SettingsSearchItem(
    val title: String,
    val path: List<String>,
    val name: String?,
    val image: String,
    val keywords: List<String>,
    val destination: @Composable () -> Unit,
) : Identifiable<String> {
    override val id: String
        get() = (path + listOf(title)).joinToString("/")
}

private fun settingsSearchWords(text: String): List<String> {
    val folded = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    return folded.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
}

fun settingsSearchMatches(text: String, title: String, path: List<String>, keywords: List<String>): Boolean {
    val filterWords = settingsSearchWords(text)
    if (filterWords.isEmpty()) {
        return false
    }
    val words = settingsSearchWords((listOf(title) + path + keywords).joinToString(" "))
    return filterWords.all { filterWord -> words.any { it.startsWith(filterWord) } }
}

private fun searchKeywords(vararg values: String): List<String> = values.map { localized(it) }

private class SettingsSearchItemsBuilder {
    val items: MutableList<SettingsSearchItem> = mutableListOf()
    var name: String? = null

    fun add(
        title: String,
        path: List<String>,
        image: String,
        keywords: List<String>,
        destination: @Composable () -> Unit,
    ) {
        items.add(
            SettingsSearchItem(
                title = title,
                path = path,
                name = name,
                image = image,
                keywords = keywords,
                destination = destination,
            )
        )
    }
}

fun settingsSearchItems(model: Model, database: Database): List<SettingsSearchItem> {
    val builder = SettingsSearchItemsBuilder()
    val showAll = database.showAllSettings
    val stream = model.stream.value
    val streams = localized("Streams")
    val scenes = localized("Scenes")
    val chat = localized("Chat")
    val display = localized("Display")
    val camera = localized("Camera")
    val audio = localized("Audio")
    val ingests = localized("Ingests")
    val debug = localized("Debug")
    val watch = localized("Apple Watch")
    val streamImage = "dot.radiowaves.left.and.right"
    builder.add(
        streams,
        emptyList(),
        streamImage,
        searchKeywords("Stream", "Platform", "Server", "Destination") + listOf("url"),
    ) {
        StreamsSettingsView(createStreamWizard = model.createStreamWizard, database = database)
    }
    if (stream !== fallbackStream) {
        val streamPath = listOf(streams)
        builder.name = stream.name
        builder.add(
            localized("Video"),
            streamPath,
            streamImage,
            searchKeywords("Bitrate", "Resolution", "FPS", "Framerate", "Codec", "Adaptive", "Keyframe") +
                listOf("hevc", "h264"),
        ) {
            StreamVideoSettingsView(database = database, stream = stream)
        }
        if (showAll) {
            builder.add(
                localized("Audio"),
                streamPath,
                streamImage,
                searchKeywords("Audio bitrate", "Codec") + listOf("aac", "opus"),
            ) {
                StreamAudioSettingsView(stream = stream)
            }
            when (stream.getProtocol()) {
                SettingsStreamProtocol.srt -> builder.add(
                    localized("SRT(LA)"),
                    streamPath,
                    streamImage,
                    searchKeywords("Latency", "Bonding", "Adaptive bitrate") + listOf("srt", "srtla"),
                ) {
                    StreamSrtSettingsView(stream = stream, srt = stream.srt)
                }

                SettingsStreamProtocol.rtmp -> builder.add(
                    localized("RTMP"),
                    streamPath,
                    streamImage,
                    listOf("rtmp", "rtmps"),
                ) {
                    StreamRtmpSettingsView(stream = stream)
                }

                SettingsStreamProtocol.rist -> builder.add(
                    localized("RIST"),
                    streamPath,
                    streamImage,
                    searchKeywords("Bonding") + listOf("rist"),
                ) {
                    StreamRistSettingsView(stream = stream)
                }

                SettingsStreamProtocol.whip -> builder.add(
                    localized("WHIP"),
                    streamPath,
                    streamImage,
                    listOf("whip", "webrtc"),
                ) {
                    StreamWhipSettingsView(model = model, stream = stream, whip = stream.whip)
                }

                else -> {}
            }
            builder.add(
                localized("Recording"),
                streamPath,
                streamImage,
                searchKeywords("Record", "Save", "Quality"),
            ) {
                StreamRecordingSettingsView(stream = stream, recording = stream.recording)
            }
            builder.add(
                localized("Snapshot"),
                streamPath,
                streamImage,
                searchKeywords("Photo", "Screenshot"),
            ) {
                StreamSnapshotSettingsView(model = model, stream = stream, recording = stream.recording)
            }
            builder.add(
                localized("Go live notification"),
                streamPath,
                streamImage,
                searchKeywords("Announce") + listOf("discord"),
            ) {
                GoLiveNotificationSettingsView(stream = stream)
            }
        }
        builder.add(
            localized("Replay"),
            streamPath,
            streamImage,
            searchKeywords("Instant replay", "Clip"),
        ) {
            StreamReplaySettingsView(database = database, stream = stream, replay = stream.replay)
        }
        builder.add(
            localized("Twitch"),
            streamPath,
            streamImage,
            searchKeywords("Login", "Channel"),
        ) {
            StreamTwitchSettingsView(stream = stream, loggedInInitial = stream.twitchLoggedIn)
        }
        builder.add(
            localized("YouTube"),
            streamPath,
            streamImage,
            searchKeywords("Login", "Channel"),
        ) {
            StreamYouTubeSettingsView(debug = database.debug, stream = stream)
        }
        builder.add(
            localized("Kick"),
            streamPath,
            streamImage,
            searchKeywords("Login", "Channel"),
        ) {
            StreamKickSettingsView(model = model, stream = stream)
        }
        builder.add(
            localized("OBS remote control"),
            streamPath,
            streamImage,
            searchKeywords("Scene") + listOf("obs", "websocket"),
        ) {
            StreamObsRemoteControlSettingsView(stream = stream)
        }
        builder.add(
            localized("Emotes"),
            streamPath,
            streamImage,
            searchKeywords("Emoji") + listOf("bttv", "ffz", "7tv"),
        ) {
            StreamEmotesSettingsView(stream = stream)
        }
        builder.add(
            localized("Background streaming"),
            streamPath,
            streamImage,
            searchKeywords("Background", "Picture in picture", "Portrait", "Multi streaming") + listOf("pip"),
        ) {
            StreamSettingsView(database = database, stream = stream)
        }
        builder.add(
            localized("Bitrate presets"),
            streamPath,
            streamImage,
            searchKeywords("Bitrate", "Quick buttons"),
        ) {
            BitratePresetsSettingsView(database = database)
        }
    }
    builder.name = null
    val scenesImage = "photo.on.rectangle"
    builder.add(
        scenes,
        emptyList(),
        scenesImage,
        searchKeywords("Scene", "Layout", "Widgets", "Overlay", "Browser source"),
    ) {
        ScenesSettingsView(model = model, database = database)
    }
    if (showAll) {
        builder.add(
            localized("Auto scene switchers"),
            listOf(scenes),
            scenesImage,
            searchKeywords("Rotate"),
        ) {
            ScenesSettingsView(model = model, database = database)
        }
        builder.add(
            localized("Disconnect protection"),
            listOf(scenes),
            scenesImage,
            searchKeywords("Be right back", "Fallback", "Offline") + listOf("brb"),
        ) {
            ScenesSettingsView(model = model, database = database)
        }
    }
    val chatImage = "message"
    builder.add(
        chat,
        emptyList(),
        chatImage,
        searchKeywords("Messages", "Font") + listOf("twitch", "youtube", "kick"),
    ) {
        ChatSettingsView(model = model, database = database, chat = database.chat, stream = stream)
    }
    builder.add(
        localized("Bot"),
        listOf(chat),
        chatImage,
        searchKeywords("Chat bot", "Commands"),
    ) {
        ChatBotSettingsView()
    }
    builder.add(
        localized("Text to speech"),
        listOf(chat),
        chatImage,
        searchKeywords("Voice", "Read aloud") + listOf("tts"),
    ) {
        ChatTextToSpeechSettingsView(
            model = model,
            chat = database.chat,
            ttsMonster = database.chat.ttsMonster,
            elevenLabs = database.chat.elevenLabs,
        )
    }
    builder.add(
        localized("Filters"),
        listOf(chat),
        chatImage,
        searchKeywords("Filter", "Block", "Hide", "Nicknames"),
    ) {
        ChatSettingsView(model = model, database = database, chat = database.chat, stream = stream)
    }
    val displayImage = "rectangle.inset.topright.fill"
    builder.add(
        display,
        emptyList(),
        displayImage,
        searchKeywords("Screen", "User interface", "Layout") + listOf("ui"),
    ) {
        DisplaySettingsView(database = database)
    }
    builder.add(
        localized("Quick buttons"),
        listOf(display),
        displayImage,
        searchKeywords("Buttons", "Shortcuts", "Control bar"),
    ) {
        QuickButtonsSettingsView(model = model, showAll = true)
    }
    builder.add(
        localized("Stream button"),
        listOf(display),
        displayImage,
        searchKeywords("Go live", "Button", "Color"),
    ) {
        StreamButtonsSettingsView(database = database)
    }
    builder.add(
        localized("Big buttons"),
        listOf(display),
        displayImage,
        searchKeywords("Large", "Size", "Vertical buttons", "Big audio level meter"),
    ) {
        DisplaySettingsView(database = database)
    }
    builder.add(
        localized("Vibrate"),
        listOf(display),
        displayImage,
        searchKeywords("Haptics"),
    ) {
        DisplaySettingsView(database = database)
    }
    if (!isMac()) {
        builder.add(
            localized("Auto rotate to portrait"),
            listOf(display),
            displayImage,
            searchKeywords("Rotation", "Orientation", "Portrait", "Landscape"),
        ) {
            DisplaySettingsView(database = database)
        }
    }
    if (showAll) {
        builder.add(
            localized("Local overlays"),
            listOf(display),
            displayImage,
            searchKeywords("Status", "Overlay", "Info"),
        ) {
            LocalOverlaysSettingsView(show = database.show)
        }
        builder.add(
            localized("Network interface names"),
            listOf(display),
            displayImage,
            searchKeywords("Cellular", "Ethernet", "Bonding") + listOf("wifi"),
        ) {
            LocalOverlaysNetworkInterfaceNamesSettingsView(model = model, database = database)
        }
        builder.add(
            localized("Low bitrate warning"),
            listOf(display),
            displayImage,
            searchKeywords("Recording confirmations"),
        ) {
            DisplaySettingsView(database = database)
        }
    }
    val cameraImage = "camera"
    builder.add(
        camera,
        emptyList(),
        cameraImage,
        searchKeywords("Video", "Lens", "Color space"),
    ) {
        CameraSettingsView(database = database, stream = stream, color = database.color)
    }
    builder.add(
        localized("Video stabilization"),
        listOf(camera),
        cameraImage,
        searchKeywords("Mirror"),
    ) {
        CameraSettingsView(database = database, stream = stream, color = database.color)
    }
    if (showAll) {
        builder.add(
            localized("Zoom"),
            listOf(camera),
            cameraImage,
            searchKeywords("Zoom", "Presets", "Lens"),
        ) {
            ZoomSettingsView(zoom = database.zoom)
        }
        builder.add(
            localized("Tap screen to focus"),
            listOf(camera),
            cameraImage,
            searchKeywords(
                "Focus",
                "Exposure",
                "Fixed horizon",
                "Camera controls",
                "Volume",
                "Buttons",
            ),
        ) {
            CameraSettingsView(database = database, stream = stream, color = database.color)
        }
        builder.add(
            localized("LUTs"),
            listOf(camera),
            cameraImage,
            searchKeywords("Color", "Grading", "Filter") + listOf("lut", "cube"),
        ) {
            CameraSettingsLutsView(color = database.color)
        }
        if (model.supportsAppleLog || model.supportsAppleLog2) {
            builder.add(
                localized("Apple Log LUT"),
                listOf(camera),
                cameraImage,
                searchKeywords("Color space") + listOf("apple log", "hdr"),
            ) {
                CameraSettingsAppleLogLutView(model = model, color = database.color)
            }
        }
    }
    if (showAll) {
        val audioImage = "waveform"
        builder.add(
            audio,
            emptyList(),
            audioImage,
            searchKeywords("Sound", "Microphone", "Bluetooth", "Stereo"),
        ) {
            AudioSettingsView(
                database = database,
                stream = stream,
                mic = model.mic,
                debug = database.debug,
                audio = database.audio,
            )
        }
        builder.add(
            localized("Mic"),
            listOf(audio),
            audioImage,
            searchKeywords("Microphone", "Input") + listOf("usb"),
        ) {
            QuickButtonMicView(model = model, mics = database.mics, modelMic = model.mic)
        }
        builder.add(
            localized("Delays"),
            listOf(audio),
            audioImage,
            searchKeywords("Sync"),
        ) {
            MicsDelaySettingsView(model = model, mics = database.mics)
        }
        builder.add(
            localized("Gains"),
            listOf(audio),
            audioImage,
            searchKeywords("Volume", "Loudness"),
        ) {
            MicsGainSettingsView(model = model, mics = database.mics)
        }
        builder.add(
            localized("Macros"),
            emptyList(),
            "increase.indent",
            searchKeywords("Automation", "Actions"),
        ) {
            MacrosSettingsView(model = model, database = database, macros = database.macros)
        }
    }
    builder.add(
        localized("Location"),
        emptyList(),
        "location",
        searchKeywords("Map", "Speed", "Altitude", "Privacy") + listOf("gps"),
    ) {
        LocationSettingsView(
            database = database,
            location = database.location,
            locationManager = model.locationManager,
            stream = model.stream.value,
        )
    }
    builder.add(
        localized("Store (support us) ❤️"),
        emptyList(),
        "cart",
        searchKeywords("Donate", "Buy"),
    ) {
        StoreSettingsView(model = model, store = model.store)
    }
    if (showAll) {
        val ingestsImage = "server.rack"
        builder.add(
            ingests,
            emptyList(),
            ingestsImage,
            searchKeywords("Server", "Receive", "Input") +
                listOf("rtmp", "srt", "srtla", "rist", "whip", "rtsp", "whep"),
        ) {
            IngestsSettingsView(model = model, database = database)
        }
        builder.add(
            localized("Talkback"),
            emptyList(),
            "speaker.wave.2.bubble.left",
            searchKeywords("Intercom", "Producer"),
        ) {
            TalkbackSettingsView(model = model, mics = database.mics, talkback = database.talkback)
        }
    }
    builder.add(
        localized("Moblink"),
        emptyList(),
        "app.connected.to.app.below.fill",
        searchKeywords("Bonding", "Relay", "Phone", "Hotspot"),
    ) {
        MoblinkSettingsView(status = model.statusOther, streamer = database.moblink.streamer)
    }
    if (showAll) {
        builder.add(
            localized("Media players"),
            emptyList(),
            "play.rectangle.on.rectangle",
            searchKeywords("Video", "Playlist", "Play"),
        ) {
            MediaPlayersSettingsView(model = model, mediaPlayers = database.mediaPlayers)
        }
        builder.add(
            localized("Apple Music"),
            emptyList(),
            "music.note",
            searchKeywords("Music", "Song", "Playlist"),
        ) {
            AppleMusicSettingsView(model = model, musicPlaylist = model.musicPlaylist)
        }
        builder.add(
            localized("Gimbal"),
            emptyList(),
            "iphone.dock.motorized.viewfinder",
            searchKeywords("Stabilizer", "Tracking"),
        ) {
            GimbalSettingsView(model = model, gimbal = database.gimbal)
        }
        builder.add(
            localized("Selfie stick"),
            emptyList(),
            "line.diagonal",
            searchKeywords("Button", "Remote"),
        ) {
            SelfieStickSettingsView(model = model, selfieStick = database.selfieStick)
        }
        builder.add(
            localized("Game controllers"),
            emptyList(),
            "gamecontroller",
            searchKeywords("Gamepad", "Controller") + listOf("xbox", "playstation"),
        ) {
            GameControllersSettingsView(model = model, database = database)
        }
        builder.add(
            localized("Keyboard"),
            emptyList(),
            "keyboard",
            searchKeywords("Hotkeys", "Shortcuts"),
        ) {
            KeyboardSettingsView(model = model, keyboard = database.keyboard)
        }
        if (isPad()) {
            builder.add(
                localized("Stream decks"),
                emptyList(),
                "square.grid.3x3.square",
                searchKeywords("Buttons") + listOf("elgato"),
            ) {
                StreamDecksSettingsView(
                    model = model,
                    streamDeck = model.streamDeck,
                    streamDecks = database.streamDecks,
                )
            }
        }
        builder.add(
            localized("Remote control"),
            emptyList(),
            "appletvremote.gen1",
            searchKeywords("Assistant", "Streamer", "Relay", "Web"),
        ) {
            RemoteControlSettingsView(
                database = database,
                stream = model.stream.value,
                onStreamChange = { model.stream.value = it },
            )
        }
        if (!isMac()) {
            builder.add(
                localized("Moblin Cloud"),
                emptyList(),
                "cloud",
                searchKeywords("Account", "Server"),
            ) {
                MoblinCloudSettingsView(model = model, database = database, moblinCloud = database.moblinCloud)
            }
        }
        builder.add(
            localized("DJI devices"),
            emptyList(),
            "appletvremote.gen1",
            searchKeywords("Action camera", "Pocket") + listOf("dji", "osmo"),
        ) {
            DjiDevicesSettingsView(model = model, djiDevices = database.djiDevices)
        }
        builder.add(
            localized("GoPro"),
            emptyList(),
            "appletvremote.gen1",
            searchKeywords("Action camera", "QR code"),
        ) {
            GoProSettingsView()
        }
        builder.add(
            localized("Cat printers"),
            emptyList(),
            "pawprint",
            searchKeywords("Print", "Thermal"),
        ) {
            CatPrintersSettingsView(model = model, catPrinters = database.catPrinters)
        }
        builder.add(
            localized("Tesla"),
            emptyList(),
            "car.side",
            searchKeywords("Car", "Vehicle"),
        ) {
            TeslaSettingsView(tesla = model.tesla)
        }
        builder.add(
            localized("Workout devices"),
            emptyList(),
            "figure.walk.motion",
            searchKeywords("Heart rate", "Cycling", "Bluetooth"),
        ) {
            WorkoutDevicesSettingsView(model = model, workoutDevices = database.workoutDevices)
        }
        builder.add(
            localized("Black Shark coolers"),
            emptyList(),
            "fan",
            searchKeywords("Cooler", "Fan", "Temperature", "Thermal", "Overheating"),
        ) {
            BlackSharkCoolerDevicesSettingsView(
                model = model,
                blackSharkCoolerDevices = database.blackSharkCoolerDevices,
            )
        }
    }
    builder.add(
        localized("Recordings"),
        emptyList(),
        "photo.on.rectangle.angled",
        searchKeywords("Video"),
    ) {
        RecordingsSettingsView(model = model)
    }
    if (showAll) {
        builder.add(
            localized("Streaming history"),
            emptyList(),
            "text.book.closed",
            searchKeywords("Statistics"),
        ) {
            StreamingHistorySettingsView(model = model)
        }
        if (isPhone()) {
            val watchImage = "applewatch"
            builder.add(watch, emptyList(), watchImage, emptyList()) {
                WatchSettingsView(
                    model = model,
                    watch = TODO("Watch settings are not implemented"),
                    onViaRemoteControlChange = {},
                )
            }
            builder.add(localized("Chat"), listOf(watch), watchImage, emptyList()) {
                WatchChatSettingsView(model = model, chat = TODO("Watch chat settings are not implemented"))
            }
            builder.add(localized("Display"), listOf(watch), watchImage, emptyList()) {
                WatchDisplaySettingsView(show = TODO("Watch display settings are not implemented"))
            }
        }
    }
    builder.add(
        localized("Help and support"),
        emptyList(),
        "questionmark.circle",
        searchKeywords("Help", "Manual") + listOf("discord", "github", "faq"),
    ) {
        HelpAndSupportSettingsView()
    }
    if (showAll) {
        builder.add(localized("About"), emptyList(), "info.circle", searchKeywords("Version", "License")) {
            AboutSettingsView()
        }
        builder.add(debug, emptyList(), "ladybug", searchKeywords("Log", "Logs", "Experimental")) {
            DebugSettingsView(model = model, debug = database.debug)
        }
        builder.add(localized("HTTP proxy"), listOf(debug), "ladybug", listOf("http")) {
            HttpProxySettingsView(model = model, status = model.statusOther, httpProxy = database.httpProxy)
        }
        builder.add(
            localized("Import and export settings"),
            emptyList(),
            "gearshape",
            searchKeywords("Backup", "Restore", "Export"),
        ) {
            ImportExportSettingsView(model = model)
        }
        builder.add(
            localized("Deep link creator"),
            emptyList(),
            "link.badge.plus",
            searchKeywords("QR code", "Share settings") + listOf("url"),
        ) {
            DeepLinkCreatorSettingsView(deepLinkCreator = database.deepLinkCreator)
        }
    }
    return builder.items
}

@Composable
fun SettingsSearchFieldView(text: MutableState<String>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SystemImage(name = "magnifyingglass", fontSize = 17.sp, tint = formPalette().secondaryLabel)
        Box(modifier = Modifier.weight(1f)) {
            if (text.value.isEmpty()) {
                Text(localized("Search"), fontSize = 17.sp, color = formPalette().secondaryLabel)
            }
            BasicTextField(
                value = text.value,
                onValueChange = { text.value = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = formBodyStyle.copy(color = formPalette().label),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                ),
                singleLine = true,
                cursorBrush = SolidColor(formPalette().accent),
            )
        }
    }
}

@Composable
fun SettingsSearchResultsView(items: List<SettingsSearchItem>, text: String) {
    val filteredItems = items.filter {
        settingsSearchMatches(text = text, title = it.title, path = it.path, keywords = it.keywords)
    }
    if (filteredItems.isEmpty()) {
        Text(localized("No results"), fontSize = 17.sp, color = formPalette().secondaryLabel)
    } else {
        Section {
            ForEach(filteredItems, id = { it.id }) { item ->
                NavigationLink(destination = { item.destination() }) {
                    SystemImage(name = item.image, fontSize = 17.sp, tint = formPalette().accent)
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(item.title, fontSize = 17.sp)
                        if (item.path.isNotEmpty()) {
                            Text(
                                (item.path + listOfNotNull(item.name)).joinToString(" › "),
                                fontSize = 12.sp,
                                color = formPalette().secondaryLabel,
                            )
                        }
                    }
                }
            }
        }
    }
}
