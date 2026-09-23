package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.localized
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.SystemImage
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGSize
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.SwiftUIFonts
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private class PollState(val size: CGSize) {
    val text = MutableStateFlow(localized("No votes yet"))
}

private fun scaledFontSize(size: CGSize): Float {
    return (30 * (size.maximum() / 1920)).toFloat()
}

@Composable
private fun PollView(state: PollState) {
    val text by state.text.collectAsState()
    val fontSize = scaledFontSize(size = state.size)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(end = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalTextStyle provides SwiftUIFonts.system(size = fontSize)) {
            SystemImage(name = "chart.bar.xaxis", fontSize = fontSize.sp)
            Text(text = text, color = Color.White)
        }
    }
}

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
        if (state.text.value == text) {
            return
        }
        state.text.value = text
    }

    private fun setup() {
        val state0 = state
        renderer = ImageRenderer(content = { PollView(state = state0) })
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
