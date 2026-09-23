package com.moblin.android.videoeffects.scoreboard

import android.graphics.Bitmap
import com.moblin.android.platform.video.CVPixelBuffer as Image
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.remotecontrol.RemoteControlScoreboardMatchConfig
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard
import com.moblin.android.various.settings.SettingsWidgetGolfScoreboard
import com.moblin.android.various.settings.SettingsWidgetModularScoreboard
import com.moblin.android.various.settings.SettingsWidgetPadelScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboard
import com.moblin.android.various.settings.SettingsWidgetScoreboardPlayer
import com.moblin.android.various.settings.SettingsWidgetScoreboardSport
import com.moblin.android.videoeffects.EffectImageCgImage
import com.moblin.android.videoeffects.toPixels
import java.util.UUID
import kotlinx.coroutines.launch

val scoreboardScoreFontSize = 37.0
val scoreboardScoreBigFontSize = 45.0

fun formatScore(score: Int): String {
    if (score == 0) {
        return localized("E")
    }
    if (score > 0) {
        return "+$score"
    }
    return "$score"
}

@Composable
fun TeamScoreView(score: Int) {
    Column {
        Spacer(modifier = Modifier.weight(1f))
        Text(text = score.toString())
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PoweredByMoblinView(
    backgroundColor: Color,
    scale: Double
) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .padding(horizontal = (8 * scale).dp, vertical = (3 * scale).dp)
    ) {
        Text(
            text = "Powered by Moblin",
            fontFamily = FontFamily.Monospace,
            fontSize = (15 * scale).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

class ScoreboardEffect(private val canvasSize: Size) : VideoEffect() {
    private var scoreboardImage: EffectImageCgImage? = null
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var sceneWidgetPipeline = SettingsSceneWidget(widgetId = UUID.randomUUID())

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            sceneWidgetPipeline.layout = sceneWidget.layout
        }
        this.sceneWidget.layout = sceneWidget.layout
    }

    fun update(
        scoreboard: SettingsWidgetScoreboard,
        config: RemoteControlScoreboardMatchConfig,
        players: List<SettingsWidgetScoreboardPlayer>
    ) {
        val scale = toPixels(
            sceneWidget.layout.size,
            minOf(canvasSize.width, canvasSize.height).toDouble()
        ) / 200
        when (scoreboard.sport) {
            SettingsWidgetScoreboardSport.generic -> updateGeneric(
                textColor = scoreboard.textColorColor,
                primaryBackgroundColor = scoreboard.primaryBackgroundColorColor,
                secondaryBackgroundColor = scoreboard.secondaryBackgroundColorColor,
                generic = scoreboard.generic,
                scale = scale
            )
            SettingsWidgetScoreboardSport.padel -> updatePadel(
                textColor = scoreboard.textColorColor,
                primaryBackgroundColor = scoreboard.primaryBackgroundColorColor,
                secondaryBackgroundColor = scoreboard.secondaryBackgroundColorColor,
                padel = scoreboard.padel,
                players = players,
                scale = scale
            )
            SettingsWidgetScoreboardSport.golf -> updateGolf(
                textColor = scoreboard.textColorColor,
                primaryBackgroundColor = scoreboard.primaryBackgroundColorColor,
                secondaryBackgroundColor = scoreboard.secondaryBackgroundColorColor,
                golf = scoreboard.golf,
                scale = scale
            )
            SettingsWidgetScoreboardSport.golfFullScorecard -> updateGolfFullScorecard(
                textColor = scoreboard.textColorColor,
                primaryBackgroundColor = scoreboard.primaryBackgroundColorColor,
                secondaryBackgroundColor = scoreboard.secondaryBackgroundColorColor,
                golf = scoreboard.golf,
                scale = scale
            )
            else -> updateModular(modular = scoreboard.modular, config = config, scale = scale)
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        return TODO("no Android counterpart for the CIImage move/crop/composite pipeline")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        return TODO("no Android counterpart for MetalPetal")
    }

    private fun setScoreboardImage(image: Bitmap?) {
        val scoreboardImage: EffectImageCgImage? =
            image?.let { TODO("no Android counterpart for converting a Bitmap to an EffectImageCgImage") }
        processorPipelineQueue.launch {
            this@ScoreboardEffect.scoreboardImage = scoreboardImage
        }
    }

    private fun updateGeneric(
        textColor: Color,
        primaryBackgroundColor: Color,
        secondaryBackgroundColor: Color,
        generic: SettingsWidgetGenericScoreboard,
        scale: Double
    ) {
        val content: @Composable () -> Unit = {
            ScoreboardEffectGenericView(
                textColor = textColor,
                primaryBackgroundColor = primaryBackgroundColor,
                secondaryBackgroundColor = secondaryBackgroundColor,
                generic = generic,
                scale = scale
            )
        }
        setScoreboardImage(image = null)
    }

    private fun updatePadel(
        textColor: Color,
        primaryBackgroundColor: Color,
        secondaryBackgroundColor: Color,
        padel: SettingsWidgetPadelScoreboard,
        players: List<SettingsWidgetScoreboardPlayer>,
        scale: Double
    ) {
        val content: @Composable () -> Unit = {
            ScoreboardEffectPadelView(
                textColor = textColor,
                primaryBackgroundColor = primaryBackgroundColor,
                secondaryBackgroundColor = secondaryBackgroundColor,
                padel = padel,
                players = players,
                scale = scale
            )
        }
        setScoreboardImage(image = null)
    }

    private fun updateModular(
        modular: SettingsWidgetModularScoreboard,
        config: RemoteControlScoreboardMatchConfig,
        scale: Double
    ) {
        val content: @Composable () -> Unit = {
            ScoreboardEffectModularView(
                modular = modular,
                config = config,
                scale = scale
            )
        }
        setScoreboardImage(image = null)
    }

    private fun updateGolf(
        textColor: Color,
        primaryBackgroundColor: Color,
        secondaryBackgroundColor: Color,
        golf: SettingsWidgetGolfScoreboard,
        scale: Double
    ) {
        val content: @Composable () -> Unit = {
            ScoreboardEffectGolfView(
                textColor = textColor,
                primaryBackgroundColor = primaryBackgroundColor,
                secondaryBackgroundColor = secondaryBackgroundColor,
                golf = golf,
                scale = scale
            )
        }
        setScoreboardImage(image = null)
    }

    private fun updateGolfFullScorecard(
        textColor: Color,
        primaryBackgroundColor: Color,
        secondaryBackgroundColor: Color,
        golf: SettingsWidgetGolfScoreboard,
        scale: Double
    ) {
        val content: @Composable () -> Unit = {
            ScoreboardEffectGolfFullScorecardView(
                textColor = textColor,
                primaryBackgroundColor = primaryBackgroundColor,
                secondaryBackgroundColor = secondaryBackgroundColor,
                golf = golf,
                scale = scale
            )
        }
        setScoreboardImage(image = null)
    }
}
