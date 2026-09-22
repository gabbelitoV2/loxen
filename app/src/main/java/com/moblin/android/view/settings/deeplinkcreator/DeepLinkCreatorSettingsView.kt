package com.moblin.android.view.settings.deeplinkcreator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.moblin.android.various.MoblinQuickButtons
import com.moblin.android.various.MoblinSettingsButton
import com.moblin.android.various.MoblinSettingsUrl
import com.moblin.android.various.MoblinSettingsUrlStream
import com.moblin.android.various.MoblinSettingsUrlStreamAudio
import com.moblin.android.various.MoblinSettingsUrlStreamKick
import com.moblin.android.various.MoblinSettingsUrlStreamObs
import com.moblin.android.various.MoblinSettingsUrlStreamSrt
import com.moblin.android.various.MoblinSettingsUrlStreamTwitch
import com.moblin.android.various.MoblinSettingsUrlStreamVideo
import com.moblin.android.various.MoblinSettingsWebBrowser
import com.moblin.android.various.settings.DeepLinkCreator
import com.moblin.android.various.settings.DeepLinkCreatorStream
import com.moblin.android.various.utils.generateQrCode
import com.moblin.android.view.settings.gopro.qrCodeHeight
import com.moblin.android.view.utils.QrCodeImageView
import com.moblin.android.view.utils.TextButtonView
import kotlinx.serialization.json.Json
import com.moblin.android.LocalOnNavigate

private const val TAG = "DeepLinkCreatorSettingsView"

private const val defaultDeepLink = "moblin://?{}"

private fun createDeepLinkStream(stream: DeepLinkCreatorStream): MoblinSettingsUrlStream {
    val newStream = MoblinSettingsUrlStream(name = stream.name, url = stream.url)
    if (stream.selected) {
        newStream.selected = true
    }
    newStream.video = MoblinSettingsUrlStreamVideo()
    if (stream.video.resolution != MoblinSettingsUrlStreamVideo.Resolution.r1920x1080) {
        newStream.video!!.resolution = stream.video.resolution
    }
    if (stream.video.fps != 30) {
        newStream.video!!.fps = stream.video.fps
    }
    if (stream.video.bitrate != 5_000_000) {
        newStream.video!!.bitrate = stream.video.bitrate
    }
    newStream.video!!.codec = stream.video.codec
    if (stream.video.bFrames) {
        newStream.video!!.bFrames = stream.video.bFrames
    }
    if (stream.video.maxKeyFrameInterval != 2) {
        newStream.video!!.maxKeyFrameInterval = stream.video.maxKeyFrameInterval
    }
    if (stream.audio.bitrate != 128_000) {
        newStream.audio = MoblinSettingsUrlStreamAudio()
        if (stream.audio.bitrate != 128_000) {
            newStream.audio!!.bitrate = stream.audio.bitrate
        }
    }
    newStream.srt = MoblinSettingsUrlStreamSrt()
    newStream.srt!!.latency = stream.srt.latency
    newStream.srt!!.adaptiveBitrateEnabled = stream.srt.adaptiveBitrateEnabled
    newStream.srt!!.dnsLookupStrategy = stream.srt.dnsLookupStrategy
    if (stream.obs.webSocketUrl.isNotEmpty()) {
        newStream.obs = MoblinSettingsUrlStreamObs(
            webSocketUrl = stream.obs.webSocketUrl,
            webSocketPassword = stream.obs.webSocketPassword
        )
    }
    if (stream.twitch.channelName.isNotEmpty() || stream.twitch.channelId.isNotEmpty()) {
        newStream.twitch = MoblinSettingsUrlStreamTwitch(
            channelName = stream.twitch.channelName,
            channelId = stream.twitch.channelId
        )
    }
    if (stream.kick.channelName.isNotEmpty()) {
        newStream.kick = MoblinSettingsUrlStreamKick(channelName = stream.kick.channelName)
    }
    return newStream
}

private fun updateDeepLinkStreams(deepLinkCreator: DeepLinkCreator, settings: MoblinSettingsUrl) {
    if (deepLinkCreator.streams.isEmpty()) {
        return
    }
    settings.streams = mutableListOf()
    for (stream in deepLinkCreator.streams) {
        settings.streams!!.add(createDeepLinkStream(stream))
    }
}

private fun updateDeepLinkQuickButtons(deepLinkCreator: DeepLinkCreator, settings: MoblinSettingsUrl) {
    if (!deepLinkCreator.quickButtonsEnabled.value) {
        return
    }
    settings.quickButtons = MoblinQuickButtons()
    settings.quickButtons!!.enableScroll = deepLinkCreator.quickButtons.enableScroll
    settings.quickButtons!!.twoColumns = deepLinkCreator.quickButtons.twoColumns
    settings.quickButtons!!.showName = deepLinkCreator.quickButtons.showName
    settings.quickButtons!!.disableAllButtons = true
    for (button in deepLinkCreator.quickButtons.buttons) {
        if (!button.enabled) {
            continue
        }
        settings.quickButtons = settings.quickButtons ?: MoblinQuickButtons()
        settings.quickButtons!!.buttons = settings.quickButtons!!.buttons ?: mutableListOf()
        val newButton = MoblinSettingsButton(type = button.type)
        newButton.enabled = true
        newButton.page = button.page
        settings.quickButtons!!.buttons!!.add(newButton)
    }
}

private fun updateDeepLinkWebBrowser(deepLinkCreator: DeepLinkCreator, settings: MoblinSettingsUrl) {
    if (!deepLinkCreator.webBrowserEnabled.value) {
        return
    }
    settings.webBrowser = MoblinSettingsWebBrowser()
    settings.webBrowser!!.home = deepLinkCreator.webBrowser.home
}

private fun updateDeepLink(deepLinkCreator: DeepLinkCreator): String? {
    val settings = MoblinSettingsUrl()
    updateDeepLinkStreams(deepLinkCreator, settings)
    updateDeepLinkQuickButtons(deepLinkCreator, settings)
    updateDeepLinkWebBrowser(deepLinkCreator, settings)
    val jsonBlob = runCatching {
        Json.encodeToString(settings)
    }.getOrNull()
    if (jsonBlob == null) {
        Log.i(TAG, "Failed to create deep link")
        return null
    }
    var encodedJsonBlob = Uri.encode(jsonBlob)
    encodedJsonBlob = encodedJsonBlob.replace("%7B", "{")
    encodedJsonBlob = encodedJsonBlob.replace("%7D", "}")
    encodedJsonBlob = encodedJsonBlob.replace("%5B", "[")
    encodedJsonBlob = encodedJsonBlob.replace("%5D", "]")
    encodedJsonBlob = encodedJsonBlob.replace("%22", "\"")
    return "moblin://?$encodedJsonBlob"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkCreatorSettingsView(
    deepLinkCreator: DeepLinkCreator,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    var deepLink by remember { mutableStateOf(defaultDeepLink) }
    val quickButtonsEnabled by deepLinkCreator.quickButtonsEnabled.collectAsState()
    val webBrowserEnabled by deepLinkCreator.webBrowserEnabled.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(quickButtonsEnabled, webBrowserEnabled) {
        updateDeepLink(deepLinkCreator)?.let { deepLink = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Deep link creator") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("DeepLinkCreatorStreamsSettingsView") }
                        .padding(16.dp)
                ) {
                    Text("Streams")
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Quick buttons",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("DeepLinkCreatorQuickButtonsSettingsView") }
                    )
                    Switch(
                        checked = quickButtonsEnabled,
                        onCheckedChange = { deepLinkCreator.quickButtonsEnabled.value = it }
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Web browser",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate("DeepLinkCreatorWebBrowserSettingsView") }
                    )
                    Switch(
                        checked = webBrowserEnabled,
                        onCheckedChange = { deepLinkCreator.webBrowserEnabled.value = it }
                    )
                }
            }
            if (deepLink != defaultDeepLink) {
                item {
                    TextButtonView("Copy to clipboard") {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("deep link", deepLink))
                    }
                }
                item {
                    val image = generateQrCode(deepLink)
                    if (image != null) {
                        QrCodeImageView(image = image, height = qrCodeHeight())
                    } else {
                        Text("Failed to create QR-code.")
                    }
                }
            } else {
                item {
                    Text(
                        "A QR code and copy to clipboard button will show up here when the " +
                            "settings above have been modified (and are not all default values)."
                    )
                }
            }
        }
    }
}
