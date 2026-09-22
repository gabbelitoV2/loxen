package com.moblin.android.view.settings.streams.stream.kick

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.kick.KickCategory
import com.moblin.android.streamingplatforms.kick.KickLoginView
import com.moblin.android.streamingplatforms.kick.KickUser
import com.moblin.android.streamingplatforms.kick.getKickChannelInfo
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsKickAlerts
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.view.settings.streams.stream.GrayTextView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticationView(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    onLoggedIn: () -> Unit
) {
    var presentingWebView by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
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
    if (presentingWebView) {
        ModalBottomSheet(
            onDismissRequest = { presentingWebView = false },
            sheetState = sheetState
        ) {
            KickLoginView(
                presenting = presentingWebView,
                onPresentingChange = { presentingWebView = it }
            ) { accessToken ->
                model.kickAuthOnComplete?.invoke(accessToken)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryButton(
    model: Model = LocalModel.current,
    stream: SettingsStream,
    category: KickCategory,
    onDismiss: () -> Unit
) {
    Button(onClick = {
        val categoryId = category.id.toIntOrNull() ?: return@Button
        model.setKickStreamCategory(stream = stream, categoryId = categoryId)
        onDismiss()
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val imageUrl = category.src
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(width = 40.dp, height = 50.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(Color.Gray.copy(alpha = 0.3f))
                )
            }
            Text(category.name)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(searchText) {
        if (searchText.isEmpty()) {
            categories = emptyList()
            fetchDefaultCategories()
        } else {
            model.searchKickCategories(stream = stream, query = searchText) { result ->
                categories = result ?: emptyList()
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Category") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            for (category in categories) {
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate("KickStreamTitle") }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
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
                .clickable { onNavigate("KickStreamCategory") }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
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
        result.fold(
            onSuccess = { info -> onChange(info.title, info.categoryName) },
            onFailure = { onChange(null, null) }
        )
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
            handleUser(data)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("KickAlertsSettingsViewChat") }
                        .padding(vertical = 12.dp)
                ) {
                    Text("Chat")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("KickAlertsSettingsViewToasts") }
                        .padding(vertical = 12.dp)
                ) {
                    Text("Toasts")
                }
            }
        }
    }
}
