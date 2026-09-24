package com.moblin.android.view.settings.streams.stream.kick

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Button
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.LocalNavigator
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Sheet
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formPalette
import com.moblin.android.streamingplatforms.kick.KickCategory
import com.moblin.android.streamingplatforms.kick.KickLoginView
import com.moblin.android.streamingplatforms.kick.KickUser
import com.moblin.android.streamingplatforms.kick.getKickChannelInfo
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.createKickApi
import com.moblin.android.various.model.fetchKickCategories
import com.moblin.android.various.model.getKickStreamInfo
import com.moblin.android.various.model.kickAccessTokenUpdated
import com.moblin.android.various.model.kickChannelNameUpdated
import com.moblin.android.various.model.kickLogin
import com.moblin.android.various.model.kickLogout
import com.moblin.android.various.model.searchKickCategories
import com.moblin.android.various.model.setKickStreamCategory
import com.moblin.android.various.model.setKickStreamTitle
import com.moblin.android.various.settings.SettingsKickAlerts
import com.moblin.android.various.network.NetworkResponse
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.view.utils.TextEditView
import java.net.URI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@Composable
private fun AuthenticationView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onLoggedIn: () -> Unit
) {
    var presentingWebView by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!stream.kickLoggedIn) {
            TextButtonView("Login") {
                presentingWebView = true
                model.kickLogin(stream = stream) {
                    onLoggedIn()
                }
            }
        } else {
            TextButtonView("Logout") {
                model.kickLogout(stream = stream)
            }
        }
    }
    Sheet(isPresented = presentingWebView, onDismissRequest = { presentingWebView = false }) {
        KickLoginView(
            presenting = presentingWebView,
            onPresentingChange = { presentingWebView = it }
        ) { accessToken ->
            model.kickAuthOnComplete?.invoke(accessToken)
        }
    }
}

@Composable
private fun CategoryButton(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    category: KickCategory,
    onDismiss: () -> Unit
) {
    Button(action = {
        val categoryId = category.id.toIntOrNull() ?: return@Button
        model.setKickStreamCategory(stream = stream, categoryId = categoryId)
        onDismiss()
    }) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val url = category.src?.let { runCatching { URI(it) }.getOrNull() }
            if (url != null) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 50.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    CacheAsyncImage(
                        url = url,
                        content = { bitmap ->
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        },
                        placeholder = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray.copy(alpha = 0.3f))
                            )
                        }
                    )
                }
            }
            Text(category.name)
        }
    }
}

@Composable
private fun KickCategoryPickerView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var categories by remember { mutableStateOf<List<KickCategory>>(emptyList()) }

    fun fetchDefaultCategories() {
        val categoryNames = listOf("IRL", "Just Chatting", "Slots & Casino")
        for (categoryName in categoryNames) {
            model.fetchKickCategories(stream = stream, query = categoryName) { result ->
                val category = result?.firstOrNull()
                if (category != null) {
                    categories = categories + category
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchDefaultCategories()
    }

    Form(title = "Category") {
        Section {
            val palette = formPalette()
            BasicTextField(
                value = searchText,
                onValueChange = { newValue ->
                    val changed = newValue != searchText
                    searchText = newValue
                    if (changed) {
                        if (newValue.isEmpty()) {
                            categories = emptyList()
                            fetchDefaultCategories()
                        } else {
                            model.searchKickCategories(stream = stream, query = newValue) { result ->
                                categories = result ?: emptyList()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = formBodyStyle.copy(color = palette.label),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchText.isEmpty()) {
                            Text(text = localized("Search"), style = formBodyStyle, color = palette.tertiaryLabel)
                        }
                        innerTextField()
                    }
                }
            )
        }
        Section {
            for (category in categories) {
                key(category.id) {
                    CategoryButton(
                        model = model,
                        stream = stream,
                        category = category,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun KickStreamLiveSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    title: String?,
    onTitleChange: (String?) -> Unit,
    category: String?,
    onCategoryChange: (String?) -> Unit,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    NavigationLink(
        destination = {
            TextEditView(
                title = localized("Title"),
                value = title ?: "",
                onSubmit = { value ->
                    model.setKickStreamTitle(stream = stream, title = value) {}
                }
            )
        }
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
            val navigator = LocalNavigator.current
            KickCategoryPickerView(model = model, stream = stream, onDismiss = { navigator?.pop() })
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KickAlertsSettingsView(
    title: String,
    alerts: SettingsKickAlerts,
    showBans: Boolean
) {
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subscriptions", modifier = Modifier.weight(1f))
                Switch(
                    checked = alerts.subscriptions,
                    onCheckedChange = { alerts.subscriptions = it }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gift subscriptions", modifier = Modifier.weight(1f))
                Switch(
                    checked = alerts.giftedSubscriptions,
                    onCheckedChange = { alerts.giftedSubscriptions = it }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rewards", modifier = Modifier.weight(1f))
                Switch(
                    checked = alerts.rewards,
                    onCheckedChange = { alerts.rewards = it }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Hosts", modifier = Modifier.weight(1f))
                Switch(
                    checked = alerts.hosts,
                    onCheckedChange = { alerts.hosts = it }
                )
            }
            if (showBans) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bans and timeouts", modifier = Modifier.weight(1f))
                    Switch(
                        checked = alerts.bans,
                        onCheckedChange = { alerts.bans = it }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kicks", modifier = Modifier.weight(1f))
                Switch(
                    checked = alerts.kicks,
                    onCheckedChange = { alerts.kicks = it }
                )
            }
            TextEditNavigationView(
                title = localized("Minimum kicks"),
                value = alerts.minimumKicks.toString(),
                onSubmit = { value ->
                    alerts.minimumKicks = value.toIntOrNull() ?: 0
                }
            )
        }
    }
}

suspend fun loadKickStreamInfo(
    model: Model,
    stream: SettingsStream,
    loggedIn: Boolean,
    onChange: (String?, String?) -> Unit
) {
    onChange(null, null)
    if (!loggedIn) {
        return
    }
    delay(1000)
    model.getKickStreamInfo(stream = stream) { result ->
        when (result) {
            is NetworkResponse.Success -> onChange(result.value.title, result.value.categoryName)
            else -> onChange(null, null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamKickSettingsView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    var fetchingChannelInfo by remember { mutableStateOf(false) }
    var fetchChannelInfoFailed by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun loadStreamInfo() {
        scope.launch {
            loadKickStreamInfo(
                model = model,
                stream = stream,
                loggedIn = stream.kickLoggedIn
            ) { newTitle, newCategory ->
                title = newTitle
                category = newCategory
            }
        }
    }

    fun resetSettings() {
        stream.kickChannelName = ""
        stream.kickChannelId = null
        stream.kickSlug = null
        stream.kickChatroomChannelId = null
    }

    fun reloadConnectionsIfEnabled() {
        if (stream.enabled) {
            model.kickAccessTokenUpdated()
        }
    }

    fun fetchChannelInfo() {
        fetchingChannelInfo = true
        fetchChannelInfoFailed = false
        getKickChannelInfo(channelName = stream.kickChannelName) { channelInfo ->
            fetchingChannelInfo = false
            if (channelInfo != null) {
                fetchChannelInfoFailed = false
                stream.kickChannelId = channelInfo.chatroom.id.toString()
                stream.kickSlug = channelInfo.slug
                stream.kickChatroomChannelId = channelInfo.chatroom.channel_id.toString()
                loadStreamInfo()
            } else {
                fetchChannelInfoFailed = true
            }
            reloadConnectionsIfEnabled()
        }
    }

    fun submitChannelName(value: String) {
        resetSettings()
        stream.kickChannelName = value
        fetchChannelInfo()
        if (stream.enabled && stream.kickChannelName.isEmpty()) {
            model.kickChannelNameUpdated()
        }
    }

    fun handleUser(data: KickUser?) {
        if (data != null) {
            resetSettings()
            stream.kickChannelName = data.username
            fetchChannelInfo()
        } else {
            reloadConnectionsIfEnabled()
        }
    }

    fun onLoggedIn() {
        model.createKickApi(stream = stream).getUser { data ->
            handleUser(data = data)
        }
    }

    LaunchedEffect(Unit) {
        loadStreamInfo()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Kick") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AuthenticationView(
                model = model,
                stream = stream,
                onLoggedIn = { onLoggedIn() }
            )
            TextEditNavigationView(
                title = localized("Channel name"),
                value = stream.kickChannelName,
                onChange = { null },
                onSubmit = { value -> submitChannelName(value) }
            )
            if (fetchingChannelInfo) {
                Text("Fetching channel info...")
            } else if (fetchChannelInfoFailed) {
                Text("Channel not found on kick.com.", color = Color.Red)
            }
            if (stream.kickLoggedIn) {
                KickStreamLiveSettingsView(
                    model = model,
                    stream = stream,
                    title = title,
                    onTitleChange = { title = it },
                    category = category,
                    onCategoryChange = { category = it },
                    onNavigate = onNavigate
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Alerts")
                NavigationLink(
                    destination = {
                        KickAlertsSettingsView(
                            title = localized("Chat"),
                            alerts = stream.kickChatAlerts,
                            showBans = true
                        )
                    }
                ) {
                    Text("Chat")
                }
                NavigationLink(
                    destination = {
                        KickAlertsSettingsView(
                            title = localized("Toasts"),
                            alerts = stream.kickToastAlerts,
                            showBans = false
                        )
                    }
                ) {
                    Text("Toasts")
                }
            }
        }
    }
}
