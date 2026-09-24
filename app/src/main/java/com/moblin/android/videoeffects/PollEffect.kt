package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.Published
import com.moblin.android.platform.swiftui.layout.EdgeSet
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.SwiftUIView
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.cornerRadius
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.padding
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class PollState(val size: CGSize) {
    var text: String by Published(localized("No votes yet"))
}

private fun scaledFontSize(size: CGSize): Double {
    return 30 * (size.maximum() / 1920)
}

internal fun ViewBuilder.PollView(state: PollState): View = HStack {
    Image(systemName = "chart.bar.xaxis")
    Text(state.text)
}
    .padding(EdgeSet.trailing, 7.0)
    .background(Color.Black.copy(alpha = 0.75f))
    .foregroundStyle(Color.White)
    .font(Font.system(size = scaledFontSize(size = state.size)))
    .cornerRadius(10.0)

class PollEffect(canvasSize: Size) : VideoEffect() {
    private val filter = CIFilter.sourceOverCompositing()
    private var overlay: EffectImageCgImage? = null
    private var renderer: ImageRenderer? = null
    private var cancellable: AnyCancellable? = null
    private val state: PollState

    init {
        state = PollState(size = canvasSize.toCGSize())
        CoroutineScope(Dispatchers.Main.immediate).launch {
            setup()
        }
    }

    fun updateText(text: String) {
        if (state.text == text) {
            return
        }
        state.text = text
    }

    private fun setup() {
        val state0 = state
        renderer = ImageRenderer(content = { SwiftUIView { PollView(state = state0) } })
        val weakSelf = WeakReference(this)
        cancellable = renderer?.objectWillChange?.sink {
            val self = weakSelf.get() ?: return@sink
            self.setOverlay(image = self.renderer?.cgImage)
        }
        setOverlay(image = renderer?.cgImage)
    }

    private fun setOverlay(image: Bitmap?) {
        val overlay = image?.toEffectImage()
        processorPipelineQueue.launch {
            this@PollEffect.overlay = overlay
        }
    }

    private fun moveToTopRight(image: CIImage, size: CGSize): CIImage {
        val x = size.width - image.extent.width
        return image
            .translated(x = x - 5, y = size.height - image.extent.height - 5)
            .cropped(to = CGRect(x = 0.0, y = 0.0, width = size.width, height = size.height))
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val overlay = overlay ?: return image
        filter.inputImage = moveToTopRight(image = overlay.getCiImage(), size = image.extent.size)
        filter.backgroundImage = image
        return filter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val overlay = overlay?.getMetalPetalImage() ?: return image
        val size = overlay.extent.size
        val position = CGPoint(x = image.extent.width - size.width / 2 - 5, y = size.height / 2 + 5)
        return overlay.positionComposited(position = position, backgroundImage = image)
    }
}
