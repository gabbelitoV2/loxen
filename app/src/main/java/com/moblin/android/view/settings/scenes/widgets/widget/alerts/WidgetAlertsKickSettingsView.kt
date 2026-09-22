package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.verticalAlignment
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.moblin.android.streamingplatforms.kick.KickPusherGiftedSubscriptionsEvent
import com.moblin.android.streamingplatforms.kick.KickPusherKickGift
import com.moblin.android.streamingplatforms.kick.KickPusherKickSender
import com.moblin.android.streamingplatforms.kick.KickPusherKicksGiftedEvent
import com.moblin.android.streamingplatforms.kick.KickPusherRewardRedeemedEvent
import com.moblin.android.streamingplatforms.kick.KickPusherStreamHostEvent
import com.moblin.android.streamingplatforms.kick.KickPusherSubscriptionEvent
import com.moblin.android.various.model.Model
import com.moblin.android.various.settings.SettingsWidgetAlertsAlert
import com.moblin.android.various.settings.SettingsWidgetAlertsCheerBitsAlertOperator
import com.moblin.android.various.settings.SettingsWidgetAlertsKick
import com.moblin.android.various.settings.SettingsWidgetAlertsKickGiftsAlert
import com.moblin.android.various.settings.cheerBitsAlertOperators
import com.moblin.android.various.utils.makeOffsets
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KickSubscriptionsView(model: Model, alert: SettingsWidgetAlertsAlert) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Subscriptions")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        }
                    )
                }
            }
            item {
                AlertMediaView(alert = alert)
            }
            item {
                AlertPositionView(alert = alert)
            }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color()
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight
                )
            }
            item {
                AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay)
            }
            item {
                TextButtonView("Test") {
                    val event = KickPusherSubscriptionEvent(
                        username = alertTestNames.random(),
                        months = (1..12).random()
                    )
                    model.testAlert(TODO("Model test alert case for kickSubscription"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KickGiftedSubscriptionsView(model: Model, alert: SettingsWidgetAlertsAlert) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Gift subscriptions")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        }
                    )
                }
            }
            item {
                AlertMediaView(alert = alert)
            }
            item {
                AlertPositionView(alert = alert)
            }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color()
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight
                )
            }
            item {
                AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay)
            }
            item {
                TextButtonView("Test") {
                    val event = KickPusherGiftedSubscriptionsEvent(
                        giftedUsernames = listOf("1", "2"),
                        gifterUsername = alertTestNames.random(),
                        gifterTotal = (1..50).random()
                    )
                    model.testAlert(TODO("Model test alert case for kickGiftedSubscriptions"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KickHostsView(model: Model, alert: SettingsWidgetAlertsAlert) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Hosts")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        }
                    )
                }
            }
            item {
                AlertMediaView(alert = alert)
            }
            item {
                AlertPositionView(alert = alert)
            }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color()
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight
                )
            }
            item {
                AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay)
            }
            item {
                TextButtonView("Test") {
                    val event = KickPusherStreamHostEvent(
                        hostUsername = alertTestNames.random(),
                        numberViewers = (1..1000).random()
                    )
                    model.testAlert(TODO("Model test alert case for kickHost"))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KickRewardsView(model: Model, alert: SettingsWidgetAlertsAlert) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Rewards")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        }
                    )
                }
            }
            item {
                AlertMediaView(alert = alert)
            }
            item {
                AlertPositionView(alert = alert)
            }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color()
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight
                )
            }
            item {
                AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay)
            }
            item {
                TextButtonView("Test") {
                    val event = KickPusherRewardRedeemedEvent(
                        rewardTitle = "Test Reward",
                        username = alertTestNames.random(),
                        userInput = ""
                    )
                    model.testAlert(TODO("Model test alert case for kickReward"))
                }
            }
        }
    }
}

private fun formatKickGiftTitle(amount: Int, comparisonOperator: String): String {
    val amountText = countFormatter.format(amount)
    return when (SettingsWidgetAlertsCheerBitsAlertOperator.fromRawValue(comparisonOperator)) {
        SettingsWidgetAlertsCheerBitsAlertOperator.EQUAL -> "Kicks $amountText"
        SettingsWidgetAlertsCheerBitsAlertOperator.GREATER_EQUAL -> "Kicks $amountText+"
        else -> ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KickGiftView(
    model: Model,
    alert: SettingsWidgetAlertsAlert,
    kickGift: SettingsWidgetAlertsKickGiftsAlert,
    amount: Int,
    onAmountChange: (Int) -> Unit,
    comparisonOperator: String,
    onComparisonOperatorChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(comparisonOperator) {
        val operator = SettingsWidgetAlertsCheerBitsAlertOperator.fromRawValue(comparisonOperator)
        kickGift.comparisonOperator = operator ?: SettingsWidgetAlertsCheerBitsAlertOperator.GREATER_EQUAL
        model.updateAlertsSettings()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(formatKickGiftTitle(kickGift.amount, kickGift.comparisonOperator.rawValue))
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(localized("Enabled"), modifier = Modifier.weight(1f))
                    Switch(
                        checked = alert.enabled,
                        onCheckedChange = { value ->
                            alert.enabled = value
                            model.updateAlertsSettings()
                        }
                    )
                }
            }
            item {
                TextEditNavigationView(
                    title = localized("Amount"),
                    value = amount.toString(),
                    onChange = { value ->
                        if (value.toIntOrNull() == null) {
                            localized("Not a number")
                        } else {
                            null
                        }
                    },
                    onSubmit = { value ->
                        val newAmount = value.toIntOrNull()
                        if (newAmount != null) {
                            onAmountChange(newAmount)
                            kickGift.amount = newAmount
                            model.updateAlertsSettings()
                        }
                    },
                    keyboardType = KeyboardType.Number
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { value -> expanded = value }
                ) {
                    OutlinedTextField(
                        value = comparisonOperator,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(localized("Comparison")) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        cheerBitsAlertOperators.forEach { operator ->
                            DropdownMenuItem(
                                text = { Text(operator) },
                                onClick = {
                                    onComparisonOperatorChange(operator)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                AlertMediaView(alert = alert)
            }
            item {
                AlertPositionView(alert = alert)
            }
            item {
                AlertColorsView(
                    alert = alert,
                    textColor = alert.textColor.color(),
                    accentColor = alert.accentColor.color()
                )
            }
            item {
                AlertFontView(
                    alert = alert,
                    fontSize = alert.fontSize.toFloat(),
                    fontDesign = alert.fontDesign,
                    fontWeight = alert.fontWeight
                )
            }
            item {
                AlertTextToSpeechView(alert = alert, ttsDelay = alert.textToSpeechDelay)
            }
            item {
                TextButtonView("Test") {
                    val event = KickPusherKicksGiftedEvent(
                        message = "",
                        sender = KickPusherKickSender(
                            id = 1,
                            username = alertTestNames.random()
                        ),
                        gift = KickPusherKickGift(name = "Kicks", amount = kickGift.amount)
                    )
                    model.testAlert(TODO("Model test alert case for kickKicks"))
                }
            }
        }
    }
}

@Composable
private fun KickGiftItemView(
    alert: SettingsWidgetAlertsAlert,
    kickGift: SettingsWidgetAlertsKickGiftsAlert,
    onNavigate: (String) -> Unit
) {
    var amount by remember { mutableStateOf(kickGift.amount) }
    var comparisonOperator by remember { mutableStateOf(kickGift.comparisonOperator.rawValue) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        DraggableItemPrefixView()
        Text(
            text = formatKickGiftTitle(amount, comparisonOperator),
            modifier = Modifier.weight(1f).clickable { onNavigate("kickGift") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KickGiftsView(model: Model, kick: SettingsWidgetAlertsKick, onNavigate: (String) -> Unit) {
    fun deleteKickGift(offsets: List<Int>) {
        offsets.sortedDescending().forEach { offset ->
            kick.kickGifts.removeAt(offset)
        }
        model.updateAlertsSettings()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Kicks")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(items = kick.kickGifts, key = { kickGift -> kickGift.id }) { kickGift ->
                KickGiftItemView(
                    alert = kickGift.alert,
                    kickGift = kickGift,
                    onNavigate = onNavigate
                )
                TODO("contextMenuDeleteButton has no Compose counterpart")
            }
            item {
                CreateButtonView {
                    kick.kickGifts.add(SettingsWidgetAlertsKickGiftsAlert())
                    model.updateAlertsSettings()
                }
            }
            item {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("The first item that matches kicks amount will be played."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = "an item")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetAlertsKickSettingsView(
    model: Model,
    kick: SettingsWidgetAlertsKick,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(localized("Kick")) })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    text = localized("Subscriptions"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("kickSubscriptions") }
                        .padding(vertical = 12.dp)
                )
            }
            item {
                Text(
                    text = localized("Gift subscriptions"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("kickGiftedSubscriptions") }
                        .padding(vertical = 12.dp)
                )
            }
            item {
                Text(
                    text = localized("Hosts"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("kickHosts") }
                        .padding(vertical = 12.dp)
                )
            }
            item {
                Text(
                    text = localized("Rewards"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("kickRewards") }
                        .padding(vertical = 12.dp)
                )
            }
            item {
                Text(
                    text = localized("Kicks"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("kickGifts") }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
