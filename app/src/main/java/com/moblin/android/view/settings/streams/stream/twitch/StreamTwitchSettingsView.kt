package com.moblin.android.view.settings.streams.stream.twitch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.twitch.TwitchApiGameData
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.settings.SettingsTwitchAlerts
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.settings.streams.stream.TokenExpiresInView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
fun TwitchStreamLiveSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    title: String?,
    category: String?,
    onNavigate: (String) -> Unit = LocalOnNavigate.current,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("title") }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Title")
            Spacer(Modifier.weight(1f))
            if (title != null) {
                GrayTextView(text = title)
            } else {
                CircularProgressIndicator()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("category") }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Category")
            Spacer(Modifier.weight(1f))
            if (category != null) {
                GrayTextView(text = category)
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwitchCategoryPickerView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onDismiss: () -> Unit,
) {
    var searchText by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<TwitchApiGameData>>(emptyList()) }

    fun fetchDefaultCategories() {
        val categoryNames = listOf("IRL", "Just Chatting", "Food & Drink")
        model.fetchTwitchGames(stream, categoryNames) { games ->
            categories = games ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        fetchDefaultCategories()
    }
    LaunchedEffect(searchText) {
        if (searchText.isEmpty()) {
            categories = emptyList()
            fetchDefaultCategories()
        } else {
            model.searchTwitchCategories(stream, searchText) { newCategories ->
                categories = newCategories ?: emptyList()
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Category") }) }) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
            items(categories) { category ->
                categoryButton(category = category, model = model, stream = stream, onDismiss = onDismiss)
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
    Button(
        onClick = {
            model.setTwitchStreamCategory(stream = stream, categoryId = category.id)
            onDismiss()
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            category.boxArtUrl(width = 80, height = 100)?.let { boxArtUrl ->
                CacheAsyncImage(
                    url = boxArtUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 40.dp, height = 50.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
            }
            Text(category.name)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwitchAlertsSettingsView(title: String, alerts: SettingsTwitchAlerts) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            item {
                val follows by alerts.follows.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Follows", modifier = Modifier.weight(1f))
                    Switch(checked = follows, onCheckedChange = { alerts.follows.value = it })
                }
            }
            item {
                val subscriptions by alerts.subscriptions.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Subscriptions", modifier = Modifier.weight(1f))
                    Switch(checked = subscriptions, onCheckedChange = { alerts.subscriptions.value = it })
                }
            }
            item {
                val giftSubscriptions by alerts.giftSubscriptions.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Gift subscriptions", modifier = Modifier.weight(1f))
                    Switch(checked = giftSubscriptions, onCheckedChange = { alerts.giftSubscriptions.value = it })
                }
            }
            item {
                val resubscriptions by alerts.resubscriptions.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Resubscriptions", modifier = Modifier.weight(1f))
                    Switch(checked = resubscriptions, onCheckedChange = { alerts.resubscriptions.value = it })
                }
            }
            item {
                val rewards by alerts.rewards.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Rewards", modifier = Modifier.weight(1f))
                    Switch(checked = rewards, onCheckedChange = { alerts.rewards.value = it })
                }
            }
            item {
                val raids by alerts.raids.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Raids", modifier = Modifier.weight(1f))
                    Switch(checked = raids, onCheckedChange = { alerts.raids.value = it })
                }
            }
            item {
                val cheers by alerts.cheers.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bits", modifier = Modifier.weight(1f))
                    Switch(checked = cheers, onCheckedChange = { alerts.cheers.value = it })
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("Minimum bits"),
                    value = alerts.minimumCheerBits.value.toString(),
                    onSubmit = { alerts.minimumCheerBits.value = it.toIntOrNull() ?: 0 },
                )
            }
            item {
                val watchStreaks by alerts.watchStreaks.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Watch streaks", modifier = Modifier.weight(1f))
                    Switch(checked = watchStreaks, onCheckedChange = { alerts.watchStreaks.value = it })
                }
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                val minimumWatchStreak by alerts.minimumWatchStreak.collectAsState()
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = minimumWatchStreak.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Minimum watch streak") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth().padding(16.dp),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(1, 3, 5, 10, 15, 20, 25).forEach { value ->
                            DropdownMenuItem(
                                text = { Text(value.toString()) },
                                onClick = {
                                    alerts.minimumWatchStreak.value = value
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                val sharedChat by alerts.sharedChat.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Shared chat", modifier = Modifier.weight(1f))
                    Switch(checked = sharedChat, onCheckedChange = { alerts.sharedChat.value = it })
                }
                Text("Also show events from other channels in a shared chat session.")
            }
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
        onChange(info.title, info.gameName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        stream.twitchChannelName.value = value
        if (stream.enabled.value) {
            model.twitchChannelNameUpdated()
        }
    }

    fun submitChannelId(value: String) {
        stream.twitchChannelId.value = value
        if (stream.enabled.value) {
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
        model.getTwitchTokenExpiresIn(stream) { newTokenExpiresIn ->
            tokenExpiresIn = newTokenExpiresIn
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

    Scaffold(topBar = { TopAppBar(title = { Text("Twitch") }) }) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (!loggedIn) {
                        TextButtonView("Login") {
                            model.twitchLogin(stream = stream, onComplete = { onLoggedIn() }) {
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
                    TokenExpiresInView(tokenExpiresIn)
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("Channel name"),
                    value = stream.twitchChannelName.value,
                    onSubmit = { submitChannelName(it) },
                    capitalize = true,
                )
                Text("The name of your channel.")
            }
            item {
                TextEditNavigationView(
                    title = localized("Channel id"),
                    value = stream.twitchChannelId.value,
                    onSubmit = { submitChannelId(it) },
                )
            }
            if (loggedIn) {
                item {
                    TwitchStreamLiveSettingsView(
                        model = model,
                        stream = stream,
                        title = title,
                        category = category,
                        onNavigate = onNavigate,
                    )
                }
            }
            item {
                Text("Alerts")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("chatAlerts") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Chat")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("toastAlerts") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Toasts")
                }
            }
        }
    }
}
