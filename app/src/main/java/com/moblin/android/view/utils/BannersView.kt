package com.moblin.android.view.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.RgbColor
import com.moblin.android.localized
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
import com.moblin.android.LocalModel

private val bannerBackgroundColor = Color(0xFF6441A5)

private fun bannerIcon(name: String): ImageVector = when (name) {
    "train.side.rear.car", "train.side.middle.car", "train.side.front.car" -> Icons.Default.Train
    "figure.run" -> Icons.Default.DirectionsRun
    "chart.bar" -> Icons.Default.BarChart
    "sparkles" -> Icons.Default.AutoAwesome
    else -> Icons.Default.Info
}

@Composable
private fun ProgressBarView(progress: ProgressBar) {
    val fraction = if (progress.goal.value > 0) {
        ((progress.goal.value - progress.progress.value).toDouble() / progress.goal.value.toDouble()).toFloat()
    } else {
        0.0f
    }
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 20.dp)
            .height(16.dp),
        color = Color.White,
    )
}

@Composable
private fun HypeTrainProgressView(progress: ProgressBar, message: String) {
    fun percentage(): Int {
        if (progress.goal.value <= 0) {
            return 0
        }
        return (100.0 * minOf(progress.progress.value.toDouble() / progress.goal.value.toDouble(), 1.0)).toInt()
    }

    Column(
        modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val fraction = if (progress.goal.value > 0) {
            (minOf(progress.progress.value.toDouble(), progress.goal.value.toDouble()) / progress.goal.value.toDouble()).toFloat()
        } else {
            0.0f
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            color = Color.White,
        )
        Row {
            Text(
                text = "${percentage()}%",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun HypeTrainView(model: Model = LocalModel.current, hypeTrain: HypeTrain) {
    val level by hypeTrain.level.collectAsState()
    val progress by hypeTrain.progress.collectAsState()
    val message by hypeTrain.message.collectAsState()

    Column(modifier = Modifier.background(bannerBackgroundColor)) {
        level?.let { currentLevel ->
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(bannerIcon("train.side.rear.car"), contentDescription = null, tint = Color.White)
                Icon(bannerIcon("train.side.middle.car"), contentDescription = null, tint = Color.White)
                Icon(bannerIcon("train.side.middle.car"), contentDescription = null, tint = Color.White)
                Icon(bannerIcon("train.side.middle.car"), contentDescription = null, tint = Color.White)
                Icon(bannerIcon("train.side.front.car"), contentDescription = null, tint = Color.White)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "LEVEL $currentLevel",
                    color = Color.White,
                )
                TextButton(onClick = { TODO("removeHypeTrain") }) {
                    Text("Close", color = Color.White)
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
                Unit
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
        Column(modifier = Modifier.background(bannerBackgroundColor)) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChannelImageView(image = channelImage)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = message,
                        color = Color.White,
                    )
                    val url = "https://twitch.tv/$channelLogin"
                    Text(
                        text = "twitch.tv/$channelLogin",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { uriHandler.openUri(url) },
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { close() }) {
                    Text(
                        text = if (state == RaidState.ongoing) "Cancel" else "Close",
                        color = Color.White,
                    )
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row {
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = detail,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            )
        }
        LinearProgressIndicator(
            progress = { fraction.toFloat().coerceIn(0.0f, 1.0f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(bannerIcon(image), contentDescription = null, tint = Color.White)
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) {
                Text("Close", color = Color.White)
            }
        }
        content()
        Text(
            text = message,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
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
            onClose = { TODO("removeTwitchPoll") },
        ) {
            choices.forEach { choice ->
                val choiceFraction = fraction(choice.votes)
                val percentage = (100.0 * choiceFraction).toInt()
                OptionBarView(
                    title = choice.title,
                    detail = localized("$percentage% (${choice.votes} votes)"),
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
            Color(0xFFF5009B)
        } else {
            Color(0xFF387AFF)
        }
    }

    if (state != TwitchPredictionState.idle) {
        BannerView(
            image = "sparkles",
            title = title,
            message = message,
            onClose = { TODO("removeTwitchPrediction") },
        ) {
            outcomes.forEach { outcome ->
                val outcomeFraction = fraction(outcome.channelPoints)
                val percentage = (100.0 * outcomeFraction).toInt()
                OptionBarView(
                    title = outcome.title,
                    detail = localized(
                        "$percentage% (${outcome.channelPoints} points, ${outcome.users} users)",
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

    fun icons(): List<String> {
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
        return icons
    }

    val icons = icons()
    if (icons.isNotEmpty()) {
        Row(
            modifier = Modifier
                .padding(top = 5.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(bannerBackgroundColor)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            icons.forEach { icon ->
                Icon(bannerIcon(icon), contentDescription = null, tint = Color.White)
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
                modifier = Modifier.clickable {
                    banners.minimized.value = !minimized
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
                        minOf(contentHeight, maxHeight - 1).coerceAtLeast(0).toDp()
                    }
                    Column(
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
