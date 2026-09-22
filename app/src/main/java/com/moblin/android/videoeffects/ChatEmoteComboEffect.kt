package com.moblin.android.videoeffects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.CacheAsyncImage
import com.moblin.android.various.ChatPost
import com.moblin.android.various.SimpleTimer
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetChatEmoteCombo
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val borderWidth: Double = 1.5

private val mainScope = CoroutineScope(Dispatchers.Main)

private class EmoteComboState {
    private val _emoteUrl = MutableStateFlow<String?>(null)
    val emoteUrl: StateFlow<String?> = _emoteUrl.asStateFlow()
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun updateEmoteUrl(value: String?) {
        _emoteUrl.value = value
    }

    fun updateCount(value: Int) {
        _count.value = value
    }
}

private fun widgetSize(sceneWidget: SettingsSceneWidget, canvasSize: Size): Double {
    return toPixels(sceneWidget.layout.size, minOf(canvasSize.width, canvasSize.height))
}

@Composable
private fun EmoteComboView(
    state: EmoteComboState,
    sceneWidget: SettingsSceneWidget,
    canvasSize: Size,
) {
    val emoteUrl by state.emoteUrl.collectAsState()
    val count by state.count.collectAsState()
    val url = emoteUrl ?: return
    val size = widgetSize(sceneWidget, canvasSize)
    Row(
        horizontalArrangement = Arrangement.spacedBy((size * 0.15).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CacheAsyncImage(
            url = url,
            modifier = Modifier.size(size.dp),
        )
        Box {
            Text(
                text = "x${count} combo!",
                style = TextStyle(
                    fontSize = (size / 2).sp,
                    fontWeight = FontWeight.Bold,
                    drawStyle = Stroke(width = borderWidth.toFloat()),
                ),
                color = Color.Black,
            )
            Text(
                text = "x${count} combo!",
                style = TextStyle(
                    fontSize = (size / 2).sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
            )
        }
    }
}

class ChatEmoteComboEffect(private val canvasSize: Size) : VideoEffect() {
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var sceneWidgetPipeline = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var comboImage: EffectImageCgImage? = null
    private var renderer: Any? = null
    private var cancellable: Job? = null
    private val state = EmoteComboState()
    private var settings = SettingsWidgetChatEmoteCombo()
    private var currentEmoteUrl: String? = null
    private var comboCount: Int = 0
    private val timer = SimpleTimer(mainScope)

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
        if (emoteUrl == currentEmoteUrl) {
            comboCount += 1
        } else {
            currentEmoteUrl = emoteUrl
            comboCount = 1
        }
        if (comboCount >= settings.minimumCombo) {
            state.updateEmoteUrl(emoteUrl)
            state.updateCount(comboCount)
        }
        timer.startSingleShot(timeout = settings.resetAfter.toDouble()) {
            currentEmoteUrl = null
            comboCount = 0
            setComboImage(null)
        }
    }

    private fun setup() {
        cancellable?.cancel()
        renderer = TODO("no Android counterpart for SwiftUI ImageRenderer")
        cancellable = mainScope.launch {
            combine(state.emoteUrl, state.count) { _, _ -> }
                .collect {
                    val image: EffectImageCgImage? = TODO("no Android counterpart for SwiftUI ImageRenderer")
                    if (comboCount > 0) {
                        setComboImage(image)
                    }
                }
        }
        setComboImage(null)
    }

    private fun setComboImage(image: EffectImageCgImage?) {
        val comboImage = image
        processorPipelineQueue.launch {
            this@ChatEmoteComboEffect.comboImage = comboImage
        }
    }

    override fun execute(image: EffectImageCiImage, info: VideoEffectInfo): EffectImageCiImage {
        val comboImage = comboImage ?: return image
        return comboImage.getCiImage()
            .move(sceneWidgetPipeline.layout, image.extent.size)
            .cropped(to = image.extent)
            .composited(over = image)
    }

    override fun executeMetalPetal(image: EffectImage, info: VideoEffectInfo): EffectImage {
        TODO("no Android counterpart for MetalPetal")
    }
}
