package com.moblin.android.videoeffects

import com.moblin.android.platform.video.CVPixelBuffer as Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private class PollState(val size: Size) {
    val text = MutableStateFlow(localized("No votes yet"))

    fun setText(text: String) {
        this.text.value = text
    }
}

private fun scaledFontSize(size: Size): Float {
    return 30 * (maxOf(size.width, size.height) / 1920f)
}

@Composable
private fun PollView(state: PollState, modifier: Modifier = Modifier) {
    val text by state.text.collectAsState()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .padding(end = 7.dp),
    ) {
        Icon(
            imageVector = Icons.Default.List,
            contentDescription = null,
            tint = Color.White,
        )
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = scaledFontSize(state.size).sp,
            ),
        )
    }
}

class PollEffect(canvasSize: Size) : VideoEffect() {
    private var overlay: EffectImageCgImage? = null
    private var renderer: EffectImageCgImage? = null
    private var cancellable: Job? = null
    private val state = PollState(canvasSize)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        scope.launch {
            setup()
        }
    }

    fun updateText(text: String) {
        if (state.text.value == text) {
            return
        }
        state.setText(text)
    }

    private fun setup() {
        renderer = null
        cancellable = scope.launch {
            state.text.collect {
                setOverlay(renderer)
            }
        }
        setOverlay(renderer)
    }

    private fun setOverlay(image: EffectImageCgImage?) {
        processorPipelineQueue.launch {
            overlay = image
        }
    }

    private fun moveToTopRight(image: EffectImageCiImage, size: Size): EffectImageCiImage {
        return TODO("no Android counterpart for CIImage translation and cropping")
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        if (overlay == null) {
            return image
        }
        return TODO("no Android counterpart for CIFilter source-over compositing")
    }

    fun executeMetalPetal(image: EffectImage, info: VideoEffectInfo): EffectImage {
        if (overlay == null) {
            return image
        }
        return TODO("no Android counterpart for MetalPetal")
    }
}
