package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.LocalOnNavigate
import com.moblin.android.common.various.color
import com.moblin.android.common.various.countFormatter
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.FormRow
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
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
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemPrefixView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView

@Composable
private fun KickSubscriptionsView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    Form(title = localized("Subscriptions")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = alert.enabled,
                onChange = { value ->
                    alert.enabled = value
                    model.updateAlertsSettings()
                }
            )
        }
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color()
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight
        )
        AlertTextToSpeechView(alert = alert)
        Section {
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

@Composable
private fun KickGiftedSubscriptionsView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    Form(title = localized("Gift subscriptions")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = alert.enabled,
                onChange = { value ->
                    alert.enabled = value
                    model.updateAlertsSettings()
                }
            )
        }
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color()
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight
        )
        AlertTextToSpeechView(alert = alert)
        Section {
            TextButtonView("Test") {
                val event = KickPusherGiftedSubscriptionsEvent(
                    gifted_usernames = listOf("1", "2"),
                    gifter_username = alertTestNames.random(),
                    gifter_total = (1..50).random()
                )
                model.testAlert(TODO("Model test alert case for kickGiftedSubscriptions"))
            }
        }
    }
}

@Composable
private fun KickHostsView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    Form(title = localized("Hosts")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = alert.enabled,
                onChange = { value ->
                    alert.enabled = value
                    model.updateAlertsSettings()
                }
            )
        }
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color()
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight
        )
        AlertTextToSpeechView(alert = alert)
        Section {
            TextButtonView("Test") {
                val event = KickPusherStreamHostEvent(
                    host_username = alertTestNames.random(),
                    number_viewers = (1..1000).random()
                )
                model.testAlert(TODO("Model test alert case for kickHost"))
            }
        }
    }
}

@Composable
private fun KickRewardsView(model: Model = LocalModel.current, alert: SettingsWidgetAlertsAlert) {
    Form(title = localized("Rewards")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = alert.enabled,
                onChange = { value ->
                    alert.enabled = value
                    model.updateAlertsSettings()
                }
            )
        }
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color()
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight
        )
        AlertTextToSpeechView(alert = alert)
        Section {
            TextButtonView("Test") {
                val event = KickPusherRewardRedeemedEvent(
                    reward_title = "Test Reward",
                    username = alertTestNames.random(),
                    user_input = ""
                )
                model.testAlert(TODO("Model test alert case for kickReward"))
            }
        }
    }
}

private fun formatKickGiftTitle(amount: Int, comparisonOperator: String): String {
    val amountText = countFormatter.format(amount)
    return when (SettingsWidgetAlertsCheerBitsAlertOperator.fromRawValue(comparisonOperator)) {
        SettingsWidgetAlertsCheerBitsAlertOperator.equal -> "Kicks $amountText"
        SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual -> "Kicks $amountText+"
        else -> ""
    }
}

@Composable
private fun KickGiftView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    kickGift: SettingsWidgetAlertsKickGiftsAlert,
    amount: Int,
    onAmountChange: (Int) -> Unit,
    comparisonOperator: String,
    onComparisonOperatorChange: (String) -> Unit
) {
    Form(title = formatKickGiftTitle(kickGift.amount, kickGift.comparisonOperator.rawValue)) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = alert.enabled,
                onChange = { value ->
                    alert.enabled = value
                    model.updateAlertsSettings()
                }
            )
        }
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
        Picker(
            title = localized("Comparison"),
            selection = comparisonOperator,
            options = cheerBitsAlertOperators,
            onChange = { value ->
                onComparisonOperatorChange(value)
                kickGift.comparisonOperator =
                    SettingsWidgetAlertsCheerBitsAlertOperator.fromRawValue(value)
                        ?: SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual
                model.updateAlertsSettings()
            }
        )
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color()
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight
        )
        AlertTextToSpeechView(alert = alert)
        Section {
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

@Composable
private fun KickGiftItemView(
    alert: SettingsWidgetAlertsAlert,
    kickGift: SettingsWidgetAlertsKickGiftsAlert,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    val amount = remember { mutableStateOf(kickGift.amount) }
    val comparisonOperator = remember { mutableStateOf(kickGift.comparisonOperator.rawValue) }
    FormRow {
        DraggableItemPrefixView()
        NavigationLink(
            destination = {
                KickGiftView(
                    alert = alert,
                    kickGift = kickGift,
                    amount = amount.value,
                    onAmountChange = { amount.value = it },
                    comparisonOperator = comparisonOperator.value,
                    onComparisonOperatorChange = { comparisonOperator.value = it }
                )
            }
        ) {
            Text(formatKickGiftTitle(amount.value, comparisonOperator.value))
        }
    }
}

@Composable
private fun KickGiftsView(
    model: Model = LocalModel.current,
    kick: SettingsWidgetAlertsKick,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    fun deleteKickGift(offsets: List<Int>) {
        kick.kickGifts = kick.kickGifts.filterIndexed { index, _ -> index !in offsets }
        model.updateAlertsSettings()
    }

    Form(title = localized("Kicks")) {
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("The first item that matches kicks amount will be played."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = "an item")
                }
            }
        ) {
            kick.kickGifts.forEach { kickGift ->
                key(kickGift.id) {
                    KickGiftItemView(
                        alert = kickGift.alert,
                        kickGift = kickGift,
                        onNavigate = onNavigate
                    )
                }
            }
            CreateButtonView {
                kick.kickGifts = kick.kickGifts + SettingsWidgetAlertsKickGiftsAlert()
                model.updateAlertsSettings()
            }
        }
    }
}

@Composable
fun WidgetAlertsKickSettingsView(
    model: Model = LocalModel.current,
    kick: SettingsWidgetAlertsKick,
    onNavigate: (String) -> Unit = LocalOnNavigate.current
) {
    Form(title = localized("Kick")) {
        Section {
            NavigationLink(
                destination = {
                    KickSubscriptionsView(alert = kick.subscriptions)
                }
            ) {
                Text(localized("Subscriptions"))
            }
            NavigationLink(
                destination = {
                    KickGiftedSubscriptionsView(alert = kick.giftedSubscriptions)
                }
            ) {
                Text(localized("Gift subscriptions"))
            }
            NavigationLink(
                destination = {
                    KickHostsView(alert = kick.hosts)
                }
            ) {
                Text(localized("Hosts"))
            }
            NavigationLink(
                destination = {
                    KickRewardsView(alert = kick.rewards)
                }
            ) {
                Text(localized("Rewards"))
            }
            NavigationLink(
                destination = {
                    KickGiftsView(kick = kick, onNavigate = onNavigate)
                }
            ) {
                Text(localized("Kicks"))
            }
        }
    }
}
