package com.moblin.android.view.settings.streams.stream.twitch

import androidx.compose.foundation.Image
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
import java.net.URI
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
        Unit
    }

    LaunchedEffect(Unit) {
        fetchDefaultCategories()
    }
    LaunchedEffect(searchText) {
        if (searchText.isEmpty()) {
            categories = emptyList()
            fetchDefaultCategories()
        } else {
            Unit
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
            Unit
            onDismiss()
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            category.boxArtUrl(width = 80, height = 100)?.let { boxArtUrl ->
                CacheAsyncImage(
                    url = URI(boxArtUrl),
                    content = { image ->
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 40.dp, height = 50.dp)
                                .clip(RoundedCornerShape(6.dp)),
                        )
                    },
                    placeholder = {},
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Follows", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.follows, onCheckedChange = { alerts.follows = it })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Subscriptions", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.subscriptions, onCheckedChange = { alerts.subscriptions = it })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Gift subscriptions", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.giftSubscriptions, onCheckedChange = { alerts.giftSubscriptions = it })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Resubscriptions", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.resubscriptions, onCheckedChange = { alerts.resubscriptions = it })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Rewards", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.rewards, onCheckedChange = { alerts.rewards = it })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Raids", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.raids, onCheckedChange = { alerts.raids = it })
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bits", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.cheers, onCheckedChange = { alerts.cheers = it })
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("Minimum bits"),
                    value = alerts.minimumCheerBits.toString(),
                    onSubmit = { alerts.minimumCheerBits = it.toIntOrNull() ?: 0 },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Watch streaks", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.watchStreaks, onCheckedChange = { alerts.watchStreaks = it })
                }
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = alerts.minimumWatchStreak.toString(),
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
                                    alerts.minimumWatchStreak = value
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Shared chat", modifier = Modifier.weight(1f))
                    Switch(checked = alerts.sharedChat, onCheckedChange = { alerts.sharedChat = it })
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
    Unit
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
        stream.twitchChannelName = value
        if (stream.enabled) {
            Unit
        }
    }

    fun submitChannelId(value: String) {
        stream.twitchChannelId = value
        if (stream.enabled) {
            Unit
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
        Unit
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
                            model.showTwitchAuth.value = true
                            Unit
                        }
                    } else {
                        TextButtonView("Logout") {
                            Unit
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
                    value = stream.twitchChannelName,
                    onSubmit = { submitChannelName(it) },
                    capitalize = true,
                )
                Text("The name of your channel.")
            }
            item {
                TextEditNavigationView(
                    title = localized("Channel id"),
                    value = stream.twitchChannelId,
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
