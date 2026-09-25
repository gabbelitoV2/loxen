package com.moblin.android.view.controlbar.quickbutton.chat.moderation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.twitch.TwitchApiChannel
import com.moblin.android.streamingplatforms.twitch.TwitchApiPollData
import com.moblin.android.streamingplatforms.twitch.TwitchApiPollStatus
import com.moblin.android.streamingplatforms.twitch.TwitchApiPredictionData
import com.moblin.android.streamingplatforms.twitch.TwitchApiPredictionOutcome
import com.moblin.android.streamingplatforms.twitch.TwitchApiPredictionStatus
import com.moblin.android.streamingplatforms.twitch.TwitchApiStreamData
import com.moblin.android.various.model.Model
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.settings.SettingsStreamTwitchRaidChannel
import com.moblin.android.various.utils.sortedBySearchPrefix
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
import com.moblin.android.common.various.isSuccessful
import com.moblin.android.various.model.createTwitchPoll
import com.moblin.android.various.model.createTwitchPrediction
import com.moblin.android.various.model.endTwitchPoll
import com.moblin.android.various.model.endTwitchPrediction
import com.moblin.android.various.model.getTwitchFollowedStreams
import com.moblin.android.various.model.getTwitchPolls
import com.moblin.android.various.model.getTwitchPredictions
import com.moblin.android.various.model.getTwitchStreams
import com.moblin.android.various.model.getTwitchUsers
import com.moblin.android.various.model.searchTwitchChannels
import com.moblin.android.various.model.sendTwitchAnnouncement
import com.moblin.android.various.model.setTwitchEmoteOnlyMode
import com.moblin.android.various.model.setTwitchFollowersMode
import com.moblin.android.various.model.setTwitchSlowMode
import com.moblin.android.various.model.setTwitchSubscribersOnlyMode
import com.moblin.android.various.model.startAds
import com.moblin.android.various.model.startRaidTwitchChannel

@Composable
private fun CreatePollView(model: Model = LocalModel.current, onCreated: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf(PollOption(), PollOption())) }
    var duration by remember { mutableStateOf(60) }
    val executor = remember { Executor() }

    Section(header = "Title") {
        IosTextField(placeholder = "Title", value = title, onValueChange = { title = it })
    }
    PollOptionsSectionView(
        header = "Choices",
        placeholder = "Choice",
        kind = localized("a choice"),
        options = options,
        onOptionsChange = { options = it },
        maxCount = 5,
    )
    Section {
        PickerRow(
            title = "Duration",
            values = listOf(30, 60, 120, 180, 300, 600),
            selected = duration,
            optionText = { formatShortDuration(seconds = it) },
            onSelected = { duration = it },
        )
    }
    Section {
        HCenter {
            ExecutorView(executor = executor) {
                CreateButtonView {
                    if (canCreatePoll(title = title, options = options)) {
                        executor.startProgress()
                        model.createTwitchPoll(
                            title = title.trim(),
                            choices = pollOptionTitles(options = options),
                            duration = duration,
                            onComplete = { result ->
                                executor.completed(result = result)
                                if (result.isSuccessful()) {
                                    onCreated()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePollView(model: Model = LocalModel.current, poll: TwitchApiPollData, onEnded: () -> Unit) {
    fun end(status: TwitchApiPollStatus, onComplete: (OperationResult) -> Unit) {
        model.endTwitchPoll(id = poll.id, status = status) { result ->
            onComplete(result)
            if (result.isSuccessful()) {
                onEnded()
            }
        }
    }

    Section(header = "Title") {
        Text(poll.title)
    }
    Section(header = "Choices") {
        poll.choices.forEach { choice ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(choice.title)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
    Section(footer = "Ending the poll shows the final results. Archiving it hides them.") {
        ActionRowView(text = "End poll", image = "stop") { onComplete ->
            end(status = TwitchApiPollStatus.terminated, onComplete = onComplete)
        }
        ActionRowView(text = "Archive poll", image = "archivebox") { onComplete ->
            end(status = TwitchApiPollStatus.archived, onComplete = onComplete)
        }
    }
}

@Composable
private fun PollFormView(model: Model = LocalModel.current) {
    var loaded by remember { mutableStateOf(false) }
    var poll by remember { mutableStateOf<TwitchApiPollData?>(null) }
    val executor = remember { Executor() }

    fun load() {
        executor.startProgress()
        model.getTwitchPolls { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    poll = result.value.firstOrNull { it.isActive() }
                    executor.completedNoTimer(result = NetworkResponse.Success(ByteArray(0)))
                }
                is NetworkResponse.AuthError -> {
                    executor.completedNoTimer(result = NetworkResponse.AuthError)
                }
                is NetworkResponse.Error -> {
                    executor.completedNoTimer(result = NetworkResponse.Error)
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
private fun PollView(model: Model = LocalModel.current) {
    NavigationLinkView(text = "Poll", image = "chart.bar") {
        PollFormView(model = model)
    }
}

@Composable
private fun CreatePredictionView(model: Model = LocalModel.current, onCreated: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var outcomes by remember { mutableStateOf(listOf(PollOption(), PollOption())) }
    var predictionWindow by remember { mutableStateOf(300) }
    val executor = remember { Executor() }

    Section(header = "Title") {
        IosTextField(placeholder = "Title", value = title, onValueChange = { title = it })
    }
    PollOptionsSectionView(
        header = "Outcomes",
        placeholder = "Outcome",
        kind = localized("an outcome"),
        options = outcomes,
        onOptionsChange = { outcomes = it },
        maxCount = 10,
    )
    Section {
        PickerRow(
            title = "Duration",
            values = listOf(60, 300, 600, 1800),
            selected = predictionWindow,
            optionText = { formatShortDuration(seconds = it) },
            onSelected = { predictionWindow = it },
        )
    }
    Section {
        HCenter {
            ExecutorView(executor = executor) {
                CreateButtonView {
                    if (canCreatePoll(title = title, options = outcomes)) {
                        executor.startProgress()
                        model.createTwitchPrediction(
                            title = title.trim(),
                            outcomes = pollOptionTitles(options = outcomes),
                            predictionWindow = predictionWindow,
                            onComplete = { result ->
                                executor.completed(result = result)
                                if (result.isSuccessful()) {
                                    onCreated()
                                }
                            },
                        )
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
                action { result -> executor.completed(result = result) }
            }
        }
    }
}

@Composable
private fun ActivePredictionView(
    model: Model = LocalModel.current,
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

    Section(header = "Title") {
        Text(prediction.title)
    }
    Section(
        header = "Outcomes",
        footer = "Resolve the prediction by selecting the winning outcome.",
    ) {
        prediction.outcomes.forEach { outcome ->
            PredictionOutcomeView(outcome = outcome) { onComplete ->
                end(
                    status = TwitchApiPredictionStatus.resolved,
                    winningOutcomeId = outcome.id,
                    onComplete = onComplete,
                )
            }
        }
    }
    Section(footer = "Cancelling the prediction refunds all channel points.") {
        if (prediction.isActive()) {
            ActionRowView(text = "Lock prediction", image = "lock") { onComplete ->
                end(status = TwitchApiPredictionStatus.locked, onComplete = onComplete)
            }
        }
        ActionRowView(text = "Cancel prediction", image = "xmark") { onComplete ->
            end(status = TwitchApiPredictionStatus.canceled, onComplete = onComplete)
        }
    }
}

@Composable
private fun PredictionFormView(model: Model = LocalModel.current) {
    var loaded by remember { mutableStateOf(false) }
    var prediction by remember { mutableStateOf<TwitchApiPredictionData?>(null) }
    val executor = remember { Executor() }

    fun load() {
        executor.startProgress()
        model.getTwitchPredictions { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    prediction = result.value.firstOrNull { it.isActive() || it.isLocked() }
                    executor.completedNoTimer(result = NetworkResponse.Success(ByteArray(0)))
                }
                is NetworkResponse.AuthError -> {
                    executor.completedNoTimer(result = NetworkResponse.AuthError)
                }
                is NetworkResponse.Error -> {
                    executor.completedNoTimer(result = NetworkResponse.Error)
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
private fun PredictionView(model: Model = LocalModel.current) {
    NavigationLinkView(text = "Prediction", image = "sparkles") {
        PredictionFormView(model = model)
    }
}

private fun makeRaidSuggestions(
    channels: List<TwitchApiChannel>,
    streams: List<TwitchApiStreamData>?,
): List<RaidSuggestion> {
    val viewerCounts = mutableMapOf<String, Int>()
    for (stream in streams ?: emptyList()) {
        viewerCounts[stream.user_id] = stream.viewer_count
    }
    return channels.map {
        RaidSuggestion(
            id = it.id,
            name = it.display_name,
            category = it.game_name,
            title = it.title,
            viewerCount = viewerCounts[it.id],
            image = it.thumbnail_url,
        )
    }
}

@Composable
private fun RaidChannelSearchView(model: Model = LocalModel.current) {
    var searchText by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<RaidSuggestion>>(emptyList()) }
    val executor = remember { Executor() }

    fun search() {
        if (searchText.isEmpty()) {
            suggestions = emptyList()
            return
        }
        val filter = searchText
        executor.startProgress()
        model.searchTwitchChannels(stream = model.stream.value, filter = filter) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    val channels = sortedBySearchPrefix(result.value, filter) { it.display_name }
                    model.getTwitchStreams(
                        stream = model.stream.value,
                        userIds = channels.map { it.id },
                        live = true,
                    ) { streams ->
                        if (filter != searchText) {
                            return@getTwitchStreams
                        }
                        suggestions = makeRaidSuggestions(channels = channels, streams = streams)
                        executor.completedNoTimer(result = NetworkResponse.Success(ByteArray(0)))
                    }
                }
                is NetworkResponse.AuthError -> {
                    executor.completedNoTimer(result = NetworkResponse.AuthError)
                }
                is NetworkResponse.Error -> {
                    executor.completedNoTimer(result = NetworkResponse.Error)
                }
            }
        }
    }

    Section {
        IosTextField(
            placeholder = "Search",
            value = searchText,
            onValueChange = { newValue ->
                searchText = newValue
                search()
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
        )
    }
    Section {
        ExecutorView(executor = executor, centerNonContent = true) {
            RaidSuggestionsView(model = model, suggestions = suggestions)
        }
    }
}

private data class RaidSuggestion(
    val id: String,
    val name: String,
    val category: String,
    val title: String,
    val viewerCount: Int?,
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

private fun makeRaidSuggestionsForRaidChannels(
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
private fun RaidSuggestionsView(model: Model = LocalModel.current, suggestions: List<RaidSuggestion>) {
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
            model.startRaidTwitchChannel(channelId = suggestion.id, onComplete = onComplete)
        }
    }
}

@Composable
private fun RaidHistoryView(model: Model = LocalModel.current, title: String, suggestions: List<RaidSuggestion>) {
    Section(header = title) {
        RaidSuggestionsView(model = model, suggestions = suggestions)
    }
}

private fun fetchRaidSuggestionImages(
    model: Model,
    userIds: List<String>,
    onComplete: (Map<String, String>) -> Unit,
) {
    model.getTwitchUsers(stream = model.stream.value, userIds = userIds.distinct()) { users ->
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
    model: Model = LocalModel.current,
    suggestions: List<RaidSuggestion>,
    executor: Executor,
) {
    Section(header = "Followed channels") {
        ExecutorView(executor = executor, centerNonContent = true) {
            RaidSuggestionsView(model = model, suggestions = suggestions)
        }
    }
}

@Composable
private fun RunCommercialView(model: Model = LocalModel.current) {
    var duration by remember { mutableStateOf(30) }
    val executor = remember { Executor() }

    NavigationLinkView(text = "Run commercial", image = "cup.and.saucer") {
        Section(header = "Duration") {
            PickerRow(
                title = "Duration",
                values = listOf(30, 60, 90, 120, 180),
                selected = duration,
                optionText = { formatShortDuration(seconds = it) },
                onSelected = { duration = it },
            )
        }
        Section {
            HCenter {
                ExecutorView(executor = executor) {
                    TextButtonView("Run commercial") {
                        executor.startProgress()
                        model.startAds(seconds = duration, onComplete = { result ->
                            executor.completed(result = result)
                        })
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
private fun SendAnnouncementView(model: Model = LocalModel.current) {
    var message by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(AnnouncementColor.primary) }
    val executor = remember { Executor() }

    fun canSend(): Boolean {
        return message.trim().isNotEmpty()
    }

    NavigationLinkView(text = "Send announcement", image = "megaphone") {
        Section(header = "Message") {
            IosTextField(placeholder = "Message", value = message, onValueChange = { message = it })
        }
        Section {
            PickerRow(
                title = "Color",
                values = AnnouncementColor.entries,
                selected = color,
                optionText = { it.toString() },
                onSelected = { color = it },
            )
        }
        Section {
            HCenter {
                ExecutorView(executor = executor) {
                    TextButtonView("Send") {
                        if (canSend()) {
                            executor.startProgress()
                            model.sendTwitchAnnouncement(
                                message = message.trim(),
                                color = color.rawValue,
                                onComplete = { result -> executor.completed(result = result) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartRaidView(model: Model = LocalModel.current) {
    var raidsSent by remember { mutableStateOf<List<RaidSuggestion>>(emptyList()) }
    var raidsReceived by remember { mutableStateOf<List<RaidSuggestion>>(emptyList()) }
    var followedChannels by remember { mutableStateOf<List<RaidSuggestion>>(emptyList()) }
    val followedChannelsExecutor = remember { Executor() }

    fun loadRaidHistory() {
        val sentChannels = model.stream.value.twitchRaidsSent
        val receivedChannels = model.stream.value.twitchRaidsReceived
        val userIds = (sentChannels.map { it.channelId } + receivedChannels.map { it.channelId }).distinct()
        model.getTwitchStreams(stream = model.stream.value, userIds = userIds, live = true) { streams ->
            if (streams == null) {
                return@getTwitchStreams
            }
            raidsSent = makeRaidSuggestionsForRaidChannels(streams = streams, channels = sentChannels)
            raidsReceived = makeRaidSuggestionsForRaidChannels(streams = streams, channels = receivedChannels)
            fetchRaidSuggestionImages(
                model = model,
                userIds = raidsSent.map { it.id } + raidsReceived.map { it.id },
            ) { images ->
                raidsSent = setRaidSuggestionImages(suggestions = raidsSent, images = images)
                raidsReceived = setRaidSuggestionImages(suggestions = raidsReceived, images = images)
            }
        }
    }

    fun loadFollowedChannels() {
        followedChannelsExecutor.startProgress()
        model.getTwitchFollowedStreams(stream = model.stream.value) { result ->
            when (result) {
                is NetworkResponse.Success -> {
                    followedChannels = makeRaidSuggestions(streams = result.value)
                    followedChannelsExecutor.completedNoTimer(result = NetworkResponse.Success(ByteArray(0)))
                    fetchRaidSuggestionImages(
                        model = model,
                        userIds = followedChannels.map { it.id },
                    ) { images ->
                        followedChannels = setRaidSuggestionImages(suggestions = followedChannels, images = images)
                    }
                }
                is NetworkResponse.AuthError -> {
                    followedChannelsExecutor.completedNoTimer(result = NetworkResponse.AuthError)
                }
                is NetworkResponse.Error -> {
                    followedChannelsExecutor.completedNoTimer(result = NetworkResponse.Error)
                }
            }
        }
    }

    NavigationLinkView(text = "Raid channel", image = "play.tv") {
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
    LaunchedEffect(Unit) {
        loadRaidHistory()
        loadFollowedChannels()
    }
}

@Composable
fun QuickButtonChatModerationTwitchView(
    model: Model = LocalModel.current,
    platform: Platform?,
    onPlatformChange: (Platform?) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            QuickButtonChatModerationTwitchForm(
                model = model,
                onPlatformChange = onPlatformChange,
            )
        },
    ) {
        TwitchLogoAndNameView()
    }
}

@Composable
fun QuickButtonChatModerationTwitchForm(
    model: Model = LocalModel.current,
    onPlatformChange: (Platform?) -> Unit,
) {
    fun slowModeAction(duration: Int?, onComplete: (OperationResult) -> Unit) {
        model.setTwitchSlowMode(enabled = duration != null, duration = duration, onComplete = onComplete)
    }

    fun followersOnlyAction(duration: Int?, onComplete: (OperationResult) -> Unit) {
        model.setTwitchFollowersMode(
            enabled = duration != null,
            duration = (duration ?: 0) / 60,
            onComplete = onComplete,
        )
    }

    Form(title = "Twitch") {
        LaunchedEffect(Unit) {
            onPlatformChange(Platform.twitch)
        }
        Section {
            StartRaidView(model = model)
            RunCommercialView(model = model)
            SendAnnouncementView(model = model)
            PollView(model = model)
            PredictionView(model = model)
        }
        Section {
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
                action = { enabled, onComplete ->
                    model.setTwitchSubscribersOnlyMode(enabled = enabled, onComplete = onComplete)
                },
            )
            EmotesOnlyView(
                action = { enabled, onComplete ->
                    model.setTwitchEmoteOnlyMode(enabled = enabled, onComplete = onComplete)
                },
            )
        }
        Section {
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

@Composable
private fun <T> PickerRow(
    title: String?,
    values: List<T>,
    selected: T,
    optionText: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Picker(
        title = title.orEmpty(),
        selection = selected,
        options = values,
        text = optionText,
        onChange = onSelected,
    )
}

@Composable
private fun IosTextField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val palette = formPalette()
    Box(modifier = Modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(
                text = localized(placeholder),
                style = formBodyStyle,
                color = palette.secondaryLabel,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = formBodyStyle.copy(color = palette.label),
            keyboardOptions = keyboardOptions,
            singleLine = true,
            cursorBrush = SolidColor(palette.accent),
        )
    }
}
