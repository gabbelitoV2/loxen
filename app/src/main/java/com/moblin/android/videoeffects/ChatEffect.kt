package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.media.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.moblin.android.common.various.*
import com.moblin.android.media.haishinkit.media.*
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.*
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.various.model.chat.ChatProvider
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetChat
import com.moblin.android.view.utils.ChatLineContent
import com.moblin.android.view.utils.ChatLineStyle
import com.moblin.android.view.utils.ChatLineUiView
import com.moblin.android.view.utils.EmotesPlayer
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private fun makeChatLineStyle(settings: SettingsWidgetChat): ChatLineStyle {
    return ChatLineStyle(
        fontSize = settings.fontSize.toFloat(),
        borderColor = if (settings.shadowColorEnabled) settings.shadowColor.color() else null,
        borderWidth = 1.5f,
        backgroundColor = if (settings.backgroundColorEnabled) {
            settings.backgroundColor.color().copy(alpha = 0.6f)
        } else {
            null
        },
        messageColor = settings.messageColor.color(),
        boldUsername = settings.boldUsername,
        boldMessage = settings.boldMessage,
        badges = settings.badges,
        sharedChatIcons = settings.sharedChatIcons,
        bigGifScale = 3f,
        highlightSymbolColor = Color.White,
        highlightDefaultColor = settings.messageColorColor,
        nicknames = settings.nicknames,
        displayStyle = settings.displayStyle,
    )
}

private data class ChatLineKey(val postId: Int, val highlight: Boolean)

private class BarLayer {
    var color: Color = Color.Transparent
    var frame: Rect = Rect.Zero
}

private class ChatRenderer(
    private val settings: SettingsWidgetChat,
    private val chat: ChatProvider,
    private val onImage: (Bitmap?) -> Unit,
) {
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val containerView: Any? = null
    private val lineViews = mutableMapOf<ChatLineKey, ChatLineUiView>()
    private val barLayers = mutableListOf<BarLayer>()
    private var cancellables = mutableListOf<Job>()
    private var stateCancellables = mutableListOf<Job>()
    private var renderPending = false

    private val width: Float
        get() = 20f * settings.fontSize

    init {
        cancellables.add(mainScope.launch {
            chat.posts.collect {
                scheduleRender()
            }
        })
        cancellables.add(mainScope.launch {
            chat.moreThanOneStreamingPlatform.collect {
                scheduleRender()
            }
        })
        cancellables.add(mainScope.launch {
            TODO("Combine objectWillChange has no Android counterpart")
        })
        cancellables.add(mainScope.launch {
            EmotesPlayer.shared.sizesVersion.drop(1).collect {
                scheduleRender()
            }
        })
        scheduleRender()
    }

    fun stop() {
        cancellables.forEach { it.cancel() }
        cancellables.clear()
        stateCancellables.forEach { it.cancel() }
        stateCancellables.clear()
        for (lineView in lineViews.values) {
            lineView.unregister()
        }
    }

    private fun scheduleRender() {
        if (renderPending) {
            return
        }
        renderPending = true
        mainScope.launch {
            renderPending = false
            render()
        }
    }

    private fun lineView(key: ChatLineKey): ChatLineUiView {
        val existing = lineViews[key]
        if (existing != null) {
            return existing
        }
        val lineView = ChatLineUiView(TODO("no Android Context available"))
        lineView.onImageLoaded = {
            scheduleRender()
        }
        TODO("UIView.addSubview has no Android counterpart; attach the chat line to the Android drawing container")
        lineViews[key] = lineView
        return lineView
    }

    private fun barLayer(index: Int): BarLayer {
        while (barLayers.size <= index) {
            val barLayer = BarLayer()
            barLayers.add(barLayer)
        }
        return barLayers[index]
    }

    private fun place(
        lineView: ChatLineUiView,
        content: ChatLineContent,
        x: Float,
        y: Float,
    ): IntSize {
        lineView.setContent(content)
        return lineView.size(availableWidth = width - x)
    }

    private fun render() {
        val posts = chat.posts.value
            .take(settings.maximumNumberOfMessages)
            .reversed()
            .filter { !it.state.deleted.value }
        stateCancellables.forEach { it.cancel() }
        stateCancellables = posts.map { post ->
            mainScope.launch {
                TODO("Combine objectWillChange has no Android counterpart")
            }
        }.toMutableList()
        val style = makeChatLineStyle(settings)
        val keys = mutableSetOf<ChatLineKey>()
        var barIndex = 0
        var y = 0f
        for ((index, post) in posts.withIndex()) {
            if (index > 0) {
                y += 1f
            }
            val startY = y
            var x = 3f
            var highlightImageLineView: ChatLineUiView? = null
            var highlightImageSize = IntSize.Zero
            val highlight = post.highlight
            if (highlight != null && highlight.titleSegments != null) {
                val highlightStyle = style.copy(backgroundColor = null)
                val key = ChatLineKey(post.id, true)
                keys.add(key)
                val lineView = lineView(key)
                lineView.setContent(highlightStyle.makeHighlightImageContent(highlight))
                highlightImageSize = lineView.size(availableWidth = width - x)
                highlightImageLineView = lineView
                x += highlightImageSize.width
            }
            val content = style.makeContent(
                post,
                chat.moreThanOneStreamingPlatform.value,
                false,
            )
            val key = ChatLineKey(post.id, false)
            keys.add(key)
            val size = place(lineView(key), content, x, y)
            y += size.height
            val postHighlight = post.highlight
            if (postHighlight != null) {
                val barLayer = barLayer(barIndex)
                barLayer.color = postHighlight.barColor
                barLayer.frame = Rect(Offset(0f, startY), Size(3f, y - startY))
                barIndex += 1
            }
        }
        for ((key, lineView) in lineViews.toList()) {
            if (!keys.contains(key)) {
                lineView.unregister()
                TODO("UIView.removeFromSuperview has no Android counterpart; detach the chat line from the Android drawing container")
                lineViews.remove(key)
            }
        }
        while (barLayers.size > barIndex) {
            barLayers.removeAt(barLayers.lastIndex)
        }
        if (y <= 0f) {
            onImage(null)
            return
        }
        TODO("set the container size and draw every ChatLineUiView into a Bitmap with android.graphics.Canvas; UIView.frame, UIView.layoutIfNeeded, CALayer.displayIfNeeded and UIGraphicsImageRenderer have no Android counterpart")
        val bitmap = Bitmap.createBitmap(width.toInt(), y.toInt(), Bitmap.Config.ARGB_8888)
        onImage(bitmap)
    }
}

class ChatEffect(private val chat: ChatProvider) : VideoEffect() {
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var chatImage: EffectImageCgImage? = null
    private var renderer: ChatRenderer? = null
    private var settings = SettingsWidgetChat()
    private var height: Double = 1.0
    private var started = false
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun start() {
        if (started) {
            return
        }
        started = true
        mainScope.launch {
            startInternal()
        }
    }

    fun stop() {
        if (!started) {
            return
        }
        started = false
        mainScope.launch {
            stopInternal()
        }
    }

    fun setSceneWidget(sceneWidget: SettingsSceneWidget) {
        processorPipelineQueue.launch {
            this@ChatEffect.sceneWidget = sceneWidget
        }
    }

    fun setSettings(settings: SettingsWidgetChat) {
        this.settings.update(other = settings)
        val maximumHeight = settings.height
        processorPipelineQueue.launch {
            this@ChatEffect.height = maximumHeight.toDouble()
        }
    }

    private fun startInternal() {
        renderer = ChatRenderer(settings, chat) { image ->
            setChatImage(image)
        }
    }

    private fun stopInternal() {
        renderer?.stop()
        renderer = null
    }

    private fun setChatImage(image: Bitmap?) {
        val chatImage = image?.toEffectImage()
        processorPipelineQueue.launch {
            this@ChatEffect.chatImage = chatImage
        }
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        var chatImage = chatImage?.getCiImage() ?: return image
        val height = image.height * this.height
        if (chatImage.extent.height > height) {
            chatImage = TODO("CIImage.cropped has no Android counterpart")
        }
        return chatImage
            .move(sceneWidget.layout, Size(image.width.toFloat(), image.height.toFloat()))
            .let { TODO("CIImage.cropped and CIImage.composited have no Android counterpart") }
    }

    override fun executeMetalPetal(
        image: Image,
        info: VideoEffectInfo,
    ): Image {
        val chatImage = chatImage?.getMetalPetalImage() ?: return image
        var contentRegion = chatImage.extent
        val height = image.height * this.height
        if (contentRegion.height > height) {
            contentRegion = Rect(
                Offset(contentRegion.left, contentRegion.bottom - height.toFloat()),
                Size(contentRegion.width, height.toFloat()),
            )
        }
        return chatImage.moveComposited(
            sceneWidget.layout,
            TODO("the video unit image cannot be used as an MTIImage"),
            contentRegion,
        ).let { TODO("MTIImage has no android.media.Image counterpart") }
    }
}
