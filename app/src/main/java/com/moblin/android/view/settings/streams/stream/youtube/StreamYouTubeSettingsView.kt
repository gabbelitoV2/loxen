package com.moblin.android.view.settings.streams.stream.youtube

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.isValidRtmpUrl
import com.moblin.android.localized
import com.moblin.android.platform.Bundle
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.ButtonRole
import com.moblin.android.platform.swiftui.ConfirmationDialog
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormButton
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.Visibility
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.streamingplatforms.youtube.YouTubeApi
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveBroadcast
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveBroadcaseVisibility
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveStream
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveStreamsListResponse
import com.moblin.android.streamingplatforms.youtube.fetchYouTubeVideoId
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getYouTubeApi
import com.moblin.android.various.model.reloadStreamIfEnabled
import com.moblin.android.various.model.youTubeSignIn
import com.moblin.android.various.model.youTubeSignOut
import com.moblin.android.various.model.youTubeVideoIdUpdated
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextEditNavigationView
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed class ScheduleStreamState {
    data object Idle : ScheduleStreamState()

    data object InProgress : ScheduleStreamState()

    data object Succeeded : ScheduleStreamState()

    data class Failed(val message: String) : ScheduleStreamState()
}

@Composable
private fun StreamDescriptionView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    youTubeStream: YouTubeApiLiveBroadcast,
    ingests: List<YouTubeApiLiveStream>,
    thumbnailUrl: String,
    startTime: Instant,
) {
    var presentingConfigureConfirm by remember { mutableStateOf(false) }
    val isLive by model.isLive.collectAsState()
    val isRecording by model.isRecording.collectAsState()
    val palette = formPalette()

    fun details(): String {
        val details = mutableListOf(
            youTubeStream.status.visibility()?.toString() ?: localized("Unknown")
        )
        if (youTubeStream.contentDetails.enableAutoStart) {
            details.add(localized("Auto-start"))
        }
        if (youTubeStream.contentDetails.enableAutoStop) {
            details.add(localized("Auto-stop"))
        }
        return details.joinToString(", ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    fun url(): String {
        var url = "-"
        val ingest = ingests.firstOrNull { youTubeStream.contentDetails.boundStreamId == it.id }
        if (ingest != null) {
            val ingestionInfo = ingest.cdn.ingestionInfo
            url = "${ingestionInfo.ingestionAddress}/${ingestionInfo.streamName}"
        }
        return url
    }

    val ingestsUrl = url()
    val startTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CacheAsyncImage(
                url = URI(thumbnailUrl),
                content = { image ->
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .size(50.dp)
                    )
                },
                placeholder = {
                    Bundle.image("AppIconNoBackground")?.asImageBitmap()?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .size(50.dp)
                        )
                    }
                }
            )
            Column(
                modifier = Modifier.padding(start = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(youTubeStream.snippet.title, color = palette.label)
                Text(
                    startTimeFormatter.format(startTime),
                    style = formFootnoteStyle,
                    color = palette.secondaryLabel
                )
                Text(details(), style = formFootnoteStyle, color = palette.secondaryLabel)
            }
        }
        if (stream.url != ingestsUrl || !stream.getYouTubeVideoIds().contains(youTubeStream.id)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    localized("⚠️ Moblin is not configured to stream to this stream."),
                    style = formFootnoteStyle,
                    color = palette.label
                )
                val configureEnabled = !(isLive || isRecording)
                Text(
                    localized("Configure"),
                    style = formFootnoteStyle,
                    color = if (configureEnabled) palette.accent else palette.secondaryLabel,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = configureEnabled
                    ) {
                        presentingConfigureConfirm = true
                    }
                )
            }
        }
    }
    ConfirmationDialog(
        title = "Overwrite Settings → Streams → ${stream.name} → URL?",
        isPresented = presentingConfigureConfirm,
        onDismissRequest = { presentingConfigureConfirm = false },
        titleVisibility = Visibility.visible,
    ) {
        Button("Yes", role = ButtonRole.destructive) {
            if (isValidRtmpUrl(url = ingestsUrl, rtmpStreamKeyRequired = true) == null) {
                stream.url = ingestsUrl
                stream.youTubeVideoIds = youTubeStream.id
                model.reloadStreamIfEnabled(stream)
            }
        }
        Button("No") {
            stream.youTubeVideoIds = youTubeStream.id
            model.youTubeVideoIdUpdated()
        }
    }
}

@Composable
private fun YouTubeStreamView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    youTubeStream: YouTubeApiLiveBroadcast,
    ingests: List<YouTubeApiLiveStream>,
    destroyImage: String,
    destroyText: String,
    destroy: (String, YouTubeApi, () -> Unit) -> Unit,
) {
    var destroying by remember { mutableStateOf(false) }
    var presentingConfirm by remember { mutableStateOf(false) }
    val palette = formPalette()

    fun handleDestroy() {
        destroying = true
        model.getYouTubeApi(stream) { youTubeApi ->
            if (youTubeApi == null) {
                destroying = false
                return@getYouTubeApi
            }
            destroy(youTubeStream.id, youTubeApi) {
                destroying = false
            }
        }
    }

    val scheduledStartTime = youTubeStream.snippet.scheduledStartTime
    if (scheduledStartTime != null) {
        val date = runCatching {
            OffsetDateTime.parse(scheduledStartTime).toInstant()
        }.getOrNull()
        if (date != null) {
            Row(
                modifier = Modifier.padding(end = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StreamDescriptionView(
                    model = model,
                    stream = stream,
                    youTubeStream = youTubeStream,
                    ingests = ingests,
                    thumbnailUrl = youTubeStream.snippet.thumbnails.default.url,
                    startTime = date
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.width(50.dp)) {
                    HCenter {
                        if (destroying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = palette.gray,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    presentingConfirm = true
                                }
                            ) {
                                SystemImage(
                                    name = destroyImage,
                                    fontSize = 28.sp,
                                    tint = palette.red
                                )
                            }
                        }
                    }
                }
            }
            ConfirmationDialog(
                title = "",
                isPresented = presentingConfirm,
                onDismissRequest = { presentingConfirm = false },
            ) {
                Button(destroyText, role = ButtonRole.destructive) {
                    handleDestroy()
                }
            }
        }
    }
}

@Composable
private fun StreamsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    title: String,
    streams: List<YouTubeApiLiveBroadcast>,
    loadError: String?,
    ingests: List<YouTubeApiLiveStream>,
    destroyImage: String,
    destroyText: String,
    destroy: (String, YouTubeApi, () -> Unit) -> Unit,
) {
    Section(header = title) {
        streams.forEach { youTubeStream ->
            key(youTubeStream.id) {
                YouTubeStreamView(
                    model = model,
                    stream = stream,
                    youTubeStream = youTubeStream,
                    ingests = ingests,
                    destroyImage = destroyImage,
                    destroyText = destroyText,
                    destroy = destroy
                )
            }
        }
        if (streams.isEmpty()) {
            HCenter {
                if (loadError != null) {
                    Text(loadError)
                } else {
                    Text(localized("None"))
                }
            }
        }
    }
}

@Composable
private fun ScheduleStreamView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    schedulingStreamState: ScheduleStreamState,
    onSchedulingStreamStateChange: (ScheduleStreamState) -> Unit,
    loadStreams: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val palette = formPalette()

    fun idleSoon() {
        scope.launch {
            delay(5000)
            onSchedulingStreamStateChange(ScheduleStreamState.Idle)
        }
    }

    fun scheduleStreamSucceeded() {
        onSchedulingStreamStateChange(ScheduleStreamState.Succeeded)
        idleSoon()
    }

    fun scheduleStreamFailed(message: String) {
        onSchedulingStreamStateChange(ScheduleStreamState.Failed(message))
        idleSoon()
    }

    fun getLiveStream(liveStreams: YouTubeApiLiveStreamsListResponse): YouTubeApiLiveStream? {
        return liveStreams.items.firstOrNull {
            val ingestionInfo = it.cdn.ingestionInfo
            val url = "${ingestionInfo.ingestionAddress}/${ingestionInfo.streamName}"
            url == stream.url
        } ?: liveStreams.items.firstOrNull()
    }

    fun handleInsertLiveBroadcastResponse(
        youTubeApi: YouTubeApi,
        liveStream: YouTubeApiLiveStream,
        response: NetworkResponse<YouTubeApiLiveBroadcast>,
    ) {
        when (response) {
            is NetworkResponse.Success -> {
                val liveBroadcast = response.value
                youTubeApi.bindLiveBroadcast(
                    boardcastId = liveBroadcast.id,
                    streamId = liveStream.id
                ) { success ->
                    if (success) {
                        stream.youTubeVideoIds = liveBroadcast.id
                        model.youTubeVideoIdUpdated()
                        scheduleStreamSucceeded()
                        loadStreams()
                    } else {
                        scheduleStreamFailed(localized("Failed to bind live stream to broadcast"))
                    }
                }
            }

            NetworkResponse.AuthError -> scheduleStreamFailed(localized("Authentication failed"))
            NetworkResponse.Error -> scheduleStreamFailed(localized("Failed to create broadcast"))
        }
    }

    fun handleListLiveStreams(
        youTubeApi: YouTubeApi,
        response: NetworkResponse<YouTubeApiLiveStreamsListResponse>,
    ) {
        when (response) {
            is NetworkResponse.Success -> {
                val liveStream = getLiveStream(response.value)
                if (liveStream == null) {
                    scheduleStreamFailed(localized("No live stream found"))
                    return
                }
                youTubeApi.insertLiveBroadcast(
                    title = stream.youTubeScheduleStreamTitle,
                    visibility = stream.youTubeScheduleStreamVisibility,
                    autoStop = stream.youTubeScheduleStreamAutoStop
                ) { response2 ->
                    handleInsertLiveBroadcastResponse(
                        youTubeApi = youTubeApi,
                        liveStream = liveStream,
                        response = response2
                    )
                }
            }

            NetworkResponse.AuthError -> scheduleStreamFailed(localized("Authentication failed"))
            NetworkResponse.Error -> scheduleStreamFailed(localized("Failed to list live streams"))
        }
    }

    fun scheduleStream() {
        onSchedulingStreamStateChange(ScheduleStreamState.InProgress)
        model.getYouTubeApi(stream) { youTubeApi ->
            if (youTubeApi == null) {
                scheduleStreamFailed(localized("Failed to get access token"))
                return@getYouTubeApi
            }
            youTubeApi.listLiveStreams { response ->
                handleListLiveStreams(youTubeApi = youTubeApi, response = response)
            }
        }
    }

    Section(header = localized("Schedule")) {
        FormRow {
            Text(localized("Title"), color = palette.label)
            Spacer(modifier = Modifier.weight(1f))
            BasicTextField(
                value = stream.youTubeScheduleStreamTitle,
                onValueChange = { stream.youTubeScheduleStreamTitle = it },
                singleLine = true,
                textStyle = formBodyStyle.copy(color = palette.secondaryLabel),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier.widthIn(min = 100.dp)
            )
        }
        Picker(
            title = localized("Visibility"),
            selection = stream.youTubeScheduleStreamVisibility,
            options = YouTubeApiLiveBroadcaseVisibility.entries,
            text = { it.toString() }
        ) {
            stream.youTubeScheduleStreamVisibility = it
        }
        Toggle(
            title = localized("Auto-stop"),
            isOn = stream.youTubeScheduleStreamAutoStop
        ) {
            stream.youTubeScheduleStreamAutoStop = it
        }
        when (val state = schedulingStreamState) {
            ScheduleStreamState.Idle -> {
                FormButton(
                    title = localized("Create"),
                    centered = true,
                    enabled = stream.youTubeScheduleStreamTitle.isNotEmpty()
                ) {
                    scheduleStream()
                }
            }

            ScheduleStreamState.InProgress -> {
                HCenter {
                    Text(localized("Creating..."))
                }
            }

            ScheduleStreamState.Succeeded -> {
                HCenter {
                    Text(localized("Created"))
                }
            }

            is ScheduleStreamState.Failed -> {
                HCenter {
                    Text(state.message, color = palette.red)
                }
            }
        }
    }
}

@Composable
fun StreamYouTubeScheduleStreamView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
) {
    var schedulingStreamState by remember { mutableStateOf<ScheduleStreamState>(ScheduleStreamState.Idle) }
    var presenting by remember { mutableStateOf(false) }
    var liveStreams by remember { mutableStateOf<List<YouTubeApiLiveBroadcast>>(emptyList()) }
    var liveStreamsLoadError by remember { mutableStateOf<String?>(null) }
    var upcomingStreams by remember { mutableStateOf<List<YouTubeApiLiveBroadcast>>(emptyList()) }
    var upcomingStreamsLoadError by remember { mutableStateOf<String?>(null) }
    var ingests by remember { mutableStateOf<List<YouTubeApiLiveStream>>(emptyList()) }

    fun loadActiveStreams() {
        liveStreamsLoadError = null
        model.getYouTubeApi(stream) { youTubeApi ->
            youTubeApi?.listLiveBroadcasts(status = "active") { response ->
                when (response) {
                    is NetworkResponse.Success -> liveStreams = response.value.items
                    NetworkResponse.AuthError -> liveStreamsLoadError = localized("Error")
                    NetworkResponse.Error -> liveStreamsLoadError = localized("Error")
                }
            }
        }
    }

    fun loadUpcomingStreams() {
        upcomingStreamsLoadError = null
        model.getYouTubeApi(stream) { youTubeApi ->
            youTubeApi?.listLiveBroadcasts(status = "upcoming") { response ->
                when (response) {
                    is NetworkResponse.Success -> upcomingStreams = response.value.items.filter {
                        it.snippet.scheduledStartTime != null
                    }

                    NetworkResponse.AuthError -> upcomingStreamsLoadError = localized("Error")
                    NetworkResponse.Error -> upcomingStreamsLoadError = localized("Error")
                }
            }
        }
    }

    fun loadIngests() {
        model.getYouTubeApi(stream) { youTubeApi ->
            youTubeApi?.listLiveStreams { response ->
                when (response) {
                    is NetworkResponse.Success -> ingests = response.value.items
                    NetworkResponse.AuthError -> ingests = emptyList()
                    NetworkResponse.Error -> ingests = emptyList()
                }
            }
        }
    }

    fun loadStreams() {
        loadActiveStreams()
        loadUpcomingStreams()
        loadIngests()
    }

    fun stopLiveStream(id: String, youTubeApi: YouTubeApi, onCompleted: () -> Unit) {
        youTubeApi.transitionLiveBroadcast(id = id, status = "complete") { success ->
            if (success) {
                liveStreams = liveStreams.filterNot { it.id == id }
            }
            onCompleted()
        }
    }

    fun deleteUpcomingStream(id: String, youTubeApi: YouTubeApi, onCompleted: () -> Unit) {
        youTubeApi.deleteLiveBroadcast(id = id) { result ->
            when (result) {
                is NetworkResponse.Success -> upcomingStreams = upcomingStreams.filterNot { it.id == id }
                else -> Unit
            }
            onCompleted()
        }
    }

    FormButton(
        title = localized("Manage streams"),
        centered = true,
        enabled = stream.isYouTubeAuthorized()
    ) {
        presenting = true
    }
    if (presenting) {
        Sheet(onDismissRequest = { presenting = false }) {
            Form(
                title = localized("Manage streams"),
                toolbar = {
                    CloseToolbar(
                        presenting = presenting,
                        onPresentingChange = { presenting = it }
                    )
                }
            ) {
                ScheduleStreamView(
                    model = model,
                    stream = stream,
                    schedulingStreamState = schedulingStreamState,
                    onSchedulingStreamStateChange = { schedulingStreamState = it },
                    loadStreams = { loadStreams() }
                )
                StreamsView(
                    model = model,
                    stream = stream,
                    title = localized("Live"),
                    streams = liveStreams,
                    loadError = liveStreamsLoadError,
                    ingests = ingests,
                    destroyImage = "stop",
                    destroyText = localized("End"),
                    destroy = { id, youTubeApi, onCompleted ->
                        stopLiveStream(id, youTubeApi, onCompleted)
                    }
                )
                StreamsView(
                    model = model,
                    stream = stream,
                    title = localized("Upcoming"),
                    streams = upcomingStreams,
                    loadError = upcomingStreamsLoadError,
                    ingests = ingests,
                    destroyImage = "trash",
                    destroyText = localized("Delete"),
                    destroy = { id, youTubeApi, onCompleted ->
                        deleteUpcomingStream(id, youTubeApi, onCompleted)
                    }
                )
                LaunchedEffect(Unit) {
                    schedulingStreamState = ScheduleStreamState.Idle
                    loadStreams()
                }
            }
        }
    }
}

@Composable
fun StreamYouTubeSettingsView(
    model: Model = LocalModel.current,
    debug: SettingsDebug,
    stream: SettingsStream,
) {
    val scope = rememberCoroutineScope()
    val palette = formPalette()
    val authState = stream.youTubeAuthState

    fun submitVideoIds(value: String) {
        stream.youTubeVideoIds = value.filterNot { it.isWhitespace() }
        if (stream.enabled) {
            model.youTubeVideoIdUpdated()
        }
    }

    fun submitHandle(value: String) {
        stream.youTubeHandle = value
    }

    fun fetchChannelHandle() {
        model.getYouTubeApi(stream) { youTubeApi ->
            youTubeApi?.listChannels { response ->
                when (response) {
                    is NetworkResponse.Success -> {
                        val handle = response.value.items.firstOrNull()?.snippet?.customUrl
                        if (handle != null) {
                            stream.youTubeHandle = handle
                        }
                    }

                    NetworkResponse.AuthError, NetworkResponse.Error -> Unit
                }
            }
        }
    }

    fun showFailedToFetchVideoIdsToast() {
        model.makeErrorToast(
            title = localized("Failed to fetch YouTube Video IDs"),
            subTitle = localized("You must be live on YouTube for this to work.")
        )
    }

    Form(title = localized("YouTube")) {
        Section {
            if (!stream.isYouTubeAuthorized()) {
                FormButton(
                    title = localized("Login"),
                    centered = true
                ) {
                    model.youTubeSignIn(stream)
                }
            } else {
                FormButton(
                    title = localized("Logout"),
                    centered = true
                ) {
                    model.youTubeSignOut(stream)
                }
            }
        }
        Section(footer = localized("Schedule a stream before going live.")) {
            StreamYouTubeScheduleStreamView(model = model, stream = stream)
        }
        Section(footer = localized("The Video ID unique for every live stream.")) {
            TextEditNavigationView(
                title = localized("Channel handle"),
                value = stream.youTubeHandle,
                onSubmit = { submitHandle(it) },
                placeholder = "@erimo144"
            )
            TextEditNavigationView(
                title = localized("Video IDs"),
                value = stream.youTubeVideoIds,
                onSubmit = { submitVideoIds(it) },
                placeholder = "FekKCUN5W8U"
            )
            FormButton(
                title = localized("Fetch Video IDs"),
                centered = true,
                enabled = stream.isYouTubeAuthorized() || stream.youTubeHandle.isNotEmpty()
            ) {
                if (stream.isYouTubeAuthorized()) {
                    model.getYouTubeApi(stream) { youTubeApi ->
                        if (youTubeApi == null) {
                            showFailedToFetchVideoIdsToast()
                            return@getYouTubeApi
                        }
                        youTubeApi.listLiveBroadcasts(status = "active") { response ->
                            when (response) {
                                is NetworkResponse.Success -> {
                                    val videoIds = response.value.items.map { it.id }
                                    if (videoIds.isEmpty()) {
                                        showFailedToFetchVideoIdsToast()
                                    } else {
                                        submitVideoIds(videoIds.joinToString(","))
                                    }
                                }

                                else -> showFailedToFetchVideoIdsToast()
                            }
                        }
                    }
                } else {
                    scope.launch {
                        runCatching {
                            fetchYouTubeVideoId(handle = stream.youTubeHandle)
                        }.onSuccess { videoId ->
                            submitVideoIds(videoId)
                        }.onFailure {
                            showFailedToFetchVideoIdsToast()
                        }
                    }
                }
            }
        }
    }
    var previousAuthState by remember { mutableStateOf(authState) }
    LaunchedEffect(authState) {
        if (authState != previousAuthState) {
            previousAuthState = authState
            if (authState != null) {
                fetchChannelHandle()
            }
        }
    }
}
