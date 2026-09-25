package com.moblin.android.view.controlbar.quickbutton.chat.moderation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.kick.KickFollowedChannel
import com.moblin.android.streamingplatforms.kick.KickLiveSearchChannel
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.createKickApi
import com.moblin.android.various.model.createKickPoll
import com.moblin.android.various.model.createKickPrediction
import com.moblin.android.various.model.deleteKickPoll
import com.moblin.android.various.model.disableKickFollowersMode
import com.moblin.android.various.model.disableKickSlowMode
import com.moblin.android.various.model.enableKickFollowersMode
import com.moblin.android.various.model.enableKickSlowMode
import com.moblin.android.various.model.hostKickChannel
import com.moblin.android.various.model.searchKickChannels
import com.moblin.android.various.model.setKickEmoteOnlyMode
import com.moblin.android.various.model.setKickShowViewCount
import com.moblin.android.various.model.setKickSubscribersOnlyMode
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
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
import com.moblin.android.view.controlbar.quickbutton.chat.ToggleActionView
import com.moblin.android.view.controlbar.quickbutton.chat.UserModerationItemView
import com.moblin.android.view.controlbar.quickbutton.chat.canCreatePoll
import com.moblin.android.view.controlbar.quickbutton.chat.pollOptionTitles
import com.moblin.android.view.settings.streams.stream.KickLogoAndNameView
import com.moblin.android.view.utils.BorderlessButtonView
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.HCenter
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun CreatePollView(model: Model = LocalModel.current) {
    var title by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf(PollOption(), PollOption())) }
    var duration by remember { mutableStateOf(30) }
    var resultDisplayDuration by remember { mutableStateOf(15) }
    val executor = remember { Executor() }

    NavigationLinkView(text = "Create poll", image = "chart.bar") {
        Section(header = "Title") {
            IosTextField(placeholder = "Title", value = title, onValueChange = { title = it })
        }
        PollOptionsSectionView(
            header = "Options",
            placeholder = "Option",
            kind = localized("an option"),
            options = options,
            onOptionsChange = { options = it },
            maxCount = 6,
        )
        Section {
            Picker(
                title = "Duration",
                selection = duration,
                options = listOf(30, 120, 180, 240, 300),
                text = { formatShortDuration(seconds = it) },
            ) {
                duration = it
            }
        }
        Section {
            Picker(
                title = "Result display duration",
                selection = resultDisplayDuration,
                options = listOf(15, 30, 120, 180, 240, 300),
                text = { formatShortDuration(seconds = it) },
            ) {
                resultDisplayDuration = it
            }
        }
        Section {
            HCenter {
                ExecutorView(executor = executor) {
                    CreateButtonView {
                        if (canCreatePoll(title = title, options = options)) {
                            executor.startProgress()
                            model.createKickPoll(
                                title = title.trim(),
                                options = pollOptionTitles(options = options),
                                duration = duration,
                                resultDisplayDuration = resultDisplayDuration,
                                onComplete = executor::completed,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePredictionView(model: Model = LocalModel.current) {
    var title by remember { mutableStateOf("") }
    var outcome1 by remember { mutableStateOf("") }
    var outcome2 by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(300) }
    val executor = remember { Executor() }

    fun canExecute(): Boolean {
        return title.trim().isNotEmpty() &&
            outcome1.trim().isNotEmpty() &&
            outcome2.trim().isNotEmpty()
    }

    NavigationLinkView(text = "Create prediction", image = "sparkles") {
        Section(header = "Title") {
            IosTextField(placeholder = "Title", value = title, onValueChange = { title = it })
        }
        Section(header = "Outcomes") {
            IosTextField(placeholder = "Outcome", value = outcome1, onValueChange = { outcome1 = it })
            IosTextField(placeholder = "Outcome", value = outcome2, onValueChange = { outcome2 = it })
        }
        Section {
            Picker(
                title = "Duration",
                selection = duration,
                options = listOf(60, 300, 600, 1800),
                text = { formatShortDuration(seconds = it) },
            ) {
                duration = it
            }
        }
        Section {
            HCenter {
                ExecutorView(executor = executor) {
                    CreateButtonView {
                        if (canExecute()) {
                            executor.startProgress()
                            model.createKickPrediction(
                                title = title.trim(),
                                outcomes = listOf(outcome1.trim(), outcome2.trim()),
                                duration = duration,
                                onComplete = executor::completed,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RaidChannelSearchView(model: Model = LocalModel.current) {
    var searchText by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<KickLiveSearchChannel>>(emptyList()) }
    val executor = remember { Executor() }

    Section {
        IosTextField(
            placeholder = "Search",
            value = searchText,
            onValueChange = { newValue ->
                val changed = newValue != searchText
                searchText = newValue
                if (changed && newValue.isEmpty()) {
                    channels = emptyList()
                } else if (changed) {
                    executor.startProgress()
                    model.searchKickChannels(query = newValue) { results ->
                        if (results != null) {
                            channels = sortedBySearchPrefix(results, searchText = newValue) { it.username }
                            executor.completedNoTimer(result = NetworkResponse.Success(ByteArray(0)))
                        } else {
                            executor.completedNoTimer(result = NetworkResponse.Error)
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
        )
    }
    Section {
        ExecutorView(executor = executor, centerNonContent = true) {
            channels.forEach { channel ->
                RaidChannelView(
                    buttonText = "Raid",
                    channel = channel.username,
                    category = channel.category ?: "",
                    title = "",
                    image = channel.profile_pic,
                    isLive = channel.is_live,
                    viewerCount = channel.viewers_count,
                ) { onComplete ->
                    model.hostKickChannel(channel = channel.username, onComplete = onComplete)
                }
            }
        }
    }
}

@Composable
private fun HostChannelView(model: Model = LocalModel.current) {
    var channels by remember { mutableStateOf<List<KickFollowedChannel>>(emptyList()) }
    var cursor by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun loadMoreChannels() {
        if (isLoading) {
            return
        }
        isLoading = true
        model.createKickApi(stream = model.stream.value).getFollowedChannels(cursor = cursor) { response ->
            isLoading = false
            if (response != null) {
                channels = channels + response.channels
                cursor = response.nextCursor
            }
        }
    }

    NavigationLinkView(text = "Raid channel", image = "play.tv") {
        RaidChannelSearchView(model = model)
        Section(header = "Followed channels") {
            channels.filter { it.is_live }.forEach { channel ->
                RaidChannelView(
                    buttonText = "Raid",
                    channel = channel.user_username,
                    category = channel.category_name ?: "",
                    title = channel.session_title ?: "",
                    image = channel.profile_picture,
                    isLive = true,
                    viewerCount = channel.viewer_count,
                ) { onComplete ->
                    model.hostKickChannel(channel = channel.user_username, onComplete = onComplete)
                }
            }
            if (isLoading) {
                HCenter {
                    CircularProgressIndicator()
                }
            } else if (cursor != null) {
                HCenter {
                    BorderlessButtonView(text = "Load more") {
                        loadMoreChannels()
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            channels = emptyList()
            cursor = null
            loadMoreChannels()
        }
    }
}

@Composable
private fun ShowViewCountView(action: (Boolean, (OperationResult) -> Unit) -> Unit) {
    ToggleActionView(text = "Show view count on channel", image = "eye", action = action)
}

@Composable
fun QuickButtonChatModerationKickView(
    model: Model = LocalModel.current,
    platform: Platform?,
    onPlatformChange: (Platform?) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            QuickButtonChatModerationKickForm(
                model = model,
                onPlatformChange = onPlatformChange,
            )
        },
    ) {
        KickLogoAndNameView()
    }
}

@Composable
private fun QuickButtonChatModerationKickForm(
    model: Model = LocalModel.current,
    onPlatformChange: (Platform?) -> Unit,
) {
    fun slowModeAction(duration: Int?, onComplete: (OperationResult) -> Unit) {
        if (duration != null) {
            model.enableKickSlowMode(messageInterval = duration, onComplete = onComplete)
        } else {
            model.disableKickSlowMode(onComplete = onComplete)
        }
    }

    fun followersOnlyAction(duration: Int?, onComplete: (OperationResult) -> Unit) {
        if (duration != null) {
            model.enableKickFollowersMode(followingMinDuration = duration / 60, onComplete = onComplete)
        } else {
            model.disableKickFollowersMode(onComplete = onComplete)
        }
    }

    Form(title = "Kick") {
        LaunchedEffect(Unit) {
            onPlatformChange(Platform.kick)
        }
        Section {
            HostChannelView(model = model)
            CreatePollView(model = model)
            ActionRowView(text = "Delete poll", image = "chart.bar") { onComplete ->
                model.deleteKickPoll(onComplete = onComplete)
            }
            CreatePredictionView(model = model)
        }
        Section {
            SlowModeView(
                durations = listOf(3, 5, 10, 30, 60, 120, 300),
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
                    model.setKickSubscribersOnlyMode(enabled = enabled, onComplete = onComplete)
                },
            )
            EmotesOnlyView(
                action = { enabled, onComplete ->
                    model.setKickEmoteOnlyMode(enabled = enabled, onComplete = onComplete)
                },
            )
            ShowViewCountView(
                action = { enabled, onComplete ->
                    model.setKickShowViewCount(enabled = enabled, onComplete = onComplete)
                },
            )
        }
        Section {
            ModActionType.entries.forEach { action ->
                UserModerationItemView(model = model, action = action, platform = Platform.kick)
            }
        }
    }
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
                color = palette.tertiaryLabel,
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
