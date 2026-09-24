package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.SwiftUIView
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.TextAlignment
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.frame
import com.moblin.android.platform.swiftui.layout.lineLimit
import com.moblin.android.platform.swiftui.layout.minimumScaleFactor
import com.moblin.android.platform.swiftui.layout.multilineTextAlignment
import com.moblin.android.platform.swiftui.layout.padding
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetBingoCard
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.launch

internal fun ViewBuilder.BingoView(
    settings: SettingsWidgetBingoCard,
    sceneWidget: SettingsSceneWidget,
    canvasSize: CGSize,
): View {
    val squaresCountSide = settings.size()
    val squareSize = squareSize(sceneWidget, canvasSize, squaresCountSide)
    return ZStack {
        VStack(spacing = 0.0) {
            for (row in 0 until squaresCountSide) {
                HStack(spacing = 0.0) {
                    for (column in 0 until squaresCountSide) {
                        ZStack {
                            Rectangle()
                                .stroke(settings.foregroundColorColor, lineWidth = 2.0)
                                .background(settings.backgroundColorColor)
                            val index = row * squaresCountSide + column
                            val squares = settings.squares
                            if (index < squares.size) {
                                val square = squares[index]
                                Text(square.text)
                                    .lineLimit(3)
                                    .minimumScaleFactor(0.4)
                                    .multilineTextAlignment(TextAlignment.center)
                                    .font(Font.system(size = 35 * squareSize / 100))
                                    .padding(4.0)
                                if (square.checked) {
                                    Text("╳")
                                        .font(Font.system(size = 80 * squareSize / 100))
                                }
                            }
                        }
                            .frame(width = squareSize, height = squareSize)
                    }
                }
            }
        }
            .padding(1.0)
        Rectangle()
            .stroke(settings.foregroundColorColor, lineWidth = 1.0)
            .background(Color.Transparent)
    }
        .foregroundStyle(settings.foregroundColorColor)
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
            SwiftUIView {
                BingoView(settings = settings0, sceneWidget = sceneWidget0, canvasSize = canvasSize0)
            }
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
