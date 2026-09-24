package com.moblin.android.videoeffects

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.moblin.android.AppDelegate
import com.moblin.android.common.various.color
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.combine.AnyCancellable
import com.moblin.android.platform.combine.dropFirst
import com.moblin.android.platform.combine.sink
import com.moblin.android.platform.coregraphics.CGColor
import com.moblin.android.platform.coregraphics.CGRect
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.uikit.CALayer
import com.moblin.android.platform.uikit.UIGraphicsImageRenderer
import com.moblin.android.platform.uikit.UIGraphicsImageRendererFormat
import com.moblin.android.platform.uikit.UIView
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.platform.uikit.frame
import com.moblin.android.platform.uikit.removeFromSuperview
import com.moblin.android.various.model.chat.ChatProvider
import com.moblin.android.various.settings.SettingsSceneWidget
import com.moblin.android.various.settings.SettingsWidgetChat
import com.moblin.android.view.utils.ChatLineContent
import com.moblin.android.view.utils.ChatLineStyle
import com.moblin.android.view.utils.ChatLineUiView
import com.moblin.android.view.utils.EmotesPlayer
import java.lang.ref.WeakReference
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        font = settings.font,
    )
}

private data class ChatLineKey(
    val postId: Int,
    val highlight: Boolean,
)

private class ChatRenderer(
    private val settings: SettingsWidgetChat,
    private val chat: ChatProvider,
    private val onImage: (Bitmap?) -> Unit,
) {
    private val containerView = UIView()
    private val lineViews = mutableMapOf<ChatLineKey, ChatLineUiView>()
    private val barLayers = mutableListOf<CALayer>()
    private var cancellables = mutableListOf<AnyCancellable>()
    private var stateCancellables = mutableListOf<AnyCancellable>()
    private var renderPending = false

    private val width: Double
        get() = 20.0 * settings.fontSize.toDouble()

    init {
        containerView.backgroundColor = Color.Transparent
        val weakSelf = WeakReference(this)
        chat.posts
            .sink { weakSelf.get()?.scheduleRender() }
            .store(into = cancellables)
        chat.moreThanOneStreamingPlatform
            .sink { weakSelf.get()?.scheduleRender() }
            .store(into = cancellables)
        EmotesPlayer.shared.sizesVersion
            .dropFirst()
            .sink { weakSelf.get()?.scheduleRender() }
            .store(into = cancellables)
        scheduleRender()
    }

    fun stop() {
        for (cancellable in cancellables) {
            cancellable.cancel()
        }
        cancellables.clear()
        for (cancellable in stateCancellables) {
            cancellable.cancel()
        }
        stateCancellables.clear()
        for (lineView in lineViews.values) {
            lineView.unregister()
        }
    }

    fun scheduleRender() {
        if (renderPending) {
            return
        }
        renderPending = true
        CoroutineScope(Dispatchers.Main).launch {
            renderPending = false
            render()
        }
    }

    private fun lineView(key: ChatLineKey): ChatLineUiView {
        lineViews[key]?.let { return it }
        val lineView = ChatLineUiView(AppDelegate.context)
        val weakSelf = WeakReference(this)
        lineView.onImageLoaded = { weakSelf.get()?.scheduleRender() }
        containerView.addSubview(lineView)
        lineViews[key] = lineView
        return lineView
    }

    private fun barLayer(index: Int): CALayer {
        while (barLayers.size <= index) {
            val barLayer = CALayer()
            barLayer.actions = mapOf<String, Any?>(
                "bounds" to null,
                "position" to null,
                "backgroundColor" to null,
            )
            containerView.layer.addSublayer(barLayer)
            barLayers.add(barLayer)
        }
        return barLayers[index]
    }

    private fun place(lineView: ChatLineUiView, content: ChatLineContent, x: Double, y: Double): CGSize {
        lineView.setContent(content)
        val size = lineView.size(availableWidth = (width - x).toFloat())
        val cgSize = CGSize(width = size.width, height = size.height)
        lineView.frame = CGRect(x = x, y = y, width = cgSize.width, height = cgSize.height)
        return cgSize
    }

    private fun render() {
        val posts = chat.posts.value
            .take(settings.maximumNumberOfMessages)
            .reversed()
            .filter { !it.state.deleted.value }
        for (cancellable in stateCancellables) {
            cancellable.cancel()
        }
        stateCancellables = posts.map { post ->
            val weakSelf = WeakReference(this)
            post.state.deleted.dropFirst().sink { weakSelf.get()?.scheduleRender() }
        }.toMutableList()
        val style = makeChatLineStyle(settings = settings)
        val keys = mutableSetOf<ChatLineKey>()
        var barIndex = 0
        var y = 0.0
        for ((index, post) in posts.withIndex()) {
            if (index > 0) {
                y += 1
            }
            val startY = y
            var x = 3.0
            var highlightImageLineView: ChatLineUiView? = null
            var highlightImageSize = CGSize.zero
            val highlight = post.highlight
            if (highlight != null && highlight.titleSegments != null) {
                val highlightStyle = style.copy(backgroundColor = null)
                val key = ChatLineKey(postId = post.id, highlight = true)
                keys.add(key)
                val lineView = lineView(key = key)
                lineView.setContent(highlightStyle.makeHighlightImageContent(highlight = highlight))
                val size = lineView.size(availableWidth = (width - x).toFloat())
                highlightImageSize = CGSize(width = size.width, height = size.height)
                highlightImageLineView = lineView
                x += highlightImageSize.width
            }
            val content = style.makeContent(
                post = post,
                platform = chat.moreThanOneStreamingPlatform.value,
                deleted = false,
            )
            val key = ChatLineKey(postId = post.id, highlight = false)
            keys.add(key)
            val size = place(lineView = lineView(key = key), content = content, x = x, y = y)
            highlightImageLineView?.frame = CGRect(
                x = 3.0,
                y = y + (size.height - highlightImageSize.height) / 2,
                width = highlightImageSize.width,
                height = highlightImageSize.height,
            )
            y += size.height
            if (highlight != null) {
                val barLayer = barLayer(index = barIndex)
                barLayer.backgroundColor = CGColor(color = highlight.barColor)
                barLayer.frame = CGRect(x = 0.0, y = startY, width = 3.0, height = y - startY)
                barIndex += 1
            }
        }
        for ((key, lineView) in lineViews.toList()) {
            if (key !in keys) {
                lineView.unregister()
                lineView.removeFromSuperview()
                lineViews.remove(key)
            }
        }
        while (barLayers.size > barIndex) {
            barLayers.removeAt(barLayers.size - 1).removeFromSuperlayer()
        }
        if (y <= 0.0) {
            onImage(null)
            return
        }
        containerView.frame = CGRect(x = 0.0, y = 0.0, width = width, height = y)
        val format = UIGraphicsImageRendererFormat()
        format.scale = 1f
        format.opaque = false
        com.moblin.android.platform.core.PipelineStats.increment("chatRenders")
        val image = UIGraphicsImageRenderer(size = containerView.bounds.size, format = format)
            .image { context ->
                containerView.draw(context.cgContext)
            }
        onImage(image.cgImage)
    }
}

class ChatEffect(private val chat: ChatProvider) : VideoEffect() {
    private var sceneWidget = SettingsSceneWidget(widgetId = UUID.randomUUID())
    private var chatImage: EffectImageCgImage? = null
    private var renderer: ChatRenderer? = null
    private var settings = SettingsWidgetChat()
    private var height: Double = 1.0
    private var started: Boolean = false

    fun start() {
        if (started) {
            return
        }
        started = true
        CoroutineScope(Dispatchers.Main.immediate).launch {
            startInternal()
        }
    }

    fun stop() {
        if (!started) {
            return
        }
        started = false
        CoroutineScope(Dispatchers.Main.immediate).launch {
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
        CoroutineScope(Dispatchers.Main.immediate).launch {
            renderer?.scheduleRender()
        }
    }

    private fun startInternal() {
        val weakSelf = WeakReference(this)
        renderer = ChatRenderer(settings = settings, chat = chat) { image ->
            weakSelf.get()?.setChatImage(image = image)
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

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        var chatImage = chatImage?.getCiImage() ?: return image
        val height = image.extent.height * this.height
        if (chatImage.extent.height > height) {
            chatImage = chatImage.cropped(
                to = CGRect(
                    x = chatImage.extent.minX,
                    y = chatImage.extent.minY,
                    width = chatImage.extent.width,
                    height = height,
                ),
            )
        }
        return chatImage
            .move(layout = sceneWidget.layout, streamSize = image.extent.size)
            .cropped(to = image.extent)
            .composited(over = image)
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val chatImage = chatImage?.getMetalPetalImage() ?: return image
        var contentRegion = chatImage.extent
        val height = image.extent.height * this.height
        if (contentRegion.height > height) {
            contentRegion = CGRect(
                x = contentRegion.minX,
                y = contentRegion.maxY - height,
                width = contentRegion.width,
                height = height,
            )
        }
        return chatImage.moveComposited(
            layout = sceneWidget.layout,
            backgroundImage = image,
            contentRegion = contentRegion,
        )
    }
}
