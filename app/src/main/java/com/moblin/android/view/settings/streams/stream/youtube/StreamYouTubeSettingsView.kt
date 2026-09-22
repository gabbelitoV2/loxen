package com.moblin.android.view.settings.streams.stream.youtube

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.R
import com.moblin.android.common.various.isValidRtmpUrl
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.youtube.YouTubeApi
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveBroadcast
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveBroadcaseVisibility
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveStream
import com.moblin.android.streamingplatforms.youtube.YouTubeApiLiveStreamsListResponse
import com.moblin.android.streamingplatforms.youtube.fetchYouTubeVideoId
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.getYouTubeApi
import com.moblin.android.various.model.youTubeSignIn
import com.moblin.android.various.model.youTubeSignOut
import com.moblin.android.various.model.youTubeVideoIdUpdated
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.settings.SettingsDebug
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel

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
                            .size(50.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                },
                placeholder = {
                }
            )
            Column {
                Text(youTubeStream.snippet.title)
                Text(
                    startTimeFormatter.format(startTime),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(details(), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (stream.url != ingestsUrl || !stream.getYouTubeVideoIds().contains(youTubeStream.id)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⚠️ Moblin is not configured to stream to this stream.",
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(
                    onClick = { presentingConfigureConfirm = true },
                    enabled = !(isLive || isRecording)
                ) {
                    Text(localized("Configure"))
                }
            }
        }
    }
    if (presentingConfigureConfirm) {
        AlertDialog(
            onDismissRequest = { presentingConfigureConfirm = false },
            title = { Text(localized("Overwrite Settings → Streams → ${stream.name} → URL?")) },
            confirmButton = {
                TextButton(onClick = {
                    if (isValidRtmpUrl(url = ingestsUrl, rtmpStreamKeyRequired = true) == null) {
                        stream.url = ingestsUrl
                        stream.youTubeVideoIds = youTubeStream.id
                        Unit
                    }
                    presentingConfigureConfirm = false
                }) {
                    Text(localized("Yes"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    stream.youTubeVideoIds = youTubeStream.id
                    model.youTubeVideoIdUpdated()
                    presentingConfigureConfirm = false
                }) {
                    Text(localized("No"))
                }
            }
        )
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
    val thumbnailUrl = youTubeStream.snippet.thumbnails.default.url
    if (scheduledStartTime != null && thumbnailUrl != null) {
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
                    thumbnailUrl = thumbnailUrl,
                    startTime = date
                )
                Spacer(modifier = Modifier.weight(1f))
                HCenter {
                    if (destroying) {
                        CircularProgressIndicator()
                    } else {
                        IconButton(onClick = { presentingConfirm = true }) {
                            Icon(
                                imageVector = when (destroyImage) {
                                    "trash" -> Icons.Default.Delete
                                    else -> Icons.Default.Close
                                },
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
            if (presentingConfirm) {
                AlertDialog(
                    onDismissRequest = { presentingConfirm = false },
                    confirmButton = {
                        TextButton(onClick = {
                            presentingConfirm = false
                            handleDestroy()
                        }) {
                            Text(destroyText)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { presentingConfirm = false }) {
                            Text(localized("Cancel"))
                        }
                    }
                )
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        streams.forEach { youTubeStream ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleStreamView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    schedulingStreamState: ScheduleStreamState,
    onSchedulingStreamStateChange: (ScheduleStreamState) -> Unit,
    loadStreams: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var visibilityExpanded by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(localized("Schedule"), style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = stream.youTubeScheduleStreamTitle,
            onValueChange = { stream.youTubeScheduleStreamTitle = it },
            label = { Text(localized("Title")) },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(
            expanded = visibilityExpanded,
            onExpandedChange = { visibilityExpanded = it }
        ) {
            OutlinedTextField(
                value = stream.youTubeScheduleStreamVisibility.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text(localized("Visibility")) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = visibilityExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = visibilityExpanded,
                onDismissRequest = { visibilityExpanded = false }
            ) {
                YouTubeApiLiveBroadcaseVisibility.entries.forEach { visibility ->
                    DropdownMenuItem(
                        text = { Text(visibility.toString()) },
                        onClick = {
                            stream.youTubeScheduleStreamVisibility = visibility
                            visibilityExpanded = false
                        }
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Auto-stop"))
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = stream.youTubeScheduleStreamAutoStop,
                onCheckedChange = { stream.youTubeScheduleStreamAutoStop = it }
            )
        }
        when (val state = schedulingStreamState) {
            ScheduleStreamState.Idle -> {
                TextButtonView(
                    title = localized("Create"),
                    action = { scheduleStream() }
                )
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
                    Text(state.message, color = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    TextButtonView(
        title = localized("Manage streams"),
        action = { presenting = true }
    )
    if (presenting) {
        ModalBottomSheet(onDismissRequest = { presenting = false }) {
            TopAppBar(
                title = { Text(localized("Manage streams")) },
                actions = {
                    CloseToolbar(
                        presenting = presenting,
                        onPresentingChange = { presenting = it }
                    )
                }
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    ScheduleStreamView(
                        model = model,
                        stream = stream,
                        schedulingStreamState = schedulingStreamState,
                        onSchedulingStreamStateChange = { schedulingStreamState = it },
                        loadStreams = { loadStreams() }
                    )
                }
                item {
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
                }
                item {
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
                }
            }
            LaunchedEffect(Unit) {
                schedulingStreamState = ScheduleStreamState.Idle
                loadStreams()
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

    fun tokenExpiresIn(): Duration? {
        val expirationDate: Instant = TODO("stream.youTubeAuthState?.lastTokenResponse?.accessTokenExpirationDate")
        return Duration.ofSeconds(
            maxOf(Duration.between(Instant.now(), expirationDate).seconds, 0)
        )
    }

    fun showFailedToFetchVideoIdsToast() {
        model.makeErrorToast(
            title = localized("Failed to fetch YouTube Video IDs"),
            subTitle = localized("You must be live on YouTube for this to work.")
        )
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            Column {
                if (!stream.isYouTubeAuthorized()) {
                    TextButtonView(
                        title = localized("Login"),
                        action = { model.youTubeSignIn(stream) }
                    )
                } else {
                    TextButtonView(
                        title = localized("Logout"),
                        action = { model.youTubeSignOut(stream) }
                    )
                }
            }
        }
        item {
            Column {
                StreamYouTubeScheduleStreamView(model = model, stream = stream)
                Text(
                    localized("Schedule a stream before going live."),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            Column {
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
                TextButtonView(
                    title = localized("Fetch Video IDs"),
                    action = {
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
                )
                Text(
                    localized("The Video ID unique for every live stream."),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    LaunchedEffect(authState) {
        if (authState != null) {
            fetchChannelHandle()
        }
    }
}
