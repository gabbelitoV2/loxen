package com.moblin.android.view.controlbar.quickbutton.chat.moderation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.twitch.TwitchApiChannel
import com.moblin.android.streamingplatforms.twitch.TwitchApiPollData
import com.moblin.android.streamingplatforms.twitch.TwitchApiPollStatus
import com.moblin.android.streamingplatforms.twitch.TwitchApiPredictionData
import com.moblin.android.streamingplatforms.twitch.TwitchApiPredictionOutcome
import com.moblin.android.streamingplatforms.twitch.TwitchApiPredictionStatus
import com.moblin.android.streamingplatforms.twitch.TwitchApiStreamData
import com.moblin.android.various.model.Model
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.settings.SettingsStreamTwitchRaidChannel
import com.moblin.android.view.controlbar.quickbutton.chat.ActionRowView
import com.moblin.android.view.controlbar.quickbutton.chat.EmotesOnlyView
import com.moblin.android.view.controlbar.quickbutton.chat.Executor
import com.moblin.android.view.controlbar.quickbutton.chat.ExecutorView
import com.moblin.android.view.controlbar.quickbutton.chat.FollowersOnlyView
import com.moblin.android.view.controlbar.quickbutton.chat.ModActionType
import com.moblin.android.view.controlbar.quickbutton.chat.NavigationLinkView
import com.moblin.android.view.controlbar.quickbutton.chat.PollOption
import com.moblin.android.view.controlbar.quickbutton.chat.PollOptionsSectionView
import com.moblin.android.view.controlbar.quickbutton.chat.RaidChannelView
import com.moblin.android.view.controlbar.quickbutton.chat.SlowModeView
import com.moblin.android.view.controlbar.quickbutton.chat.SubscribersOnlyView
import com.moblin.android.view.controlbar.quickbutton.chat.UserModerationItemView
import com.moblin.android.view.controlbar.quickbutton.chat.canCreatePoll
import com.moblin.android.view.controlbar.quickbutton.chat.pollOptionTitles
import com.moblin.android.view.settings.streams.stream.TwitchLogoAndNameView
import com.moblin.android.view.utils.BorderlessButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.TextButtonView

@Composable
private fun CreatePollView(model: Model, onCreated: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf(PollOption(), PollOption())) }
    var duration by remember { mutableStateOf(60) }
    val executor = remember { Executor() }

    Column {
        Text("Title", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        PollOptionsSectionView(
            header = "Choices",
            placeholder = "Choice",
            kind = localized("a choice"),
            options = options,
            onOptionsChange = { options = it },
            maxCount = 5,
        )
        PickerRow(
            title = "Duration",
            values = listOf(30, 60, 120, 180, 300, 600),
            selected = duration,
            optionText = { formatShortDuration(seconds = it) },
            onSelected = { duration = it },
        )
        HCenter {
            ExecutorView(executor = executor) {
                CreateButtonView(
                    enabled = canCreatePoll(title = title.trim(), options = options),
                ) {
                    executor.startProgress()
                    model.createTwitchPoll(
                        title = title.trim(),
                        choices = pollOptionTitles(options),
                        duration = duration,
                    ) { result ->
                        executor.completed(result)
                        if (result.isSuccessful()) {
                            onCreated()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePollView(model: Model, poll: TwitchApiPollData, onEnded: () -> Unit) {
    fun end(status: TwitchApiPollStatus, onComplete: (OperationResult) -> Unit) {
        model.endTwitchPoll(id = poll.id, status = status) { result ->
            onComplete(result)
            if (result.isSuccessful()) {
                onEnded()
            }
        }
    }

    Column {
        Text("Title", style = MaterialTheme.typography.titleMedium)
        Text(poll.title)
        Text("Choices", style = MaterialTheme.typography.titleMedium)
        poll.choices.forEach { choice ->
            Row {
                Text(choice.title)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        ActionRowView(text = "End poll", image = "stop") { onComplete ->
            end(status = TwitchApiPollStatus.terminated, onComplete = onComplete)
        }
        ActionRowView(text = "Archive poll", image = "archivebox") { onComplete ->
            end(status = TwitchApiPollStatus.archived, onComplete = onComplete)
        }
        Text("Ending the poll shows the final results. Archiving it hides them.")
    }
}

@Composable
private fun PollFormView(model: Model) {
    var loaded by remember { mutableStateOf(false) }
    var poll by remember { mutableStateOf<TwitchApiPollData?>(null) }
    val executor = remember { Executor() }

    fun load() {
        executor.startProgress()
        model.getTwitchPolls { result ->
            when (result) {
                is OperationResult.Success -> {
                    poll = result.value.firstOrNull { it.isActive() }
                    executor.completedNoTimer(result = OperationResult.Success(ByteArray(0)))
                }
                OperationResult.AuthError -> {
                    executor.completedNoTimer(result = OperationResult.AuthError)
                }
                OperationResult.Error -> {
                    executor.completedNoTimer(result = OperationResult.Error)
                }
            }
        }
    }

    fun loadOnce() {
        if (loaded) {
            return
        }
        loaded = true
        load()
    }

    ExecutorView(executor = executor, centerNonContent = true) {
        val currentPoll = poll
        if (currentPoll != null) {
            ActivePollView(model = model, poll = currentPoll, onEnded = { load() })
        } else {
            CreatePollView(model = model, onCreated = { load() })
        }
    }
    LaunchedEffect(Unit) {
        loadOnce()
    }
}

@Composable
private fun PollView(model: Model) {
    NavigationLinkView(text = "Poll", image = "chart.bar") {
        PollFormView(model = model)
    }
}

@Composable
private fun CreatePredictionView(model: Model, onCreated: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var outcomes by remember { mutableStateOf(listOf(PollOption(), PollOption())) }
    var predictionWindow by remember { mutableStateOf(300) }
    val executor = remember { Executor() }

    Column {
        Text("Title", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        PollOptionsSectionView(
            header = "Outcomes",
            placeholder = "Outcome",
            kind = localized("an outcome"),
            options = outcomes,
            onOptionsChange = { outcomes = it },
            maxCount = 10,
        )
        PickerRow(
            title = "Duration",
            values = listOf(60, 300, 600, 1800),
            selected = predictionWindow,
            optionText = { formatShortDuration(seconds = it) },
            onSelected = { predictionWindow = it },
        )
        HCenter {
            ExecutorView(executor = executor) {
                CreateButtonView(
                    enabled = canCreatePoll(title = title.trim(), options = outcomes),
                ) {
                    executor.startProgress()
                    model.createTwitchPrediction(
                        title = title.trim(),
                        outcomes = pollOptionTitles(outcomes),
                        predictionWindow = predictionWindow,
                    ) { result ->
                        executor.completed(result)
                        if (result.isSuccessful()) {
                            onCreated()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionOutcomeView(
    outcome: TwitchApiPredictionOutcome,
    action: ((OperationResult) -> Unit) -> Unit,
) {
    val executor = remember { Executor() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(outcome.title)
        Spacer(modifier = Modifier.weight(1f))
        ExecutorView(executor = executor) {
            BorderlessButtonView(text = "Resolve") {
                executor.startProgress()
                action { result -> executor.completed(result) }
            }
        }
    }
}

@Composable
private fun ActivePredictionView(
    model: Model,
    prediction: TwitchApiPredictionData,
    onEnded: () -> Unit,
) {
    fun end(
        status: TwitchApiPredictionStatus,
        winningOutcomeId: String? = null,
        onComplete: (OperationResult) -> Unit,
    ) {
        model.endTwitchPrediction(
            id = prediction.id,
            status = status,
            winningOutcomeId = winningOutcomeId,
        ) { result ->
            onComplete(result)
            if (result.isSuccessful()) {
                onEnded()
            }
        }
    }

    Column {
        Text("Title", style = MaterialTheme.typography.titleMedium)
        Text(prediction.title)
        Text("Outcomes", style = MaterialTheme.typography.titleMedium)
        prediction.outcomes.forEach { outcome ->
            PredictionOutcomeView(outcome = outcome) { onComplete ->
                end(
                    status = TwitchApiPredictionStatus.resolved,
                    winningOutcomeId = outcome.id,
                    onComplete = onComplete,
                )
            }
        }
        Text("Resolve the prediction by selecting the winning outcome.")
        if (prediction.isActive()) {
            ActionRowView(text = "Lock prediction", image = "lock") { onComplete ->
                end(status = TwitchApiPredictionStatus.locked, onComplete = onComplete)
            }
        }
        ActionRowView(text = "Cancel prediction", image = "xmark") { onComplete ->
            end(status = TwitchApiPredictionStatus.canceled, onComplete = onComplete)
        }
        Text("Cancelling the prediction refunds all channel points.")
    }
}

@Composable
private fun PredictionFormView(model: Model) {
    var loaded by remember { mutableStateOf(false) }
    var prediction by remember { mutableStateOf<TwitchApiPredictionData?>(null) }
    val executor = remember { Executor() }

    fun load() {
        executor.startProgress()
        model.getTwitchPredictions { result ->
            when (result) {
                is OperationResult.Success -> {
                    prediction = result.value.firstOrNull { it.isActive() || it.isLocked() }
                    executor.completedNoTimer(result = OperationResult.Success(ByteArray(0)))
                }
                OperationResult.AuthError -> {
                    executor.completedNoTimer(result = OperationResult.AuthError)
                }
                OperationResult.Error -> {
                    executor.completedNoTimer(result = OperationResult.Error)
                }
            }
        }
    }

    fun loadOnce() {
        if (loaded) {
            return
        }
        loaded = true
        load()
    }

    ExecutorView(executor = executor, centerNonContent = true) {
        val currentPrediction = prediction
        if (currentPrediction != null) {
            ActivePredictionView(
                model = model,
                prediction = currentPrediction,
                onEnded = { load() },
            )
        } else {
            CreatePredictionView(model = model, onCreated = { load() })
        }
    }
    LaunchedEffect(Unit) {
        loadOnce()
    }
}

@Composable
private fun PredictionView(model: Model) {
    NavigationLinkView(text = "Prediction", image = "sparkles") {
        PredictionFormView(model = model)
    }
}

@Composable
private fun RaidChannelSearchView(model: Model) {
    var searchText by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<TwitchApiChannel>>(emptyList()) }
    val executor = remember { Executor() }

    LaunchedEffect(searchText) {
        if (searchText.isEmpty()) {
            channels = emptyList()
            return@LaunchedEffect
        }
        executor.startProgress()
        model.searchTwitchChannels(stream = model.stream, filter = searchText) { result ->
            when (result) {
                is OperationResult.Success -> {
                    channels = result.value.sortedWith { first, second ->
                        val search = searchText.lowercase()
                        val firstName = first.display_name.lowercase()
                        val secondName = second.display_name.lowercase()
                        when {
                            firstName.startsWith(search) -> -1
                            secondName.startsWith(search) -> 1
                            else -> 0
                        }
                    }
                    executor.completedNoTimer(result = OperationResult.Success(ByteArray(0)))
                }
                OperationResult.AuthError -> {
                    executor.completedNoTimer(result = OperationResult.AuthError)
                }
                OperationResult.Error -> {
                    executor.completedNoTimer(result = OperationResult.Error)
                }
            }
        }
    }

    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        ExecutorView(executor = executor, centerNonContent = true) {
            Column {
                channels.forEach { channel ->
                    RaidChannelView(
                        buttonText = "Raid",
                        channel = channel.display_name,
                        category = channel.game_name,
                        title = channel.title,
                        image = channel.thumbnail_url,
                        isLive = true,
                        viewerCount = null,
                    ) { onComplete ->
                        model.startRaidTwitchChannel(
                            channelId = channel.id,
                            onComplete = onComplete,
                        )
                    }
                }
            }
        }
    }
}

private data class RaidSuggestion(
    val id: String,
    val name: String,
    val category: String,
    val title: String,
    val viewerCount: Int,
    var image: String? = null,
)

private fun makeRaidSuggestions(streams: List<TwitchApiStreamData>): List<RaidSuggestion> {
    return streams.map {
        RaidSuggestion(
            id = it.user_id,
            name = it.user_name,
            category = it.game_name,
            title = it.title,
            viewerCount = it.viewer_count,
        )
    }
}

private fun makeRaidSuggestions(
    streams: List<TwitchApiStreamData>,
    channels: List<SettingsStreamTwitchRaidChannel>,
): List<RaidSuggestion> {
    val suggestions = mutableMapOf<String, RaidSuggestion>()
    for (suggestion in makeRaidSuggestions(streams = streams)) {
        suggestions[suggestion.id] = suggestion
    }
    return channels.mapNotNull { suggestions[it.channelId] }
}

@Composable
private fun RaidSuggestionsView(model: Model, suggestions: List<RaidSuggestion>) {
    Column {
        suggestions.forEach { suggestion ->
            RaidChannelView(
                buttonText = "Raid",
                channel = suggestion.name,
                category = suggestion.category,
                title = suggestion.title,
                image = suggestion.image,
                isLive = true,
                viewerCount = suggestion.viewerCount,
            ) { onComplete ->
                model.startRaidTwitchChannel(
                    channelId = suggestion.id,
                    onComplete = onComplete,
                )
            }
        }
    }
}

@Composable
private fun RaidHistoryView(model: Model, title: String, suggestions: List<RaidSuggestion>) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        RaidSuggestionsView(model = model, suggestions = suggestions)
    }
}

private fun fetchRaidSuggestionImages(
    model: Model,
    userIds: List<String>,
    onComplete: (Map<String, String>) -> Unit,
) {
    model.getTwitchUsers(
        stream = model.stream,
        userIds = userIds.toSet().toList(),
    ) { users ->
        if (users == null) {
            return@getTwitchUsers
        }
        val images = mutableMapOf<String, String>()
        for (user in users) {
            images[user.id] = user.profile_image_url
        }
        onComplete(images)
    }
}

private fun setRaidSuggestionImages(
    suggestions: List<RaidSuggestion>,
    images: Map<String, String>,
): List<RaidSuggestion> {
    return suggestions.map {
        it.copy(image = images[it.id])
    }
}

@Composable
private fun RaidFollowedChannelsView(
    model: Model,
    suggestions: List<RaidSuggestion>,
    executor: Executor,
) {
    Column {
        Text("Followed channels", style = MaterialTheme.typography.titleMedium)
        ExecutorView(executor = executor, centerNonContent = true) {
            RaidSuggestionsView(model = model, suggestions = suggestions)
        }
    }
}

@Composable
private fun RunCommercialView(model: Model) {
    var duration by remember { mutableStateOf(30) }
    val executor = remember { Executor() }

    NavigationLinkView(text = "Run commercial", image = "cup.and.saucer") {
        Column {
            Text("Duration", style = MaterialTheme.typography.titleMedium)
            PickerRow(
                title = null,
                values = listOf(30, 60, 90, 120, 180),
                selected = duration,
                optionText = { formatShortDuration(seconds = it) },
                onSelected = { duration = it },
            )
            HCenter {
                ExecutorView(executor = executor) {
                    TextButtonView("Run commercial") {
                        executor.startProgress()
                        model.startAds(
                            seconds = duration,
                            onComplete = { result -> executor.completed(result) },
                        )
                    }
                }
            }
        }
    }
}

private enum class AnnouncementColor(val rawValue: String) {
    primary("primary"),
    blue("blue"),
    green("green"),
    orange("orange"),
    purple("purple");

    override fun toString(): String {
        return when (this) {
            primary -> localized("Primary")
            blue -> "🔵"
            green -> "🟢"
            orange -> "🟠"
            purple -> "🟣"
        }
    }
}

@Composable
private fun SendAnnouncementView(model: Model) {
    var message by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(AnnouncementColor.primary) }
    val executor = remember { Executor() }

    fun canSend(): Boolean {
        return message.trim().isNotEmpty()
    }

    NavigationLinkView(text = "Send announcement", image = "megaphone") {
        Column {
            Text("Message", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
            )
            PickerRow(
                title = "Color",
                values = AnnouncementColor.entries,
                selected = color,
                optionText = { it.toString() },
                onSelected = { color = it },
            )
            HCenter {
                ExecutorView(executor = executor) {
                    TextButtonView("Send", enabled = canSend()) {
                        executor.startProgress()
                        model.sendTwitchAnnouncement(
                            message = message.trim(),
                            color = color.rawValue,
                            onComplete = { result -> executor.completed(result) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartRaidView(model: Model) {
    var raidsSent by remember { mutableStateOf<List<RaidSuggestion>>(emptyList()) }
    var raidsReceived by remember { mutableStateOf<List<RaidSuggestion>>(emptyList()) }
    var followedChannels by remember { mutableStateOf<List<RaidSuggestion>>(emptyList()) }
    val followedChannelsExecutor = remember { Executor() }

    fun loadRaidHistory() {
        val sentChannels = model.stream.twitchRaidsSent
        val receivedChannels = model.stream.twitchRaidsReceived
        val userIds = (
            sentChannels.map { it.channelId } + receivedChannels.map { it.channelId }
            ).toSet()
        model.getTwitchStreams(
            stream = model.stream,
            userIds = userIds.toList(),
            live = true,
        ) { streams ->
            if (streams == null) {
                return@getTwitchStreams
            }
            raidsSent = makeRaidSuggestions(streams = streams, channels = sentChannels)
            raidsReceived = makeRaidSuggestions(streams = streams, channels = receivedChannels)
            fetchRaidSuggestionImages(
                model = model,
                userIds = raidsSent.map { it.id } + raidsReceived.map { it.id },
            ) { images ->
                raidsSent = setRaidSuggestionImages(raidsSent, images = images)
                raidsReceived = setRaidSuggestionImages(raidsReceived, images = images)
            }
        }
    }

    fun loadFollowedChannels() {
        followedChannelsExecutor.startProgress()
        model.getTwitchFollowedStreams(stream = model.stream) { result ->
            when (result) {
                is OperationResult.Success -> {
                    followedChannels = makeRaidSuggestions(streams = result.value)
                    followedChannelsExecutor.completedNoTimer(
                        result = OperationResult.Success(ByteArray(0)),
                    )
                    fetchRaidSuggestionImages(
                        model = model,
                        userIds = followedChannels.map { it.id },
                    ) { images ->
                        followedChannels =
                            setRaidSuggestionImages(followedChannels, images = images)
                    }
                }
                OperationResult.AuthError -> {
                    followedChannelsExecutor.completedNoTimer(result = OperationResult.AuthError)
                }
                OperationResult.Error -> {
                    followedChannelsExecutor.completedNoTimer(result = OperationResult.Error)
                }
            }
        }
    }

    NavigationLinkView(text = "Raid channel", image = "play.tv") {
        Column {
            RaidChannelSearchView(model = model)
            RaidHistoryView(
                model = model,
                title = "Raided before",
                suggestions = raidsSent,
            )
            RaidHistoryView(
                model = model,
                title = "Raided you",
                suggestions = raidsReceived,
            )
            RaidFollowedChannelsView(
                model = model,
                suggestions = followedChannels,
                executor = followedChannelsExecutor,
            )
        }
    }
    LaunchedEffect(Unit) {
        loadRaidHistory()
        loadFollowedChannels()
    }
}

@Composable
fun QuickButtonChatModerationTwitchView(
    model: Model,
    platform: Platform?,
    onPlatformChange: (Platform?) -> Unit,
    onNavigate: (String) -> Unit,
) {
    Box(modifier = Modifier.clickable { onNavigate("QuickButtonChatModerationTwitch") }) {
        TwitchLogoAndNameView()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonChatModerationTwitchForm(
    model: Model,
    onPlatformChange: (Platform?) -> Unit,
) {
    fun slowModeAction(duration: Int?, onComplete: (OperationResult) -> Unit) {
        model.setTwitchSlowMode(
            enabled = duration != null,
            duration = duration,
            onComplete = onComplete,
        )
    }

    fun followersOnlyAction(duration: Int?, onComplete: (OperationResult) -> Unit) {
        model.setTwitchFollowersMode(
            enabled = duration != null,
            duration = (duration ?: 0) / 60,
            onComplete = onComplete,
        )
    }

    LaunchedEffect(Unit) {
        onPlatformChange(Platform.twitch)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Twitch") })
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item {
                Column {
                    StartRaidView(model = model)
                    RunCommercialView(model = model)
                    SendAnnouncementView(model = model)
                    PollView(model = model)
                    PredictionView(model = model)
                }
            }
            item {
                Column {
                    SlowModeView(
                        durations = listOf(3, 5, 10, 30, 60, 120),
                        action = { duration, onComplete ->
                            slowModeAction(duration = duration, onComplete = onComplete)
                        },
                    )
                    FollowersOnlyView(
                        durations = listOf(60, 300, 600, 3600),
                        action = { duration, onComplete ->
                            followersOnlyAction(duration = duration, onComplete = onComplete)
                        },
                    )
                    SubscribersOnlyView(
                        action = { onComplete -> model.setTwitchSubscribersOnlyMode(onComplete) },
                    )
                    EmotesOnlyView(
                        action = { onComplete -> model.setTwitchEmoteOnlyMode(onComplete) },
                    )
                }
            }
            item {
                Column {
                    ModActionType.entries.forEach { action ->
                        UserModerationItemView(
                            model = model,
                            action = action,
                            platform = Platform.twitch,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> PickerRow(
    title: String?,
    values: List<T>,
    selected: T,
    optionText: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (title != null) {
            Text(title, modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            ) {
                Text(optionText(selected))
            }
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                values.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(optionText(value)) },
                        onClick = {
                            onSelected(value)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
