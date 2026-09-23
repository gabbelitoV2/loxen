package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.combine.sink
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.ChatPost
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetChatEmoteCombo
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private val borderWidth = 1.5

private class EmoteComboState {
    val emoteUrl = MutableStateFlow<URI?>(null)
    val count = MutableStateFlow(0)
}

private fun widgetSize(sceneWidget: SettingsSceneWidget, canvasSize: CGSize): Double {
    return toPixels(sceneWidget.layout.size, canvasSize.minimum())
}

@Composable
private fun EmoteComboView(state: EmoteComboState, sceneWidget: SettingsSceneWidget, canvasSize: CGSize) {
    val emoteUrl by state.emoteUrl.collectAsState()
    val count by state.count.collectAsState()
    val url = emoteUrl
    if (url != null) {
        val size = widgetSize(sceneWidget = sceneWidget, canvasSize = canvasSize)
        val strokeWidth = with(LocalDensity.current) { borderWidth.dp.toPx() }
        val textStyle = SwiftUIFonts.system(size / 2.0, FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy((size * 0.15).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(size.dp)) {
                CacheAsyncImage(
                    url = url,
                    content = { image ->
                        Image(
                            bitmap = image,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    },
                    placeholder = {},
                )
            }
            Box {
                Text(
                    text = "x$count combo!",
                    style = textStyle.copy(drawStyle = Stroke(width = strokeWidth)),
                    color = Color.Black,
                )
                Text(
                    text = "x$count combo!",
                    style = textStyle,
                    color = Color.White,
                )
            }
        }
    }
}

class ChatEmoteComboEffect(private val canvasSize: CGSize) : VideoEffect() {
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var sceneWidgetPipeline = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var comboImage: EffectImageCgImage? = null
    private var renderer: ImageRenderer? = null
    private var cancellable: AnyCancellable? = null
    private val state = EmoteComboState()
    private var settings = SettingsWidgetChatEmoteCombo()
    private var currentEmoteUrl: String? = null
    private var comboCount: Int = 0
    private val timer = SimpleTimer(Dispatchers.Main)

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            sceneWidgetPipeline.layout = sceneWidget.layout
        }
        this.sceneWidget.layout = sceneWidget.layout
    }

    fun setSettings(settings: SettingsWidgetChatEmoteCombo) {
        if (settings === this.settings) {
            return
        }
        this.settings = settings
        setup()
    }

    fun appendMessage(post: ChatPost) {
        val emoteUrl = post.segments.firstOrNull()?.url?.url(animated = false) ?: return
        val emoteUri = runCatching { URI(emoteUrl) }.getOrNull() ?: return
        if (emoteUrl == currentEmoteUrl) {
            comboCount += 1
        } else {
            currentEmoteUrl = emoteUrl
            comboCount = 1
        }
        if (comboCount >= settings.minimumCombo) {
            state.emoteUrl.value = emoteUri
            state.count.value = comboCount
        }
        timer.startSingleShot(timeout = settings.resetAfter.toDouble()) {
            currentEmoteUrl = null
            comboCount = 0
            setComboImage(image = null)
        }
    }

    private fun setup() {
        cancellable?.cancel()
        val state0 = state
        val sceneWidget0 = sceneWidget
        val canvasSize0 = canvasSize
        renderer = ImageRenderer(content = {
            EmoteComboView(state = state0, sceneWidget = sceneWidget0, canvasSize = canvasSize0)
        })
        val weakSelf = java.lang.ref.WeakReference(this)
        cancellable = renderer?.objectWillChange?.sink {
            val self = weakSelf.get()
            if (self != null) {
                val image = self.renderer?.cgImage
                if (self.comboCount > 0) {
                    self.setComboImage(image = image)
                }
            }
        }
        renderer?.cgImage
        setComboImage(image = null)
    }

    private fun setComboImage(image: Bitmap?) {
        val comboImage = image?.toEffectImage()
        processorPipelineQueue.launch {
            this@ChatEmoteComboEffect.comboImage = comboImage
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        val comboImage = comboImage ?: return image
        return comboImage.getCiImage()
            .move(sceneWidgetPipeline.layout, image.extent.size)
            .cropped(to = image.extent)
            .composited(over = image)
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        return comboImage?.getMetalPetalImage()
            ?.moveComposited(sceneWidgetPipeline.layout, image) ?: image
    }
}
