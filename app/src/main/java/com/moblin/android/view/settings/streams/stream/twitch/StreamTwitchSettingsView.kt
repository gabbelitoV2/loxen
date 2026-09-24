package com.moblin.android.view.settings.streams.stream.twitch

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.*
import com.moblin.android.streamingplatforms.twitch.TwitchApiGameData
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsTwitchAlerts
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.settings.streams.stream.TokenExpiresInView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextEditView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import java.net.URI
import com.moblin.android.various.model.fetchTwitchGames
import com.moblin.android.various.model.getTwitchChannelInformation
import com.moblin.android.various.model.getTwitchTokenExpiresIn
import com.moblin.android.various.model.searchTwitchCategories
import com.moblin.android.various.model.setTwitchStreamCategory
import com.moblin.android.various.model.setTwitchStreamTitle
import com.moblin.android.various.model.twitchChannelIdUpdated
import com.moblin.android.various.model.twitchChannelNameUpdated
import com.moblin.android.various.model.twitchLogin
import com.moblin.android.various.model.twitchLogout

@Composable
fun TwitchStreamLiveSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    title: String?,
    category: String?,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    NavigationLink(
        destination = {
            TextEditView(
                title = localized("Title"),
                value = title ?: "",
                onSubmit = { value ->
                    model.setTwitchStreamTitle(stream, value)
                },
            )
        },
    ) {
        Text(localized("Title"))
        Spacer(Modifier.weight(1f))
        if (title != null) {
            GrayTextView(text = title)
        } else {
            CircularProgressIndicator()
        }
    }
    NavigationLink(
        destination = {
            TwitchCategoryPickerView(stream = stream, onDismiss = rememberDismiss())
        },
    ) {
        Text(localized("Category"))
        Spacer(Modifier.weight(1f))
        if (category != null) {
            GrayTextView(text = category)
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun TwitchCategoryPickerView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onDismiss: () -> Unit,
) {
    var searchText by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<TwitchApiGameData>>(emptyList()) }

    fun fetchDefaultCategories() {
        model.fetchTwitchGames(stream, listOf("IRL", "Just Chatting", "Food & Drink")) { games ->
            categories = games ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        fetchDefaultCategories()
    }

    Form(title = "Category") {
        Section {
            BasicTextField(
                value = searchText,
                onValueChange = { value ->
                    searchText = value
                    if (value.isEmpty()) {
                        categories = emptyList()
                        fetchDefaultCategories()
                    } else {
                        model.searchTwitchCategories(stream, value) { result ->
                            categories = result ?: emptyList()
                        }
                    }
                },
                textStyle = formBodyStyle.copy(color = formPalette().label),
                singleLine = true,
                cursorBrush = SolidColor(formPalette().accent),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchText.isEmpty()) {
                            Text(
                                text = localized("Search"),
                                style = formBodyStyle,
                                color = formPalette().tertiaryLabel,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        Section {
            categories.forEach { category ->
                key(category.id) {
                    categoryButton(
                        category = category,
                        model = model,
                        stream = stream,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryButton(
    category: TwitchApiGameData,
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onDismiss: () -> Unit,
) {
    FormRow(
        onClick = {
            model.setTwitchStreamCategory(stream, category.id)
            onDismiss()
        },
    ) {
        category.boxArtUrl(width = 80, height = 100)?.let { boxArtUrl ->
            CacheAsyncImage(
                url = URI(boxArtUrl),
                content = { image ->
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .size(width = 40.dp, height = 50.dp),
                    )
                },
                placeholder = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp)).background(formPalette().gray.copy(alpha = 0.3f))
                            .size(width = 40.dp, height = 50.dp),
                    )
                },
            )
        }
        Text(category.name)
    }
}

@Composable
fun TwitchAlertsSettingsView(title: String, alerts: SettingsTwitchAlerts) {
    Form(title = title) {
        Section {
            Toggle(
                "Follows",
                isOn = binding({ alerts.follows }, { alerts.follows = it }),
            )
            Toggle(
                "Subscriptions",
                isOn = binding({ alerts.subscriptions }, { alerts.subscriptions = it }),
            )
            Toggle(
                "Gift subscriptions",
                isOn = binding({ alerts.giftSubscriptions }, { alerts.giftSubscriptions = it }),
            )
            Toggle(
                "Resubscriptions",
                isOn = binding({ alerts.resubscriptions }, { alerts.resubscriptions = it }),
            )
            Toggle(
                "Rewards",
                isOn = binding({ alerts.rewards }, { alerts.rewards = it }),
            )
            Toggle(
                "Raids",
                isOn = binding({ alerts.raids }, { alerts.raids = it }),
            )
            Toggle(
                "Bits",
                isOn = binding({ alerts.cheers }, { alerts.cheers = it }),
            )
            TextEditNavigationView(
                title = localized("Minimum bits"),
                value = alerts.minimumCheerBits.toString(),
                onSubmit = { alerts.minimumCheerBits = it.toIntOrNull() ?: 0 },
            )
            Toggle(
                "Watch streaks",
                isOn = binding({ alerts.watchStreaks }, { alerts.watchStreaks = it }),
            )
            Picker(
                title = "Minimum watch streak",
                selection = alerts.minimumWatchStreak,
                options = listOf(1, 3, 5, 10, 15, 20, 25),
                onChange = { alerts.minimumWatchStreak = it },
            )
        }
        Section(
            footerContent = {
                Text(localized("Also show events from other channels in a shared chat session."))
            },
        ) {
            Toggle(
                "Shared chat",
                isOn = binding({ alerts.sharedChat }, { alerts.sharedChat = it }),
            )
        }
    }
}

suspend fun loadTwitchStreamInfo(
    model: Model,
    stream: SettingsStream,
    loggedIn: Boolean,
    onChange: (String?, String?) -> Unit,
) {
    onChange(null, null)
    if (!loggedIn) {
        return
    }
    delay(1000)
    model.getTwitchChannelInformation(stream) { info ->
        onChange(info.title, info.game_name)
    }
}

@Composable
fun StreamTwitchSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    loggedInInitial: Boolean,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(loggedInInitial) }
    var title by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf<String?>(null) }
    var tokenExpiresIn by remember { mutableStateOf<Duration?>(null) }

    fun submitChannelName(value: String) {
        stream.twitchChannelName = value
        if (stream.enabled) {
            model.twitchChannelNameUpdated()
        }
    }

    fun submitChannelId(value: String) {
        stream.twitchChannelId = value
        if (stream.enabled) {
            model.twitchChannelIdUpdated()
        }
    }

    fun loadStreamInfo() {
        scope.launch {
            loadTwitchStreamInfo(model, stream, loggedIn) { newTitle, newCategory ->
                title = newTitle
                category = newCategory
            }
        }
    }

    fun loadTokenExpiresIn() {
        tokenExpiresIn = null
        if (!loggedIn) {
            return
        }
        model.getTwitchTokenExpiresIn(stream) {
            tokenExpiresIn = it?.let { duration -> duration.toNanos().nanoseconds }
        }
    }

    fun onLoggedIn() {
        loggedIn = true
        loadStreamInfo()
        loadTokenExpiresIn()
    }

    LaunchedEffect(Unit) {
        loadStreamInfo()
        loadTokenExpiresIn()
    }

    Form(title = "Twitch") {
        Section(
            footerContent = {
                TokenExpiresInView(expiresIn = tokenExpiresIn)
            },
        ) {
            if (!loggedIn) {
                TextButtonView("Login") {
                    model.twitchLogin(stream, { onLoggedIn() }) {
                        model.showTwitchAuth.value = true
                    }
                }
            } else {
                TextButtonView("Logout") {
                    model.twitchLogout(stream)
                    loggedIn = false
                    tokenExpiresIn = null
                }
            }
        }
        Section(
            footerContent = {
                Text(localized("The name of your channel."))
            },
        ) {
            TextEditNavigationView(
                title = localized("Channel name"),
                value = stream.twitchChannelName,
                onSubmit = { submitChannelName(it) },
                capitalize = true,
            )
        }
        Section {
            TextEditNavigationView(
                title = localized("Channel id"),
                value = stream.twitchChannelId,
                onSubmit = { submitChannelId(it) },
            )
        }
        if (loggedIn) {
            Section {
                TwitchStreamLiveSettingsView(
                    model = model,
                    stream = stream,
                    title = title,
                    category = category,
                    onNavigate = onNavigate,
                )
            }
        }
        Section(
            headerContent = {
                Text(localized("Alerts"))
            },
        ) {
            NavigationLink(
                destination = {
                    TwitchAlertsSettingsView(
                        title = localized("Chat"),
                        alerts = stream.twitchChatAlerts,
                    )
                },
            ) {
                Text(localized("Chat"))
            }
            NavigationLink(
                destination = {
                    TwitchAlertsSettingsView(
                        title = localized("Toasts"),
                        alerts = stream.twitchToastAlerts,
                    )
                },
            ) {
                Text(localized("Toasts"))
            }
        }
    }
}
