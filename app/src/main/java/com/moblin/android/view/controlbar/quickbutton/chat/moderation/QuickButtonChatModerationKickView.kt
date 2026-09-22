package com.moblin.android.view.controlbar.quickbutton.chat.moderation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.kick.KickFollowedChannel
import com.moblin.android.streamingplatforms.kick.KickLiveSearchChannel
import com.moblin.android.various.model.Model
import com.moblin.android.various.network.OperationResult
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePollView(model: Model = LocalModel.current) {
    var title by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf(PollOption(), PollOption())) }
    var duration by remember { mutableStateOf(30) }
    var resultDisplayDuration by remember { mutableStateOf(15) }
    var durationExpanded by remember { mutableStateOf(false) }
    var resultDisplayExpanded by remember { mutableStateOf(false) }
    val executor = remember { Executor() }

    NavigationLinkView(text = "Create poll", image = "chart.bar") {
        Text("Title", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        PollOptionsSectionView(
            header = "Options",
            placeholder = "Option",
            kind = localized("an option"),
            options = options,
            onOptionsChange = { options = it },
            maxCount = 6,
        )
        ExposedDropdownMenuBox(
            expanded = durationExpanded,
            onExpandedChange = { durationExpanded = it },
        ) {
            OutlinedTextField(
                value = formatShortDuration(seconds = duration),
                onValueChange = {},
                readOnly = true,
                label = { Text("Duration") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = durationExpanded,
                onDismissRequest = { durationExpanded = false },
            ) {
                listOf(30, 120, 180, 240, 300).forEach { value ->
                    DropdownMenuItem(
                        text = { Text(formatShortDuration(seconds = value)) },
                        onClick = {
                            duration = value
                            durationExpanded = false
                        },
                    )
                }
            }
        }
        ExposedDropdownMenuBox(
            expanded = resultDisplayExpanded,
            onExpandedChange = { resultDisplayExpanded = it },
        ) {
            OutlinedTextField(
                value = formatShortDuration(seconds = resultDisplayDuration),
                onValueChange = {},
                readOnly = true,
                label = { Text("Result display duration") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = resultDisplayExpanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = resultDisplayExpanded,
                onDismissRequest = { resultDisplayExpanded = false },
            ) {
                listOf(15, 30, 120, 180, 240, 300).forEach { value ->
                    DropdownMenuItem(
                        text = { Text(formatShortDuration(seconds = value)) },
                        onClick = {
                            resultDisplayDuration = value
                            resultDisplayExpanded = false
                        },
                    )
                }
            }
        }
        HCenter {
            ExecutorView(executor = executor) {
                CreateButtonView(action = {
                    executor.startProgress()
                    Unit
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePredictionView(model: Model = LocalModel.current) {
    var title by remember { mutableStateOf("") }
    var outcome1 by remember { mutableStateOf("") }
    var outcome2 by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(300) }
    var durationExpanded by remember { mutableStateOf(false) }
    val executor = remember { Executor() }

    fun canExecute(): Boolean {
        return title.trim().isNotEmpty() &&
            outcome1.trim().isNotEmpty() &&
            outcome2.trim().isNotEmpty()
    }

    NavigationLinkView(text = "Create prediction", image = "sparkles") {
        Text("Title", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Outcomes", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = outcome1,
            onValueChange = { outcome1 = it },
            label = { Text("Outcome") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = outcome2,
            onValueChange = { outcome2 = it },
            label = { Text("Outcome") },
            modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenuBox(
            expanded = durationExpanded,
            onExpandedChange = { durationExpanded = it },
        ) {
            OutlinedTextField(
                value = formatShortDuration(seconds = duration),
                onValueChange = {},
                readOnly = true,
                label = { Text("Duration") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded)
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = durationExpanded,
                onDismissRequest = { durationExpanded = false },
            ) {
                listOf(60, 300, 600, 1800).forEach { value ->
                    DropdownMenuItem(
                        text = { Text(formatShortDuration(seconds = value)) },
                        onClick = {
                            duration = value
                            durationExpanded = false
                        },
                    )
                }
            }
        }
        HCenter {
            ExecutorView(executor = executor) {
                CreateButtonView(action = {
                    executor.startProgress()
                    Unit
                })
            }
        }
    }
}

@Composable
private fun RaidChannelSearchView(model: Model = LocalModel.current) {
    var searchText by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<KickLiveSearchChannel>>(emptyList()) }
    val executor = remember { Executor() }

    LaunchedEffect(searchText) {
        if (searchText.isEmpty()) {
            channels = emptyList()
            return@LaunchedEffect
        }
        executor.startProgress()
        Unit
    }

    Column {
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
        )
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
                    Unit
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
        Unit
    }

    NavigationLinkView(text = "Raid channel", image = "play.tv") {
        RaidChannelSearchView(model = model)
        Text("Followed channels", style = MaterialTheme.typography.titleMedium)
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
                Unit
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

@Composable
private fun ShowViewCountView(action: (Boolean, (OperationResult) -> Unit) -> Unit) {
    ToggleActionView(text = "Show view count on channel", image = "eye", action = action)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonChatModerationKickView(
    model: Model = LocalModel.current,
    platform: Platform?,
    onPlatformChange: (Platform?) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    fun slowModeAction(duration: Int?, onComplete: (OperationResult) -> Unit) {
        if (duration != null) {
            Unit
        } else {
            Unit
        }
    }

    fun followersOnlyAction(duration: Int?, onComplete: (OperationResult) -> Unit) {
        if (duration != null) {
            Unit
        } else {
            Unit
        }
    }

    LaunchedEffect(Unit) {
        onPlatformChange(Platform.kick)
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        TopAppBar(title = { Text("Kick") })
        Box(modifier = Modifier.clickable { onNavigate("Kick") }) {
            KickLogoAndNameView()
        }
        HostChannelView(model = model)
        CreatePollView(model = model)
        ActionRowView(text = "Delete poll", image = "chart.bar") { onComplete ->
            Unit
        }
        CreatePredictionView(model = model)
        HorizontalDivider()
        SlowModeView(
            durations = listOf(3, 5, 10, 30, 60, 120, 300),
            action = ::slowModeAction,
        )
        FollowersOnlyView(
            durations = listOf(60, 300, 600, 3600),
            action = ::followersOnlyAction,
        )
        SubscribersOnlyView(
            action = { enabled, onComplete ->
                Unit
            },
        )
        EmotesOnlyView(
            action = { enabled, onComplete ->
                Unit
            },
        )
        ShowViewCountView(
            action = { enabled, onComplete ->
                Unit
            },
        )
        HorizontalDivider()
        ModActionType.entries.forEach { action ->
            UserModerationItemView(model = model, action = action, platform = Platform.kick)
        }
    }
}
