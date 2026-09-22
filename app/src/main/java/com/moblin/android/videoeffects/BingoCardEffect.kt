package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.media.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.minDimension
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetBingoCard
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
private fun BingoView(
    settings: SettingsWidgetBingoCard,
    sceneWidget: SettingsSceneWidget,
    canvasSize: Size,
) {
    fun squareSize(squaresCountSide: Int): Double {
        return toPixels(sceneWidget.layout.size, canvasSize.minDimension.toDouble()) / squaresCountSide
    }

    val squaresCountSide = settings.size()
    val squareSize = squareSize(squaresCountSide)
    Box {
        Column(
            modifier = Modifier.padding(1.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            for (row in 0 until squaresCountSide) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    for (column in 0 until squaresCountSide) {
                        Box(
                            modifier = Modifier
                                .size(squareSize.dp, squareSize.dp)
                                .background(settings.backgroundColorColor)
                                .border(2.dp, settings.foregroundColorColor),
                        ) {
                            val index = row * squaresCountSide + column
                            if (index < settings.squares.size) {
                                val square = settings.squares[index]
                                Text(
                                    text = square.text,
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .then(TODO("no Compose counterpart for minimumScaleFactor")),
                                    maxLines = 3,
                                    textAlign = TextAlign.Center,
                                    color = settings.foregroundColorColor,
                                    style = TextStyle(fontSize = (35 * squareSize / 100).sp),
                                )
                                if (square.checked) {
                                    Text(
                                        text = "╳",
                                        color = settings.foregroundColorColor,
                                        style = TextStyle(fontSize = (80 * squareSize / 100).sp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier.border(1.dp, settings.foregroundColorColor),
        )
    }
}

class BingoCardEffect(canvasSize: Size) : VideoEffect() {
    private val canvasSize: Size = canvasSize
    private var settings = SettingsWidgetBingoCard()
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var sceneWidgetPipeline = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var renderer: Any? = null
    private var cancellable: Job? = null
    private var bingoImage: EffectImageCgImage? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            sceneWidgetPipeline.layout = sceneWidget.layout
        }
        this.sceneWidget.layout = sceneWidget.layout
    }

    fun setSettings(settings: SettingsWidgetBingoCard) {
        if (settings === this.settings) {
            return
        }
        this.settings = settings
        setup()
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        val background = EffectImagePixelBuffer(image).getCiImage()
        bingoImage
            ?.getCiImage()
            ?.move(sceneWidgetPipeline.layout, background.extent.size)
            ?.cropped(background.extent)
            ?.composited(background)
        return TODO("no Android counterpart for CIImage to android.media.Image")
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        val background = EffectImagePixelBuffer(image).getMetalPetalImage()
        bingoImage
            ?.getMetalPetalImage()
            ?.moveComposited(sceneWidgetPipeline.layout, background)
        return TODO("no Android counterpart for MTIImage to android.media.Image")
    }

    private fun CIImage.cropped(extent: Any): CIImage = TODO("no Android counterpart for CIImage.cropped(to:)")

    private fun CIImage.composited(over: CIImage): CIImage = TODO("no Android counterpart for CIImage.composited(over:)")

    private fun setup() {
        cancellable?.cancel()
        renderer = TODO("no Android counterpart for SwiftUI ImageRenderer")
        cancellable = mainScope.launch {
            Unit
        }
        setBingoImage(image = TODO("no Android counterpart for SwiftUI ImageRenderer.cgImage"))
    }

    private fun setBingoImage(image: Bitmap?) {
        val bingoImage = image?.toEffectImage()
        processorPipelineQueue.launch {
            this@BingoCardEffect.bingoImage = bingoImage
        }
    }
}
