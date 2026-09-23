package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetBingoCard
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
private fun BingoView(
    settings: SettingsWidgetBingoCard,
    sceneWidget: SettingsSceneWidget,
    canvasSize: CGSize,
) {
    val squaresCountSide = settings.size()
    val squareSize = squareSize(sceneWidget, canvasSize, squaresCountSide)
    Box(contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(1.dp),
        ) {
            repeat(squaresCountSide) { row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(squaresCountSide) { column ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(squareSize.dp)
                                .background(settings.backgroundColorColor)
                                .border(2.dp, settings.foregroundColorColor),
                        ) {
                            val index = row * squaresCountSide + column
                            val squares = settings.squares
                            if (index < squares.size) {
                                val square = squares[index]
                                BingoCardSquareText(
                                    text = square.text,
                                    fontSize = (35.0 * squareSize / 100.0).toFloat(),
                                    minimumScale = 0.4f,
                                    color = settings.foregroundColorColor,
                                    modifier = Modifier.padding(4.dp),
                                )
                                if (square.checked) {
                                    Text(
                                        text = "╳",
                                        color = settings.foregroundColorColor,
                                        fontSize = (80.0 * squareSize / 100.0).sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, settings.foregroundColorColor),
        )
    }
}

@Composable
private fun BingoCardSquareText(
    text: String,
    fontSize: Float,
    minimumScale: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var currentSize by remember(fontSize) { mutableStateOf(fontSize) }
    Text(
        text = text,
        color = color,
        fontSize = currentSize.sp,
        textAlign = TextAlign.Center,
        maxLines = 3,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && currentSize > fontSize * minimumScale) {
                currentSize = maxOf(currentSize - 1f, fontSize * minimumScale)
            }
        },
    )
}

private fun squareSize(
    sceneWidget: SettingsSceneWidget,
    canvasSize: CGSize,
    squaresCountSide: Int,
): Double {
    return toPixels(sceneWidget.layout.size, canvasSize.minimum()) / squaresCountSide.toDouble()
}

class BingoCardEffect(canvasSize: Size) : VideoEffect() {
    private val canvasSize: CGSize = canvasSize.toCGSize()
    private var settings = SettingsWidgetBingoCard()
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var sceneWidgetPipeline = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var renderer: ImageRenderer? = null
    private var cancellable: AnyCancellable? = null
    private var bingoImage: EffectImageCgImage? = null

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

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        return bingoImage?.getCiImage()
            ?.move(sceneWidgetPipeline.layout, image.extent.size)
            ?.cropped(to = image.extent)
            ?.composited(over = image) ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        return bingoImage?.getMetalPetalImage()?.moveComposited(sceneWidgetPipeline.layout, image) ?: image
    }

    private fun setup() {
        cancellable?.cancel()
        val settings0 = settings
        val sceneWidget0 = sceneWidget
        val canvasSize0 = canvasSize
        renderer = ImageRenderer(content = {
            BingoView(settings = settings0, sceneWidget = sceneWidget0, canvasSize = canvasSize0)
        })
        val weakSelf = WeakReference(this)
        cancellable = renderer?.objectWillChange?.sink {
            val self = weakSelf.get() ?: return@sink
            self.setBingoImage(image = self.renderer?.cgImage)
        }
        setBingoImage(image = renderer?.cgImage)
    }

    private fun setBingoImage(image: Bitmap?) {
        val bingoImage = image?.toEffectImage()
        processorPipelineQueue.launch {
            this@BingoCardEffect.bingoImage = bingoImage
        }
    }
}
