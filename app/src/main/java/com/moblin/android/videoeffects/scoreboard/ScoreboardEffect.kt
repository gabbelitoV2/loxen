package com.moblin.android.videoeffects.scoreboard

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.SwiftUIFonts
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
import com.moblin.android.videoeffects.move
import com.moblin.android.videoeffects.moveComposited
import com.moblin.android.videoeffects.toEffectImage
import com.moblin.android.videoeffects.toPixels
import com.moblin.android.view.utils.FontDesign
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.weight(1f))
        Text(score.toString())
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PoweredByMoblinView(backgroundColor: Color, scale: Double) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(backgroundColor)
            .padding(horizontal = (8 * scale).dp, vertical = (3 * scale).dp),
    ) {
        Text(
            text = localized("Powered by Moblin"),
            style = SwiftUIFonts.system(
                size = (15 * scale).toFloat(),
                weight = FontWeight.Bold,
                design = FontDesign.Monospaced,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

class ScoreboardEffect(canvasSize: Size) : VideoEffect() {
    private val canvasSize = canvasSize.toCGSize()
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
        players: List<SettingsWidgetScoreboardPlayer>,
    ) {
        val scale = toPixels(sceneWidget.layout.size, canvasSize.minimum()) / 200
        when (scoreboard.sport) {
            SettingsWidgetScoreboardSport.generic -> updateGeneric(
                textColor = scoreboard.textColorColor,
                primaryBackgroundColor = scoreboard.primaryBackgroundColorColor,
                secondaryBackgroundColor = scoreboard.secondaryBackgroundColorColor,
                generic = scoreboard.generic,
                scale = scale,
            )
            SettingsWidgetScoreboardSport.padel -> updatePadel(
                textColor = scoreboard.textColorColor,
                primaryBackgroundColor = scoreboard.primaryBackgroundColorColor,
                secondaryBackgroundColor = scoreboard.secondaryBackgroundColorColor,
                padel = scoreboard.padel,
                players = players,
                scale = scale,
            )
            SettingsWidgetScoreboardSport.golf -> updateGolf(
                textColor = scoreboard.textColorColor,
                primaryBackgroundColor = scoreboard.primaryBackgroundColorColor,
                secondaryBackgroundColor = scoreboard.secondaryBackgroundColorColor,
                golf = scoreboard.golf,
                scale = scale,
            )
            SettingsWidgetScoreboardSport.golfFullScorecard -> updateGolfFullScorecard(
                textColor = scoreboard.textColorColor,
                primaryBackgroundColor = scoreboard.primaryBackgroundColorColor,
                secondaryBackgroundColor = scoreboard.secondaryBackgroundColorColor,
                golf = scoreboard.golf,
                scale = scale,
            )
            else -> updateModular(modular = scoreboard.modular, config = config, scale = scale)
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        return scoreboardImage?.getCiImage()
            ?.move(layout = sceneWidgetPipeline.layout, streamSize = image.extent.size)
            ?.cropped(to = image.extent)
            ?.composited(over = image) ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        return scoreboardImage?.getMetalPetalImage()
            ?.moveComposited(layout = sceneWidgetPipeline.layout, backgroundImage = image) ?: image
    }

    private fun setScoreboardImage(image: Bitmap?) {
        val scoreboardImage = image?.toEffectImage()
        processorPipelineQueue.launch {
            this@ScoreboardEffect.scoreboardImage = scoreboardImage
        }
    }

    private fun updateGeneric(
        textColor: Color,
        primaryBackgroundColor: Color,
        secondaryBackgroundColor: Color,
        generic: SettingsWidgetGenericScoreboard,
        scale: Double,
    ) {
        val textColor0 = textColor
        val primaryBackgroundColor0 = primaryBackgroundColor
        val secondaryBackgroundColor0 = secondaryBackgroundColor
        val generic0 = generic
        val scale0 = scale
        setScoreboardImage(
            image = ImageRenderer(content = {
                ScoreboardEffectGenericView(
                    textColor = textColor0,
                    primaryBackgroundColor = primaryBackgroundColor0,
                    secondaryBackgroundColor = secondaryBackgroundColor0,
                    generic = generic0,
                    scale = scale0,
                )
            }).cgImage,
        )
    }

    private fun updatePadel(
        textColor: Color,
        primaryBackgroundColor: Color,
        secondaryBackgroundColor: Color,
        padel: SettingsWidgetPadelScoreboard,
        players: List<SettingsWidgetScoreboardPlayer>,
        scale: Double,
    ) {
        val textColor0 = textColor
        val primaryBackgroundColor0 = primaryBackgroundColor
        val secondaryBackgroundColor0 = secondaryBackgroundColor
        val padel0 = padel
        val players0 = players
        val scale0 = scale
        setScoreboardImage(
            image = ImageRenderer(content = {
                ScoreboardEffectPadelView(
                    textColor = textColor0,
                    primaryBackgroundColor = primaryBackgroundColor0,
                    secondaryBackgroundColor = secondaryBackgroundColor0,
                    padel = padel0,
                    players = players0,
                    scale = scale0,
                )
            }).cgImage,
        )
    }

    private fun updateModular(
        modular: SettingsWidgetModularScoreboard,
        config: RemoteControlScoreboardMatchConfig,
        scale: Double,
    ) {
        val modular0 = modular
        val config0 = config
        val scale0 = scale
        setScoreboardImage(
            image = ImageRenderer(content = {
                ScoreboardEffectModularView(
                    modular = modular0,
                    config = config0,
                    scale = scale0,
                )
            }).cgImage,
        )
    }

    private fun updateGolf(
        textColor: Color,
        primaryBackgroundColor: Color,
        secondaryBackgroundColor: Color,
        golf: SettingsWidgetGolfScoreboard,
        scale: Double,
    ) {
        val textColor0 = textColor
        val primaryBackgroundColor0 = primaryBackgroundColor
        val secondaryBackgroundColor0 = secondaryBackgroundColor
        val golf0 = golf
        val scale0 = scale
        setScoreboardImage(
            image = ImageRenderer(content = {
                ScoreboardEffectGolfView(
                    textColor = textColor0,
                    primaryBackgroundColor = primaryBackgroundColor0,
                    secondaryBackgroundColor = secondaryBackgroundColor0,
                    golf = golf0,
                    scale = scale0,
                )
            }).cgImage,
        )
    }

    private fun updateGolfFullScorecard(
        textColor: Color,
        primaryBackgroundColor: Color,
        secondaryBackgroundColor: Color,
        golf: SettingsWidgetGolfScoreboard,
        scale: Double,
    ) {
        val textColor0 = textColor
        val primaryBackgroundColor0 = primaryBackgroundColor
        val secondaryBackgroundColor0 = secondaryBackgroundColor
        val golf0 = golf
        val scale0 = scale
        setScoreboardImage(
            image = ImageRenderer(content = {
                ScoreboardEffectGolfFullScorecardView(
                    textColor = textColor0,
                    primaryBackgroundColor = primaryBackgroundColor0,
                    secondaryBackgroundColor = secondaryBackgroundColor0,
                    golf = golf0,
                    scale = scale0,
                )
            }).cgImage,
        )
    }
}
