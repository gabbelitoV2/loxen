package com.moblin.android.view.settings.scenes.widgets.widget.alerts

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.input.KeyboardType
import com.moblin.android.LocalModel
import com.moblin.android.common.various.color
import com.moblin.android.common.various.countFormatter
import com.moblin.android.localized
import com.moblin.android.platform.swiftui.ForEach
import com.moblin.android.platform.swiftui.Form
import com.moblin.android.platform.swiftui.IndexSet
import com.moblin.android.platform.swiftui.NavigationLink
import com.moblin.android.platform.swiftui.Picker
import com.moblin.android.platform.swiftui.Section
import com.moblin.android.platform.swiftui.Toggle
import com.moblin.android.platform.swiftui.binding
import com.moblin.android.platform.swiftui.moving
import com.moblin.android.platform.swiftui.removing
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
import com.moblin.android.view.utils.ContextMenuDeleteButton
import com.moblin.android.view.utils.CreateButtonView
import com.moblin.android.view.utils.DraggableItemTextView
import com.moblin.android.view.utils.SwipeLeftToDeleteHelpView
import com.moblin.android.view.utils.TextButtonView
import com.moblin.android.view.utils.TextEditNavigationView
import com.moblin.android.various.model.fetchTwitchRewards

@Composable
private fun TwitchFollowsView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onBack: () -> Unit,
) {
    Form(title = localized("Follows")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = binding(
                    get = { alert.enabled },
                    set = { value ->
                        alert.enabled = value
                        model.updateAlertsSettings()
                    },
                ),
            )
        }
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color(),
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight,
        )
        AlertTextToSpeechView(alert = alert)
        Section {
            TextButtonView("Test") {
                val event = TwitchEventSubNotificationChannelFollowEvent(
                    user_name = alertTestNames.random(),
                )
                model.testAlert(TODO("testAlert with the twitchFollow alert case"))
            }
        }
    }
}

@Composable
private fun TwitchSubscriptionsView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onBack: () -> Unit,
) {
    Form(title = localized("Subscriptions")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = binding(
                    get = { alert.enabled },
                    set = { value ->
                        alert.enabled = value
                        model.updateAlertsSettings()
                    },
                ),
            )
        }
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color(),
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight,
        )
        AlertTextToSpeechView(alert = alert)
        Section {
            TextButtonView("Test") {
                val event = TwitchEventSubNotificationChannelSubscribeEvent(
                    user_name = alertTestNames.random(),
                    tier = "2000",
                    is_gift = false,
                    is_prime = false,
                )
                model.testAlert(TODO("testAlert with the twitchSubscribe alert case"))
            }
        }
    }
}

@Composable
private fun TwitchRaidsView(
    model: Model = LocalModel.current,
    alert: SettingsWidgetAlertsAlert,
    onBack: () -> Unit,
) {
    Form(title = localized("Raids")) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = binding(
                    get = { alert.enabled },
                    set = { value ->
                        alert.enabled = value
                        model.updateAlertsSettings()
                    },
                ),
            )
        }
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color(),
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight,
        )
        AlertTextToSpeechView(alert = alert)
        Section {
            TextButtonView("Test") {
                val event = TwitchEventSubChannelRaidEvent(
                    from_broadcaster_user_id = "1234",
                    from_broadcaster_user_name = alertTestNames.random(),
                    viewers = (1 until 1000).random(),
                )
                model.testAlert(TODO("testAlert with the twitchRaid alert case"))
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
    Form(title = formatTitle(cheerBit.bits, cheerBit.comparisonOperator.rawValue)) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = binding(
                    get = { alert.enabled },
                    set = { value ->
                        alert.enabled = value
                        model.updateAlertsSettings()
                    },
                ),
            )
        }
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
        Picker(
            title = localized("Comparison"),
            selection = comparisonOperator,
            options = cheerBitsAlertOperators,
            onChange = { value ->
                onComparisonOperatorChange(value)
                cheerBit.comparisonOperator =
                    SettingsWidgetAlertsCheerBitsAlertOperator.fromRawValue(value)
                        ?: SettingsWidgetAlertsCheerBitsAlertOperator.greaterEqual
                model.updateAlertsSettings()
            },
        )
        AlertMediaView(alert = alert)
        AlertPositionView(alert = alert)
        AlertColorsView(
            alert = alert,
            textColor = alert.textColor.color(),
            accentColor = alert.accentColor.color(),
        )
        AlertFontView(
            alert = alert,
            fontSize = alert.fontSize.toFloat(),
            fontDesign = alert.fontDesign,
            fontWeight = alert.fontWeight,
        )
        AlertTextToSpeechView(alert = alert)
        Section {
            TextButtonView("Test") {
                val event = TwitchEventSubChannelCheerEvent(
                    user_name = alertTestNames.random(),
                    message = "A test message!",
                    bits = cheerBit.bits,
                )
                model.testAlert(TODO("testAlert with the twitchCheer alert case"))
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
    var bits by remember(cheerBit) { mutableStateOf(cheerBit.bits) }
    var comparisonOperator by remember(cheerBit) {
        mutableStateOf(cheerBit.comparisonOperator.rawValue)
    }
    NavigationLink(
        destination = {
            TwitchCheerView(
                model = model,
                alert = alert,
                cheerBit = cheerBit,
                bits = bits,
                comparisonOperator = comparisonOperator,
                onBitsChange = { bits = it },
                onComparisonOperatorChange = { comparisonOperator = it },
                onBack = {},
            )
        },
    ) {
        DraggableItemTextView(name = formatTitle(bits, comparisonOperator))
    }
}

private fun deleteCheerBit(
    twitch: SettingsWidgetAlertsTwitch,
    model: Model,
    offsets: IndexSet,
) {
    twitch.cheerBits = twitch.cheerBits.removing(atOffsets = offsets)
    model.updateAlertsSettings()
}

@Composable
private fun TwitchCheerBitsView(
    model: Model = LocalModel.current,
    twitch: SettingsWidgetAlertsTwitch,
    onBack: () -> Unit,
) {
    Form(title = localized("Cheers")) {
        Section(
            footerContent = {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(localized("The first item that matches cheered bits will be played."))
                    Text("")
                    SwipeLeftToDeleteHelpView(kind = "an item")
                }
            },
        ) {
            ForEach(
                twitch.cheerBits,
                id = { it.id },
                onDelete = { deleteCheerBit(twitch, model, it) },
                onMove = { froms, to ->
                    twitch.cheerBits = twitch.cheerBits.moving(fromOffsets = froms, toOffset = to)
                    model.updateAlertsSettings()
                },
            ) { cheerBit ->
                ContextMenuDeleteButton(
                    action = {
                        val index = twitch.cheerBits.indexOfFirst { it.id == cheerBit.id }
                        if (index >= 0) {
                            deleteCheerBit(twitch, model, setOf(index))
                        }
                    },
                ) {
                    TwitchCheerBitsItemView(
                        model = model,
                        alert = cheerBit.alert,
                        cheerBit = cheerBit,
                    )
                }
            }
            CreateButtonView {
                twitch.cheerBits = twitch.cheerBits + SettingsWidgetAlertsCheerBitsAlert()
                model.updateAlertsSettings()
            }
        }
    }
}

@Composable
private fun TwitchRewardView(
    model: Model = LocalModel.current,
    reward: SettingsStreamTwitchReward,
    onBack: () -> Unit,
) {
    Form(title = reward.title) {
        Section {
            Toggle(
                title = localized("Enabled"),
                isOn = binding(
                    get = { reward.alert.enabled },
                    set = { value ->
                        reward.alert.enabled = value
                        model.updateAlertsSettings()
                    },
                ),
            )
        }
        AlertMediaView(alert = reward.alert)
    }
}

@Composable
private fun TwitchRewardsView(
    model: Model = LocalModel.current,
    onBack: () -> Unit,
) {
    val stream by model.stream.collectAsState()
    LaunchedEffect(Unit) {
        model.fetchTwitchRewards()
    }
    Form(title = localized("Rewards")) {
        if (stream.twitchRewards.isEmpty()) {
            Text(localized("No rewards found"))
        } else {
            stream.twitchRewards.forEach { reward ->
                key(reward.id) {
                    NavigationLink(
                        destination = {
                            TwitchRewardView(
                                model = model,
                                reward = reward,
                                onBack = {},
                            )
                        },
                    ) {
                        Text(reward.title)
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetAlertsTwitchSettingsView(
    model: Model = LocalModel.current,
    twitch: SettingsWidgetAlertsTwitch,
) {
    val twitchRewards by model.database.debug.twitchRewards.collectAsState()
    Form(title = localized("Twitch")) {
        Section {
            NavigationLink(
                destination = {
                    TwitchFollowsView(
                        model = model,
                        alert = twitch.follows,
                        onBack = {},
                    )
                },
            ) {
                Text(localized("Follows"))
            }
            NavigationLink(
                destination = {
                    TwitchSubscriptionsView(
                        model = model,
                        alert = twitch.subscriptions,
                        onBack = {},
                    )
                },
            ) {
                Text(localized("Subscriptions"))
            }
            NavigationLink(
                destination = {
                    TwitchRaidsView(
                        model = model,
                        alert = twitch.raids,
                        onBack = {},
                    )
                },
            ) {
                Text(localized("Raids"))
            }
            NavigationLink(
                destination = {
                    TwitchCheerBitsView(
                        model = model,
                        twitch = twitch,
                        onBack = {},
                    )
                },
            ) {
                Text(localized("Cheers"))
            }
            if (twitchRewards) {
                NavigationLink(
                    destination = {
                        TwitchRewardsView(
                            model = model,
                            onBack = {},
                        )
                    },
                ) {
                    Text(localized("Rewards"))
                }
            }
        }
    }
}
