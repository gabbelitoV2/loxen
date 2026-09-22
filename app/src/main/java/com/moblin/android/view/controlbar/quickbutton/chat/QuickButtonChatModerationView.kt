package com.moblin.android.view.controlbar.quickbutton.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moblin.android.R
import com.moblin.android.common.various.countFormatter
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.twitch.TwitchLoginView
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.network.AuthError
import com.moblin.android.various.network.Error
import com.moblin.android.various.network.OperationResult
import com.moblin.android.various.network.Success
import com.moblin.android.view.controlbar.quickbutton.chat.moderation.QuickButtonChatModerationKickView
import com.moblin.android.view.controlbar.quickbutton.chat.moderation.QuickButtonChatModerationTwitchView
import com.moblin.android.view.utils.AddButtonView
import com.moblin.android.view.utils.BorderlessButtonView
import com.moblin.android.view.utils.CloseToolbar
import com.moblin.android.view.utils.HCenter
import com.moblin.android.view.utils.IconAndTextLocalizedView
import com.moblin.android.view.utils.ShortcutSectionView
import com.moblin.android.view.utils.StreamingPlatformsShortcutView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel

enum class ExecutorState {
    idle,
    inProgress,
    success,
    authError,
    error,
}

class Executor {
    private val scope = CoroutineScope(Dispatchers.Main)
    val state = MutableStateFlow(ExecutorState.idle)

    fun startProgress() {
        state.value = ExecutorState.inProgress
    }

    fun completed(result: OperationResult) {
        state.value = when (result) {
            is Success<*> -> ExecutorState.success
            AuthError -> ExecutorState.authError
            Error -> ExecutorState.error
        }
        scope.launch {
            delay(3000)
            state.value = ExecutorState.idle
        }
    }

    fun completedNoTimer(result: OperationResult) {
        state.value = when (result) {
            is Success<*> -> ExecutorState.idle
            AuthError -> ExecutorState.authError
            Error -> ExecutorState.error
        }
    }
}

@Composable
fun ExecutorView(
    model: Model = LocalModel.current,
    executor: Executor,
    centerNonContent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val state by executor.state.collectAsState()

    val handleState: () -> Unit = {
        if (executor.state.value == ExecutorState.authError) {
            TODO("model.twitchLogin(stream) { showModerationAuth = true }")
        }
    }

    LaunchedEffect(Unit) {
        handleState()
    }
    LaunchedEffect(state) {
        handleState()
    }

    when (state) {
        ExecutorState.idle -> content()
        ExecutorState.inProgress -> {
            if (centerNonContent) {
                HCenter {
                    CircularProgressIndicator()
                }
            } else {
                CircularProgressIndicator()
            }
        }
        ExecutorState.success -> {
            if (centerNonContent) {
                HCenter {
                    Text(text = localized("Success"), color = Color.Green)
                }
            } else {
                Text(text = localized("Success"), color = Color.Green)
            }
        }
        ExecutorState.authError -> {
            if (centerNonContent) {
                HCenter {
                    Text(text = localized("Not logged in"), color = Color.Red)
                }
            } else {
                Text(text = localized("Not logged in"), color = Color.Red)
            }
        }
        ExecutorState.error -> {
            if (centerNonContent) {
                HCenter {
                    Text(text = localized("Failed"), color = Color.Red)
                }
            } else {
                Text(text = localized("Failed"), color = Color.Red)
            }
        }
    }
}

@Composable
private fun button(
    text: String,
    on: Boolean,
    executor: Executor,
    action: (Boolean, (OperationResult) -> Unit) -> Unit,
) {
    BorderlessButtonView(text = text) {
        executor.startProgress()
        action(on, executor::completed)
    }
}

@Composable
fun ToggleActionView(
    model: Model = LocalModel.current,
    text: String,
    image: String,
    action: (Boolean, (OperationResult) -> Unit) -> Unit,
) {
    val executor = remember { Executor() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconAndTextLocalizedView(image = image, text = text)
        Spacer(Modifier.weight(1f))
        ExecutorView(model = model, executor = executor) {
            Box(modifier = Modifier.padding(end = 15.dp)) {
                button(text = "On", on = true, executor = executor, action = action)
            }
            button(text = "Off", on = false, executor = executor, action = action)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationActionView(
    model: Model = LocalModel.current,
    text: String,
    image: String,
    durations: List<Int>,
    action: (Int?, (OperationResult) -> Unit) -> Unit,
) {
    val executor = remember { Executor() }
    var duration by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconAndTextLocalizedView(image = image, text = text)
        Spacer(Modifier.weight(1f))
        ExecutorView(model = model, executor = executor) {
            Box(modifier = Modifier.padding(end = 15.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = duration?.let { formatShortDuration(seconds = it) } ?: localized("Off"),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(localized("Off")) },
                            onClick = {
                                duration = null
                                expanded = false
                            },
                        )
                        durations.forEach { value ->
                            DropdownMenuItem(
                                text = { Text(formatShortDuration(seconds = value)) },
                                onClick = {
                                    duration = value
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            BorderlessButtonView(text = "Send") {
                executor.startProgress()
                action(duration, executor::completed)
            }
        }
    }
}

enum class ModActionType {
    ban,
    timeout,
    unban,
    mod,
    unmod,
    vip,
    unvip;

    fun title(): String {
        return when (this) {
            ban -> "Ban"
            timeout -> "Timeout"
            unban -> "Unban"
            mod -> "Mod"
            unmod -> "Unmod"
            vip -> "VIP"
            unvip -> "UnVIP"
        }
    }

    fun image(): String {
        return when (this) {
            ban -> "hand.raised"
            timeout -> "clock"
            unban -> "checkmark.circle"
            mod -> "shield"
            unmod -> "shield.slash"
            vip -> "crown"
            unvip -> "crown"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserModerationItemView(
    model: Model = LocalModel.current,
    action: ModActionType,
    platform: Platform,
) {
    var username by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var timeoutDuration by remember { mutableStateOf(60) }
    val executor = remember { Executor() }
    val timeoutPresets = remember { listOf(60, 300, 600, 1800, 3600, 21600, 86400, 604800) }
    var timeoutExpanded by remember { mutableStateOf(false) }

    fun canExecute(): Boolean {
        return username.trim().isNotEmpty()
    }

    fun executeKickAction(
        user: String,
        banReason: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        when (action) {
            ModActionType.ban -> TODO("model.banKickUser(user, duration = null, reason, onComplete)")
            ModActionType.timeout -> TODO("model.banKickUser(user, duration = timeoutDuration, onComplete)")
            ModActionType.unban -> TODO("model.unbanKickUser(user, onComplete)")
            ModActionType.mod -> TODO("model.modKickUser(user, onComplete)")
            ModActionType.unmod -> TODO("model.unmodKickUser(user, onComplete)")
            ModActionType.vip -> TODO("model.vipKickUser(user, onComplete)")
            ModActionType.unvip -> TODO("model.unvipKickUser(user, onComplete)")
        }
    }

    fun executeTwitchAction(
        user: String,
        banReason: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        when (action) {
            ModActionType.ban -> TODO("model.banTwitchUser(user, duration = null, reason, onComplete)")
            ModActionType.timeout -> TODO("model.banTwitchUser(user, duration = timeoutDuration, reason = null, onComplete)")
            ModActionType.unban -> TODO("model.unbanTwitchUser(user, onComplete)")
            ModActionType.mod -> TODO("model.modTwitchUser(user, onComplete)")
            ModActionType.unmod -> TODO("model.unmodTwitchUser(user, onComplete)")
            ModActionType.vip -> TODO("model.vipTwitchUser(user, onComplete)")
            ModActionType.unvip -> TODO("model.unvipTwitchUser(user, onComplete)")
        }
    }

    fun executeAction(onComplete: (OperationResult) -> Unit) {
        val user = username.trim()
        val banReason = reason.trim()
        when (platform) {
            Platform.kick -> executeKickAction(user, banReason, onComplete)
            Platform.twitch -> executeTwitchAction(user, banReason, onComplete)
            else -> Unit
        }
    }

    NavigationLinkView(text = action.title(), image = action.image()) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(localized("Username"))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                placeholder = { Text(localized("Username")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (action == ModActionType.timeout) {
                Column {
                    Text(localized("Duration"))
                    ExposedDropdownMenuBox(
                        expanded = timeoutExpanded,
                        onExpandedChange = { timeoutExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = formatShortDuration(seconds = timeoutDuration),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeoutExpanded)
                            },
                            modifier = Modifier.menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = timeoutExpanded,
                            onDismissRequest = { timeoutExpanded = false },
                        ) {
                            timeoutPresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(formatShortDuration(seconds = preset)) },
                                    onClick = {
                                        timeoutDuration = preset
                                        timeoutExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (action == ModActionType.ban) {
                Text(localized("Reason"))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = { Text(localized("Reason")) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HCenter {
                ExecutorView(model = model, executor = executor) {
                    TextButtonView("Send") {
                        if (canExecute()) {
                            executor.startProgress()
                            executeAction(executor::completed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionRowView(
    model: Model = LocalModel.current,
    text: String,
    image: String,
    action: ((OperationResult) -> Unit) -> Unit,
) {
    val executor = remember { Executor() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconAndTextLocalizedView(image = image, text = text)
        Spacer(Modifier.weight(1f))
        ExecutorView(model = model, executor = executor) {
            BorderlessButtonView(text = "Send") {
                executor.startProgress()
                action(executor::completed)
            }
        }
    }
}

data class PollOption(
    val id: UUID = UUID.randomUUID(),
    val text: String = "",
)

fun canCreatePoll(title: String, options: List<PollOption>): Boolean {
    return title.trim().isNotEmpty() && options.count { it.text.trim().isNotEmpty() } >= 2
}

fun pollOptionTitles(options: List<PollOption>): List<String> {
    return options.map { it.text.trim() }.filter { it.isNotEmpty() }
}

@Composable
fun PollOptionsSectionView(
    header: String,
    placeholder: String,
    kind: String,
    options: List<PollOption>,
    onOptionsChange: (List<PollOption>) -> Unit,
    maxCount: Int,
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(header)
        options.forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = option.text,
                    onValueChange = { newValue ->
                        onOptionsChange(
                            options.map {
                                if (it.id == option.id) it.copy(text = newValue) else it
                            },
                        )
                    },
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.weight(1f),
                )
                if (options.size > 2) {
                    IconButton(onClick = {
                        onOptionsChange(options.filterNot { it.id == option.id })
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
        }
        if (options.size < maxCount) {
            AddButtonView {
                onOptionsChange(options + PollOption())
            }
        }
        SwipeLeftToDeleteHelpView(kind = kind)
    }
}

@Composable
fun ChannelImageView(image: String?) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape),
    ) {
        if (!image.isNullOrEmpty()) {
            CacheAsyncImage(
                url = URI(image),
                content = { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                placeholder = {},
            )
        } else {
            Icon(
                painter = painterResource(id = TODO("app icon drawable is missing")),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun RaidChannelView(
    model: Model = LocalModel.current,
    buttonText: String,
    channel: String,
    category: String,
    title: String,
    image: String?,
    isLive: Boolean,
    viewerCount: Int?,
    action: ((OperationResult) -> Unit) -> Unit,
) {
    val executor = remember { Executor() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        ChannelImageView(image = image)
        Column(horizontalAlignment = Alignment.Start) {
            Text(channel)
            if (isLive) {
                Text(category, style = MaterialTheme.typography.bodySmall)
                Text(title, style = MaterialTheme.typography.bodySmall)
            } else {
                Text(localized("Offline"), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.weight(1f))
        if (viewerCount != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Text(
                    countFormatter.format(viewerCount),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (isLive) {
            ExecutorView(model = model, executor = executor) {
                BorderlessButtonView(text = buttonText) {
                    executor.startProgress()
                    action(executor::completed)
                }
            }
        }
    }
}

@Composable
fun SlowModeView(
    model: Model = LocalModel.current,
    durations: List<Int>,
    action: (Int?, (OperationResult) -> Unit) -> Unit,
) {
    DurationActionView(
        model = model,
        text = "Slow mode",
        image = "tortoise",
        durations = durations,
        action = action,
    )
}

@Composable
fun FollowersOnlyView(
    model: Model = LocalModel.current,
    durations: List<Int>,
    action: (Int?, (OperationResult) -> Unit) -> Unit,
) {
    DurationActionView(
        model = model,
        text = "Followers only",
        image = "person.2",
        durations = durations,
        action = action,
    )
}

@Composable
fun SubscribersOnlyView(
    model: Model = LocalModel.current,
    action: (Boolean, (OperationResult) -> Unit) -> Unit,
) {
    ToggleActionView(
        model = model,
        text = "Subscribers only",
        image = "star",
        action = action,
    )
}

@Composable
fun EmotesOnlyView(
    model: Model = LocalModel.current,
    action: (Boolean, (OperationResult) -> Unit) -> Unit,
) {
    val darkMode = androidx.compose.foundation.isSystemInDarkTheme()
    ToggleActionView(
        model = model,
        text = "Emotes only",
        image = if (darkMode) "face.smiling.inverse" else "face.smiling",
        action = action,
    )
}

@Composable
fun NavigationLinkView(
    text: String,
    image: String,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.Start) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconAndTextLocalizedView(image = image, text = text)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }
        if (expanded) {
            Column(horizontalAlignment = Alignment.Start) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickButtonChatModerationView(
    model: Model = LocalModel.current,
    presentingModeration: Boolean,
    onPresentingModerationChange: (Boolean) -> Unit,
) {
    var platform by remember { mutableStateOf<Platform?>(null) }
    val showModerationAuth by model.showModerationAuth.collectAsState()

    Column {
        TopAppBar(
            title = { Text(localized("Moderation")) },
            navigationIcon = {
                CloseToolbar(presentingModeration, onPresentingModerationChange)
            },
        )
        Column {
            QuickButtonChatModerationTwitchView(
                model = model,
                platform = platform,
                onPlatformChange = { platform = it },
            )
            QuickButtonChatModerationKickView(
                model = model,
                platform = platform,
                onPlatformChange = { platform = it },
            )
            ShortcutSectionView {
                StreamingPlatformsShortcutView(model = model, stream = model.stream.value)
            }
        }
    }

    if (showModerationAuth) {
        ModalBottomSheet(onDismissRequest = { model.showModerationAuth.value = false }) {
            when (platform) {
                Platform.twitch -> TwitchLoginView(model, showModerationAuth) {
                    model.showModerationAuth.value = it
                }
                else -> Unit
            }
        }
    }
}
