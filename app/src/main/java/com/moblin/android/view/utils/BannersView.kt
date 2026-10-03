package com.moblin.android.view.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.LocalModel
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.swiftui.formBodyStyle
import com.moblin.android.platform.swiftui.formFootnoteStyle
import com.moblin.android.various.model.Banners
import com.moblin.android.various.model.HypeTrain
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.ProgressBar
import com.moblin.android.various.model.Raid
import com.moblin.android.various.model.RaidState
import com.moblin.android.various.model.TwitchPoll
import com.moblin.android.various.model.TwitchPollState
import com.moblin.android.various.model.TwitchPrediction
import com.moblin.android.various.model.TwitchPredictionOutcome
import com.moblin.android.various.model.TwitchPredictionState
import com.moblin.android.view.controlbar.quickbutton.chat.ChannelImageView
import com.moblin.android.various.model.cancelRaidTwitchChannel
import java.math.RoundingMode
import java.text.NumberFormat

private val bannerBackgroundColor = Color(red = 0x64, green = 0x41, blue = 0xA5)
private val predictionPinkColor = Color(red = 0xF5, green = 0x00, blue = 0x9B)
private val predictionBlueColor = Color(red = 0x38, green = 0x7A, blue = 0xFF)
private val bannerTrackColor = Color.White.copy(alpha = 0.24f)
private val bannerButtonBackgroundColor = Color.White.copy(alpha = 0.15f)

@Composable
private fun BannerButton(title: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .alpha(if (pressed) 0.5f else 1f)
            .clip(RoundedCornerShape(6.dp))
            .background(bannerButtonBackgroundColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            style = formBodyStyle,
        )
    }
}

@Composable
private fun ProgressBarView(progress: ProgressBar) {
    val goal by progress.goal.collectAsState()
    val current by progress.progress.collectAsState()
    val fraction = if (goal > 0) {
        ((goal - current).toDouble() / goal.toDouble()).toFloat()
    } else {
        0.0f
    }
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0.0f, 1.0f) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 20.dp)
            .height(16.dp),
        color = Color.White,
        trackColor = bannerTrackColor,
    )
}

@Composable
private fun HypeTrainProgressView(progress: ProgressBar, message: String) {
    val goal by progress.goal.collectAsState()
    val current by progress.progress.collectAsState()
    val percentage = if (goal > 0) {
        (100.0 * minOf(current.toDouble() / goal.toDouble(), 1.0)).toInt()
    } else {
        0
    }
    val fraction = if (goal > 0) {
        (minOf(current, goal).toDouble() / goal.toDouble()).toFloat()
    } else {
        0.0f
    }
    Column(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0.0f, 1.0f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            color = Color.White,
            trackColor = bannerTrackColor,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$percentage%",
                color = Color.White,
                style = formFootnoteStyle,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = message,
                color = Color.White,
                style = formFootnoteStyle,
            )
        }
    }
}

@Composable
private fun HypeTrainView(model: Model = LocalModel.current, hypeTrain: HypeTrain) {
    val level by hypeTrain.level.collectAsState()
    val progress by hypeTrain.progress.collectAsState()
    val message by hypeTrain.message.collectAsState()

    Column(
        modifier = Modifier.background(bannerBackgroundColor),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        level?.let { currentLevel ->
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SystemImage(name = "train.side.rear.car", fontSize = formBodyStyle.fontSize, tint = Color.White)
                    SystemImage(name = "train.side.middle.car", fontSize = formBodyStyle.fontSize, tint = Color.White)
                    SystemImage(name = "train.side.middle.car", fontSize = formBodyStyle.fontSize, tint = Color.White)
                    SystemImage(name = "train.side.middle.car", fontSize = formBodyStyle.fontSize, tint = Color.White)
                    SystemImage(name = "train.side.front.car", fontSize = formBodyStyle.fontSize, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = localized("LEVEL $currentLevel"),
                    color = Color.White,
                    style = formBodyStyle,
                )
                BannerButton(title = localized("Close")) {
                    hypeTrain.level.value = null
                    hypeTrain.progress.value = null
                }
            }
        }
        progress?.let { currentProgress ->
            HypeTrainProgressView(progress = currentProgress, message = message)
        }
    }
}

@Composable
private fun RaidView(model: Model = LocalModel.current, raid: Raid) {
    val state by raid.state.collectAsState()
    val message by raid.message.collectAsState()
    val channelImage by raid.channelImage.collectAsState()
    val channelLogin by raid.channelLogin.collectAsState()
    val progress by raid.progress.collectAsState()
    val uriHandler = LocalUriHandler.current

    fun close() {
        when (state) {
            RaidState.idle -> {
            }
            RaidState.ongoing -> {
                raid.message.value = localized("Cancelling raid")
                model.cancelRaidTwitchChannel { result ->
                    if (!result.toString().contains("success", ignoreCase = true)) {
                        raid.message.value = localized("Failed to cancel the raid")
                        raid.state.value = RaidState.completed
                    }
                }
                raid.state.value = RaidState.cancelling
            }
            RaidState.cancelling -> {
            }
            RaidState.completed -> {
                raid.state.value = RaidState.idle
            }
        }
    }

    if (state != RaidState.idle) {
        Column(
            modifier = Modifier.background(bannerBackgroundColor),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelImageView(image = channelImage)
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        style = formBodyStyle,
                    )
                    Text(
                        text = "twitch.tv/$channelLogin",
                        color = Color.White,
                        style = formFootnoteStyle.copy(textDecoration = TextDecoration.Underline),
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://twitch.tv/$channelLogin")
                        },
                    )
                }
                Spacer(Modifier.weight(1f))
                BannerButton(
                    title = if (state == RaidState.ongoing) localized("Cancel") else localized("Close"),
                ) {
                    close()
                }
            }
            progress?.let { currentProgress ->
                ProgressBarView(progress = currentProgress)
            }
        }
    }
}

@Composable
private fun OptionBarView(
    title: String,
    detail: String,
    fraction: Double,
    color: Color,
    bold: Boolean,
) {
    val textWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = Color.White,
                style = formFootnoteStyle,
                fontWeight = textWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = detail,
                color = Color.White,
                style = formFootnoteStyle,
                fontWeight = textWeight,
            )
        }
        LinearProgressIndicator(
            progress = { fraction.toFloat().coerceIn(0.0f, 1.0f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.24f),
        )
    }
}

@Composable
private fun BannerView(
    image: String,
    title: String,
    message: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .background(bannerBackgroundColor)
            .padding(10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SystemImage(name = image, fontSize = formBodyStyle.fontSize, tint = Color.White)
            Text(
                text = title,
                color = Color.White,
                style = formBodyStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            BannerButton(title = localized("Close"), onClick = onClose)
        }
        content()
        Text(
            text = message,
            color = Color.White,
            style = formFootnoteStyle,
        )
    }
}

private fun formatPercentage(fraction: Double): String {
    val format = NumberFormat.getPercentInstance()
    format.maximumFractionDigits = 0
    format.minimumFractionDigits = 0
    format.roundingMode = RoundingMode.DOWN
    return format.format(fraction)
}

@Composable
private fun TwitchPollView(model: Model = LocalModel.current, poll: TwitchPoll) {
    val state by poll.state.collectAsState()
    val title by poll.title.collectAsState()
    val message by poll.message.collectAsState()
    val choices by poll.choices.collectAsState()
    val totalVotes by poll.totalVotes.collectAsState()

    fun fraction(votes: Int): Double {
        if (totalVotes <= 0) {
            return 0.0
        }
        return votes.toDouble() / totalVotes.toDouble()
    }

    if (state != TwitchPollState.idle) {
        BannerView(
            image = "chart.bar",
            title = title,
            message = message,
            onClose = {
                poll.state.value = TwitchPollState.idle
                poll.title.value = ""
                poll.choices.value = emptyList()
                poll.totalVotes.value = 0
                poll.message.value = ""
            },
        ) {
            choices.forEach { choice ->
                val choiceFraction = fraction(choice.votes)
                val percentage = formatPercentage(choiceFraction)
                OptionBarView(
                    title = choice.title,
                    detail = localized("$percentage (${choice.votes} votes)"),
                    fraction = choiceFraction,
                    color = Color.White,
                    bold = false,
                )
            }
        }
    }
}

@Composable
private fun TwitchPredictionView(model: Model = LocalModel.current, prediction: TwitchPrediction) {
    val state by prediction.state.collectAsState()
    val title by prediction.title.collectAsState()
    val message by prediction.message.collectAsState()
    val outcomes by prediction.outcomes.collectAsState()
    val totalChannelPoints by prediction.totalChannelPoints.collectAsState()

    fun fraction(channelPoints: Int): Double {
        if (totalChannelPoints <= 0) {
            return 0.0
        }
        return channelPoints.toDouble() / totalChannelPoints.toDouble()
    }

    fun color(outcome: TwitchPredictionOutcome): Color {
        return if (outcome.color == "pink") {
            predictionPinkColor
        } else {
            predictionBlueColor
        }
    }

    if (state != TwitchPredictionState.idle) {
        BannerView(
            image = "sparkles",
            title = title,
            message = message,
            onClose = {
                prediction.state.value = TwitchPredictionState.idle
                prediction.title.value = ""
                prediction.outcomes.value = emptyList()
                prediction.totalChannelPoints.value = 0
                prediction.message.value = ""
            },
        ) {
            outcomes.forEach { outcome ->
                val outcomeFraction = fraction(outcome.channelPoints)
                val percentage = formatPercentage(outcomeFraction)
                OptionBarView(
                    title = outcome.title,
                    detail = localized(
                        "$percentage (${outcome.channelPoints} points, ${outcome.users} users)",
                    ),
                    fraction = outcomeFraction,
                    color = color(outcome),
                    bold = outcome.winner,
                )
            }
        }
    }
}

@Composable
private fun MinimizedView(
    hypeTrain: HypeTrain,
    raid: Raid,
    poll: TwitchPoll,
    prediction: TwitchPrediction,
) {
    val hypeTrainLevel by hypeTrain.level.collectAsState()
    val hypeTrainProgress by hypeTrain.progress.collectAsState()
    val raidState by raid.state.collectAsState()
    val pollState by poll.state.collectAsState()
    val predictionState by prediction.state.collectAsState()

    val icons = mutableListOf<String>()
    if (hypeTrainLevel != null || hypeTrainProgress != null) {
        icons.add("train.side.front.car")
    }
    if (raidState != RaidState.idle) {
        icons.add("figure.run")
    }
    if (pollState != TwitchPollState.idle) {
        icons.add("chart.bar")
    }
    if (predictionState != TwitchPredictionState.idle) {
        icons.add("sparkles")
    }

    if (icons.isNotEmpty()) {
        Row(
            modifier = Modifier
                .padding(top = 5.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(bannerBackgroundColor)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icons.forEach { icon ->
                SystemImage(name = icon, fontSize = formBodyStyle.fontSize, tint = Color.White)
            }
        }
    }
}

@Composable
fun BannersView(model: Model = LocalModel.current, banners: Banners) {
    val minimized by banners.minimized.collectAsState()
    val hypeTrain = model.hypeTrain
    val raid = model.raid
    val twitchPoll = model.twitchPoll
    val twitchPrediction = model.twitchPrediction
    var contentHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxHeight = constraints.maxHeight
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp),
            )
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures {
                        banners.minimized.value = !banners.minimized.value
                    }
                },
            ) {
                if (minimized) {
                    MinimizedView(
                        hypeTrain = hypeTrain,
                        raid = raid,
                        poll = twitchPoll,
                        prediction = twitchPrediction,
                    )
                } else {
                    val height = with(density) {
                        minOf(contentHeight, (maxHeight - 1).coerceAtLeast(0)).toDp()
                    }
                    Box(
                        modifier = Modifier
                            .height(height)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Column(modifier = Modifier.onSizeChanged { contentHeight = it.height }) {
                            HypeTrainView(model = model, hypeTrain = hypeTrain)
                            RaidView(model = model, raid = raid)
                            TwitchPollView(model = model, poll = twitchPoll)
                            TwitchPredictionView(model = model, prediction = twitchPrediction)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}
