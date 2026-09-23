package com.moblin.android.view.controlbar.quickbutton.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.countFormatter
import com.moblin.android.common.various.formatShortDuration
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.streamingplatforms.Platform
import com.moblin.android.streamingplatforms.twitch.TwitchLoginView
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.network.OperationResult
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
import kotlinx.coroutines.launch
import com.moblin.android.various.model.banKickUser
import com.moblin.android.various.model.banTwitchUser
import com.moblin.android.various.model.modKickUser
import com.moblin.android.various.model.modTwitchUser
import com.moblin.android.various.model.twitchLogin
import com.moblin.android.various.model.unbanKickUser
import com.moblin.android.various.model.unbanTwitchUser
import com.moblin.android.various.model.unmodKickUser
import com.moblin.android.various.model.unmodTwitchUser
import com.moblin.android.various.model.unvipKickUser
import com.moblin.android.various.model.unvipTwitchUser
import com.moblin.android.various.model.vipKickUser
import com.moblin.android.various.model.vipTwitchUser

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
            is NetworkResponse.Success<*> -> ExecutorState.success
            NetworkResponse.AuthError -> ExecutorState.authError
            NetworkResponse.Error -> ExecutorState.error
        }
        scope.launch {
            delay(3000)
            state.value = ExecutorState.idle
        }
    }

    fun completedNoTimer(result: OperationResult) {
        state.value = when (result) {
            is NetworkResponse.Success<*> -> ExecutorState.idle
            NetworkResponse.AuthError -> ExecutorState.authError
            NetworkResponse.Error -> ExecutorState.error
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
    val palette = formPalette()

    fun handleState() {
        if (executor.state.value == ExecutorState.authError) {
            model.twitchLogin(stream = model.stream.value) {
                model.showModerationAuth.value = true
            }
        }
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
                    Text(text = localized("Success"), color = palette.green)
                }
            } else {
                Text(text = localized("Success"), color = palette.green)
            }
        }
        ExecutorState.authError -> {
            if (centerNonContent) {
                HCenter {
                    Text(text = localized("Not logged in"), color = palette.red)
                }
            } else {
                Text(text = localized("Not logged in"), color = palette.red)
            }
        }
        ExecutorState.error -> {
            if (centerNonContent) {
                HCenter {
                    Text(text = localized("Failed"), color = palette.red)
                }
            } else {
                Text(text = localized("Failed"), color = palette.red)
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
    val durationOptions: List<Int?> = listOf(null) + durations

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconAndTextLocalizedView(image = image, text = text)
        Spacer(Modifier.weight(1f))
        ExecutorView(model = model, executor = executor) {
            Box(modifier = Modifier.padding(end = 15.dp)) {
                Picker(
                    title = "",
                    selection = duration,
                    options = durationOptions,
                    text = { value ->
                        value?.let { formatShortDuration(seconds = it) } ?: localized("Off")
                    },
                    onChange = { duration = it },
                )
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

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val palette = formPalette()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = formBodyStyle.copy(color = palette.label),
        cursorBrush = SolidColor(palette.accent),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = localized(placeholder),
                        style = formBodyStyle,
                        color = palette.tertiaryLabel,
                    )
                }
                innerTextField()
            }
        },
    )
}

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

    fun canExecute(): Boolean {
        return username.trim().isNotEmpty()
    }

    fun executeKickAction(
        user: String,
        banReason: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        when (action) {
            ModActionType.ban -> model.banKickUser(
                user = user,
                duration = null,
                reason = banReason.ifEmpty { null },
                onComplete = onComplete,
            )
            ModActionType.timeout -> model.banKickUser(
                user = user,
                duration = timeoutDuration,
                onComplete = onComplete,
            )
            ModActionType.unban -> model.unbanKickUser(user = user, onComplete = onComplete)
            ModActionType.mod -> model.modKickUser(user = user, onComplete = onComplete)
            ModActionType.unmod -> model.unmodKickUser(user = user, onComplete = onComplete)
            ModActionType.vip -> model.vipKickUser(user = user, onComplete = onComplete)
            ModActionType.unvip -> model.unvipKickUser(user = user, onComplete = onComplete)
        }
    }

    fun executeTwitchAction(
        user: String,
        banReason: String,
        onComplete: (OperationResult) -> Unit,
    ) {
        when (action) {
            ModActionType.ban -> model.banTwitchUser(
                user = user,
                duration = null,
                reason = banReason.ifEmpty { null },
                onComplete = onComplete,
            )
            ModActionType.timeout -> model.banTwitchUser(
                user = user,
                duration = timeoutDuration,
                reason = null,
                onComplete = onComplete,
            )
            ModActionType.unban -> model.unbanTwitchUser(user = user, onComplete = onComplete)
            ModActionType.mod -> model.modTwitchUser(user = user, onComplete = onComplete)
            ModActionType.unmod -> model.unmodTwitchUser(user = user, onComplete = onComplete)
            ModActionType.vip -> model.vipTwitchUser(user = user, onComplete = onComplete)
            ModActionType.unvip -> model.unvipTwitchUser(user = user, onComplete = onComplete)
        }
    }

    fun executeAction(onComplete: (OperationResult) -> Unit) {
        val user = username.trim()
        val banReason = reason.trim()
        when (platform) {
            Platform.kick -> executeKickAction(user = user, banReason = banReason, onComplete = onComplete)
            Platform.twitch -> executeTwitchAction(user = user, banReason = banReason, onComplete = onComplete)
            else -> Unit
        }
    }

    NavigationLinkView(text = action.title(), image = action.image()) {
        Section(header = localized("Username")) {
            FormRow {
                FormTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Username",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (action == ModActionType.timeout) {
            Section(header = localized("Duration")) {
                FormRow {
                    Picker(
                        title = "",
                        selection = timeoutDuration,
                        options = timeoutPresets,
                        text = { formatShortDuration(seconds = it) },
                        onChange = { timeoutDuration = it },
                    )
                }
            }
        }
        if (action == ModActionType.ban) {
            Section(header = localized("Reason")) {
                FormRow {
                    FormTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        placeholder = "Reason",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        Section {
            HCenter {
                ExecutorView(model = model, executor = executor) {
                    TextButtonView(title = "Send") {
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
    val palette = formPalette()
    Section(
        header = localized(header),
        footerContent = {
            SwipeLeftToDeleteHelpView(kind = kind)
        },
    ) {
        options.forEach { option ->
            key(option.id) {
                FormRow {
                    FormTextField(
                        value = option.text,
                        onValueChange = { newValue ->
                            onOptionsChange(
                                options.map {
                                    if (it.id == option.id) it.copy(text = newValue) else it
                                },
                            )
                        },
                        placeholder = placeholder,
                        modifier = Modifier.weight(1f),
                    )
                    if (options.size > 2) {
                        SystemImage(
                            name = "minus.circle",
                            fontSize = 17.sp,
                            modifier = Modifier.clickable {
                                onOptionsChange(options.filterNot { it.id == option.id })
                            },
                            tint = palette.red,
                        )
                    }
                }
            }
        }
        if (options.size < maxCount) {
            AddButtonView {
                onOptionsChange(options + PollOption())
            }
        }
    }
}

@Composable
fun ChannelImageView(image: String?) {
    val url = image?.let { URI(it) }
    val appIcon = remember {
        com.moblin.android.platform.Bundle.image("AppIconNoBackground")?.asImageBitmap()
    }
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape),
    ) {
        if (url != null) {
            CacheAsyncImage(
                url = url,
                content = { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                placeholder = {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                },
            )
        } else if (appIcon != null) {
            Image(
                bitmap = appIcon,
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
                Text(category, fontSize = 12.sp)
                Text(title, fontSize = 12.sp)
            } else {
                Text(localized("Offline"), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        if (viewerCount != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SystemImage(name = "eye", fontSize = 12.sp)
                Text(countFormatter.format(viewerCount), fontSize = 12.sp)
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
    val darkMode = isSystemInDarkTheme()
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
    NavigationLink(
        destination = {
            Form(title = localized(text)) {
                content()
            }
        },
    ) {
        IconAndTextLocalizedView(image = image, text = text)
    }
}

@Composable
fun QuickButtonChatModerationView(
    model: Model = LocalModel.current,
    presentingModeration: Boolean,
    onPresentingModerationChange: (Boolean) -> Unit,
) {
    var platform by remember { mutableStateOf<Platform?>(null) }
    val showModerationAuth by model.showModerationAuth.collectAsState()
    val stream by model.stream.collectAsState()

    Form(
        title = localized("Moderation"),
        toolbar = {
            CloseToolbar(presentingModeration, onPresentingModerationChange)
        },
    ) {
        Section {
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
        }
        ShortcutSectionView {
            StreamingPlatformsShortcutView(model = model, stream = stream)
        }
    }

    if (showModerationAuth) {
        Sheet(onDismissRequest = { model.showModerationAuth.value = false }) {
            when (platform) {
                Platform.twitch -> TwitchLoginView(
                    model = model,
                    presenting = showModerationAuth,
                    onPresentingChange = { model.showModerationAuth.value = it },
                )
                else -> Unit
            }
        }
    }
}
