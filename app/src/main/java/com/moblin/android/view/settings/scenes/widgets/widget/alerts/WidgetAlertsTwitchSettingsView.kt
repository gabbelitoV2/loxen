package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.countFormatter
import com.moblin.android.localized
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelCheerEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubChannelRaidEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelFollowEvent
import com.moblin.android.streamingplatforms.twitch.TwitchEventSubNotificationChannelSubscribeEvent
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsStreamTwitchReward
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsCheerBitsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsCheerBitsAlertOperator
import com.moblin.android.various.settings.SettingsWidgetAlertsTwitch
import com.moblin.android.various.settings.cheerBitsAlertOperators
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import kotlin.random.random
import com.moblin.android.LocalModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwitchFollowsView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized("Follows")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        },
                    )
                }
            }
            item { AlertMediaView(alert = alert) }
            item { AlertPositionView(alert = alert) }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color(),
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight,
                )
            }
            item { AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay) }
            item {
                TextButtonView("Test") {
                    val event = TwitchEventSubNotificationChannelFollowEvent(
                        userName = alertTestNames.random(),
                    )
                    model.testAlert(TODO("testAlert with the twitchFollow alert case"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwitchSubscriptionsView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized("Subscriptions")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        },
                    )
                }
            }
            item { AlertMediaView(alert = alert) }
            item { AlertPositionView(alert = alert) }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color(),
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight,
                )
            }
            item { AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay) }
            item {
                TextButtonView("Test") {
                    val event = TwitchEventSubNotificationChannelSubscribeEvent(
                        userName = alertTestNames.random(),
                        tier = "2000",
                        isGift = false,
                        isPrime = false,
                    )
                    model.testAlert(TODO("testAlert with the twitchSubscribe alert case"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwitchRaidsView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized("Raids")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        },
                    )
                }
            }
            item { AlertMediaView(alert = alert) }
            item { AlertPositionView(alert = alert) }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color(),
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight,
                )
            }
            item { AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay) }
            item {
                TextButtonView("Test") {
                    val event = TwitchEventSubChannelRaidEvent(
                        fromBroadcasterUserId = "1234",
                        fromBroadcasterUserName = alertTestNames.random(),
                        viewers = (1 until 1000).random(),
                    )
                    model.testAlert(TODO("testAlert with the twitchRaid alert case"))
                }
            }
        }
    }
}

private fun formatTitle(bits: Int, comparisonOperator: String): String {
    val bitsText = countFormatter.format(bits)
    return when (SettingsWidgetAlertsCheerBitsAlertOperator.fromRawValue(comparisonOperator)) {
        SettingsWidgetAlertsCheerBitsAlertOperator.equal ->
            if (bits == 1) {
                "Cheer $bitsText bit"
            } else {
                "Cheer $bitsText bits"
            }
        SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual -> "Cheer $bitsText+ bits"
        else -> ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwitchCheerView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    cheerBit: SettingsWidgetAlertsCheerBitsAlert,
    bits: Int,
    comparisonOperator: String,
    onBitsChange: (Int) -> Unit,
    onComparisonOperatorChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(comparisonOperator) {
        val newComparisonOperator =
            SettingsWidgetAlertsCheerBitsAlertOperator.fromRawValue(comparisonOperator)
        cheerBit.comparisonOperator =
            newComparisonOperator ?: SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual
        model.updateAlertsSettings()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(formatTitle(cheerBit.bits, cheerBit.comparisonOperator.rawValue)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        },
                    )
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("Bits"),
                    value = bits.toString(),
                    onChange = { value ->
                        if (value.toIntOrNull() != null) {
                            null
                        } else {
                            localized("Not a number")
                        }
                    },
                    onSubmit = { value ->
                        val newBits = value.toIntOrNull()
                        if (newBits != null) {
                            onBitsChange(newBits)
                            cheerBit.bits = newBits
                            model.updateAlertsSettings()
                        }
                    },
                    keyboardType = KeyboardType.Number,
                )
            }
            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                ) {
                    OutlinedTextField(
                        value = comparisonOperator,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(localized("Comparison")) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        cheerBitsAlertOperators.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onComparisonOperatorChange(option)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
            item { AlertMediaView(alert = alert) }
            item { AlertPositionView(alert = alert) }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color(),
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight,
                )
            }
            item { AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay) }
            item {
                TextButtonView("Test") {
                    val event = TwitchEventSubChannelCheerEvent(
                        userName = alertTestNames.random(),
                        message = "A test message!",
                        bits = cheerBit.bits,
                    )
                    model.testAlert(TODO("testAlert with the twitchCheer alert case"))
                }
            }
        }
    }
}

@Composable
private fun TwitchCheerBitsItemView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    cheerBit: SettingsWidgetAlertsCheerBitsAlert,
) {
    var bits by remember { mutableStateOf(cheerBit.bits) }
    var comparisonOperator by remember { mutableStateOf(cheerBit.comparisonOperator.rawValue) }
    var showCheerView by remember { mutableStateOf(false) }
    if (showCheerView) {
        TwitchCheerView(
            model = model,
            alert = alert,
            cheerBit = cheerBit,
            bits = bits,
            comparisonOperator = comparisonOperator,
            onBitsChange = { bits = it },
            onComparisonOperatorChange = { comparisonOperator = it },
            onBack = { showCheerView = false },
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DraggableItemPrefixView()
            Text(
                text = formatTitle(bits, comparisonOperator),
                modifier = Modifier.weight(1f).clickable { showCheerView = true },
            )
        }
    }
}

private fun deleteCheerBit(
    twitch: SettingsWidgetAlertsTwitch,
    model: Model,
    offsets: Set<Int>,
) {
    offsets.sortedDescending().forEach { index ->
        if (index in twitch.cheerBits.indices) {
            twitch.cheerBits.removeAt(index)
        }
    }
    model.updateAlertsSettings()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwitchCheerBitsView(
    model: Model = LocalModel.current,
    twitch: SettingsWidgetAlertsTwitch,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized("Cheers")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            items(twitch.cheerBits) { cheerBit ->
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            makeOffsets(twitch.cheerBits, cheerBit.id)?.let { offsets ->
                                deleteCheerBit(twitch, model, offsets)
                            }
                        },
                    ),
                ) {
                    TwitchCheerBitsItemView(
                        model = model,
                        alert = cheerBit.alert,
                        cheerBit = cheerBit,
                    )
                }
            }
            item {
                CreateButtonView {
                    twitch.cheerBits.add(SettingsWidgetAlertsCheerBitsAlert())
                    model.updateAlertsSettings()
                }
            }
            item {
                Column {
                    Text(localized("The first item that matches cheered bits will be played."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = "an item")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwitchRewardView(
    model: Model = LocalModel.current,
    reward: SettingsStreamTwitchReward,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(reward.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = reward.alert.enabled,
                        onCheckedChange = { value ->
                            reward.alert.enabled = value
                            model.updateAlertsSettings()
                        },
                    )
                }
            }
            item { AlertMediaView(alert = reward.alert) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwitchRewardsView(
    model: Model = LocalModel.current,
    onBack: () -> Unit,
) {
    val stream by model.stream.collectAsState()
    var selectedReward by remember { mutableStateOf<SettingsStreamTwitchReward?>(null) }
    val reward = selectedReward
    if (reward != null) {
        TwitchRewardView(
            model = model,
            reward = reward,
            onBack = { selectedReward = null },
        )
    } else {
        LaunchedEffect(Unit) {
            model.fetchTwitchRewards()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(localized("Rewards")) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null)
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (stream.twitchRewards.isEmpty()) {
                    item {
                        Text(localized("No rewards found"))
                    }
                } else {
                    items(stream.twitchRewards) { twitchReward ->
                        Text(
                            text = twitchReward.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReward = twitchReward }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetAlertsTwitchSettingsView(
    model: Model = LocalModel.current,
    twitch: SettingsWidgetAlertsTwitch,
) {
    val database by model.database.collectAsState()
    var destination by remember { mutableStateOf<String?>(null) }
    when (destination) {
        "Follows" -> TwitchFollowsView(
            model = model,
            alert = twitch.follows,
            onBack = { destination = null },
        )
        "Subscriptions" -> TwitchSubscriptionsView(
            model = model,
            alert = twitch.subscriptions,
            onBack = { destination = null },
        )
        "Raids" -> TwitchRaidsView(
            model = model,
            alert = twitch.raids,
            onBack = { destination = null },
        )
        "Cheers" -> TwitchCheerBitsView(
            model = model,
            twitch = twitch,
            onBack = { destination = null },
        )
        "Rewards" -> TwitchRewardsView(
            model = model,
            onBack = { destination = null },
        )
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text(localized("Twitch")) })
                },
            ) { paddingValues ->
                LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    item {
                        Text(
                            text = localized("Follows"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { destination = "Follows" }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    item {
                        Text(
                            text = localized("Subscriptions"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { destination = "Subscriptions" }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    item {
                        Text(
                            text = localized("Raids"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { destination = "Raids" }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    item {
                        Text(
                            text = localized("Cheers"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { destination = "Cheers" }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                    if (database.debug.twitchRewards) {
                        item {
                            Text(
                                text = localized("Rewards"),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { destination = "Rewards" }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
