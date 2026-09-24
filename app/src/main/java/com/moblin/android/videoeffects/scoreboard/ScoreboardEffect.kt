package com.moblin.android.videoeffects.scoreboard

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.layout.EdgeSet
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.SwiftUIView
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.bold
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.fontDesign
import com.moblin.android.platform.swiftui.layout.padding
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

fun ViewBuilder.TeamScoreView(score: Int): View = VStack {
    Spacer(minLength = 0.0)
    Text(score.toString())
    Spacer(minLength = 0.0)
}

fun ViewBuilder.PoweredByMoblinView(backgroundColor: Color, scale: Double): View = HStack {
    Text(localized("Powered by Moblin"))
        .fontDesign(FontDesign.Monospaced)
        .font(Font.system(size = 15 * scale))
        .bold()
    Spacer()
}
    .padding(EdgeSet.horizontal, 8 * scale)
    .padding(EdgeSet.vertical, 3 * scale)
    .background(backgroundColor)

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
                SwiftUIView {
                    ScoreboardEffectGenericView(
                        textColor = textColor0,
                        primaryBackgroundColor = primaryBackgroundColor0,
                        secondaryBackgroundColor = secondaryBackgroundColor0,
                        generic = generic0,
                        scale = scale0,
                    )
                }
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
                SwiftUIView {
                    ScoreboardEffectPadelView(
                        textColor = textColor0,
                        primaryBackgroundColor = primaryBackgroundColor0,
                        secondaryBackgroundColor = secondaryBackgroundColor0,
                        padel = padel0,
                        players = players0,
                        scale = scale0,
                    )
                }
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
                SwiftUIView {
                    ScoreboardEffectModularView(
                        modular = modular0,
                        config = config0,
                        scale = scale0,
                    )
                }
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
                SwiftUIView {
                    ScoreboardEffectGolfView(
                        textColor = textColor0,
                        primaryBackgroundColor = primaryBackgroundColor0,
                        secondaryBackgroundColor = secondaryBackgroundColor0,
                        golf = golf0,
                        scale = scale0,
                    )
                }
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
                SwiftUIView {
                    ScoreboardEffectGolfFullScorecardView(
                        textColor = textColor0,
                        primaryBackgroundColor = primaryBackgroundColor0,
                        secondaryBackgroundColor = secondaryBackgroundColor0,
                        golf = golf0,
                        scale = scale0,
                    )
                }
            }).cgImage,
        )
    }
}
