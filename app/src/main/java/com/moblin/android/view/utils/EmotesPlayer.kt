package com.moblin.android.view.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.moblin.android.AppDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

private val maxFramesBytes = 256 * 1024 * 1024
private const val animatedFrameRate = 15.0
private const val emotesPlayerTag = "EmotesPlayer"
private const val symbolRenderScale = 3f

sealed class ChatImageSource {
    data class Url(val url: String) : ChatImageSource()

    data class Asset(val name: String) : ChatImageSource()

    data class Symbol(val name: String, val size: Float, val color: Color) : ChatImageSource()
}

internal data class EmoteBorder(val color: Color, val width: Int)

internal data class EmoteKey(
    val source: ChatImageSource,
    val animated: Boolean,
    val height: Int,
    val border: EmoteBorder?,
)

private class WeakEmoteUiView(view: EmoteUiView) {
    private val reference = WeakReference(view)

    val view: EmoteUiView?
        get() = reference.get()
}

private class BitmapCanvas(val bitmap: Bitmap, val canvas: Canvas)

private class EmoteImage(val bitmap: Bitmap, val movie: Movie?) {
    val width: Int
        get() = movie?.width() ?: bitmap.width

    val height: Int
        get() = movie?.height() ?: bitmap.height

    val size: Size
        get() = Size(width.toFloat(), height.toFloat())
}

private fun makeBitmapContext(width: Int, height: Int): BitmapCanvas? {
    if (width <= 0 || height <= 0) {
        return null
    }
    return runCatching {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        BitmapCanvas(bitmap, Canvas(bitmap))
    }.getOrNull()
}

private fun highQualityPaint(): Paint = Paint().apply {
    isFilterBitmap = true
    isAntiAlias = true
}

private fun drawBitmapIn(canvas: Canvas, bitmap: Bitmap, destination: RectF, paint: Paint) {
    canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), destination, paint)
}

private fun secondsBetween(startNanos: Long, endNanos: Long): Double =
    (endNanos - startNanos) / 1_000_000_000.0

private class AnimatedEmote(
    private val player: EmotesPlayer,
    image: EmoteImage,
    key: EmoteKey,
) {
    private val border: EmoteBorder? = key.border
    private val height: Int = key.height
    private val width: Int
    private val contentRect: RectF
    private val startTime: Long = System.nanoTime()
    private val frames: MutableList<Bitmap?>
    private val startTimes: MutableList<Double>
    private val totalDuration: Double
    private val movie: Movie?
    private val sourceImage: Bitmap?
    private var currentIndex = -1
    private var views: MutableList<WeakEmoteUiView> = mutableListOf()
    private var contentContext: BitmapCanvas? = null
    private var renderedFrames = 0

    var framesBytes = 0
        private set

    var lastUsedTime: Long = System.nanoTime()
        private set

    init {
        val borderWidth = key.border?.width ?: 0
        val imageHeight = image.height
        val imageWidth = image.width
        val contentHeight = max(height - 2 * borderWidth, 1)
        val aspectRatio = if (imageHeight > 0) imageWidth.toFloat() / imageHeight.toFloat() else 1f
        val contentWidth = max((contentHeight * aspectRatio).roundToInt(), 1)
        contentRect = RectF(
            borderWidth.toFloat(),
            borderWidth.toFloat(),
            (contentWidth + borderWidth).toFloat(),
            (contentHeight + borderWidth).toFloat(),
        )
        width = contentWidth + 2 * borderWidth
        val animatedMovie = if (key.animated) image.movie else null
        if (animatedMovie != null && animatedMovie.duration() > 0) {
            movie = animatedMovie
            sourceImage = null
            val durationMs = animatedMovie.duration()
            val frameCount = max((durationMs / 1000.0 * animatedFrameRate).roundToInt(), 1)
            val frameDuration = durationMs / 1000.0 / frameCount
            val frameList = MutableList<Bitmap?>(frameCount) { null }
            val timeList = mutableListOf<Double>()
            var time = 0.0
            for (index in 0 until frameCount) {
                timeList.add(time)
                time += max(frameDuration, 0.01)
            }
            frames = frameList
            startTimes = timeList
            totalDuration = time
        } else {
            movie = null
            sourceImage = image.bitmap
            frames = mutableListOf(null)
            startTimes = mutableListOf(0.0)
            totalDuration = 0.0
        }
    }

    fun isAnimated(): Boolean {
        return movie != null
    }

    fun isUsed(): Boolean {
        views.removeAll { it.view == null }
        return views.isNotEmpty()
    }

    fun add(view: EmoteUiView, now: Long) {
        views.add(WeakEmoteUiView(view))
        lastUsedTime = now
        if (currentIndex == -1) {
            update(now)
        } else {
            view.setFrame(frames[currentIndex])
        }
    }

    fun remove(view: EmoteUiView) {
        views.removeAll { it.view == null || it.view === view }
        lastUsedTime = System.nanoTime()
    }

    fun update(now: Long) {
        val index = frameIndex(now)
        if (index == currentIndex) {
            return
        }
        currentIndex = index
        val frame = frame(index)
        for (view in views) {
            view.view?.setFrame(frame)
        }
    }

    private fun frameIndex(now: Long): Int {
        if (totalDuration <= 0) {
            return 0
        }
        val offset = secondsBetween(startTime, now) % totalDuration
        var index = startTimes.size - 1
        while (index > 0 && startTimes[index] > offset) {
            index -= 1
        }
        return index
    }

    private fun frame(index: Int): Bitmap? {
        if (index < 0 || index >= frames.size) {
            return null
        }
        val cached = frames[index]
        if (cached != null) {
            return cached
        }
        val sourceFrame = renderMovieFrame(index) ?: sourceImage
        val frame = render(sourceFrame)
        frames[index] = frame
        if (frame != null) {
            val bytes = frame.rowBytes * frame.height
            framesBytes += bytes
            player.addFramesBytes(bytes)
            renderedFrames += 1
            if (renderedFrames == frames.size) {
                contentContext = null
            }
        }
        return frame
    }

    private fun renderMovieFrame(index: Int): Bitmap? {
        val movie = movie ?: return null
        if (index < 0 || index >= startTimes.size) {
            return null
        }
        movie.setTime((startTimes[index] * 1000.0).toInt())
        val movieWidth = movie.width()
        val movieHeight = movie.height()
        if (movieWidth <= 0 || movieHeight <= 0) {
            return null
        }
        val context = makeBitmapContext(movieWidth, movieHeight) ?: return null
        context.canvas.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
        movie.draw(context.canvas, 0f, 0f)
        return context.bitmap
    }

    private fun render(sourceFrame: Bitmap?): Bitmap? {
        if (sourceFrame == null) {
            return null
        }
        val border = border
        if (border == null) {
            val context = makeBitmapContext(width, height) ?: return null
            drawBitmapIn(context.canvas, sourceFrame, contentRect, highQualityPaint())
            return context.bitmap
        }
        val content = renderContent(sourceFrame) ?: return null
        val context = makeBitmapContext(width, height) ?: return null
        val straight = border.width.toFloat()
        val diagonal = (straight * 0.7f).roundToInt().toFloat()
        val offsets = listOf(
            straight to 0f,
            -straight to 0f,
            0f to straight,
            0f to -straight,
            diagonal to diagonal,
            diagonal to -diagonal,
            -diagonal to diagonal,
            -diagonal to -diagonal,
        )
        val paint = highQualityPaint()
        for ((dx, dy) in offsets) {
            val rect = RectF(contentRect)
            rect.offset(dx, dy)
            drawBitmapIn(context.canvas, content, rect, paint)
        }
        val borderPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            color = border.color.toArgb()
        }
        context.canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), borderPaint)
        drawBitmapIn(context.canvas, content, contentRect, highQualityPaint())
        return context.bitmap
    }

    private fun renderContent(sourceFrame: Bitmap): Bitmap? {
        val rect = RectF(0f, 0f, contentRect.width(), contentRect.height())
        if (contentContext == null) {
            contentContext = makeBitmapContext(rect.width().toInt(), rect.height().toInt())
        }
        val contentContext = contentContext ?: return null
        contentContext.canvas.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
        drawBitmapIn(contentContext.canvas, sourceFrame, rect, highQualityPaint())
        return contentContext.bitmap
    }
}

class EmotesPlayer private constructor(private val context: Context) {
    companion object {
        val shared: EmotesPlayer by lazy { EmotesPlayer(AppDelegate.context) }
    }

    val sizesVersion = MutableStateFlow(0)

    private val emotes: MutableMap<EmoteKey, AnimatedEmote> = mutableMapOf()
    private val animatingEmotes: MutableMap<EmoteKey, AnimatedEmote> = mutableMapOf()
    private var framesBytes = 0
    private var latestEvictTime = System.nanoTime()
    private val pendingViews: MutableMap<EmoteKey, MutableList<WeakEmoteUiView>> = mutableMapOf()
    private val sizes: MutableMap<ChatImageSource, Size> = mutableMapOf()
    private val localImages: MutableMap<ChatImageSource, EmoteImage> = mutableMapOf()
    private val loadingHandlers: MutableMap<String, MutableList<(EmoteImage) -> Unit>> = mutableMapOf()
    private val failedUrls: MutableMap<String, Long> = mutableMapOf()
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var tickJob: Job? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun size(source: ChatImageSource): Size? {
        val knownSize = sizes[source]
        if (knownSize != null) {
            return knownSize
        }
        var loading = true
        load(source) { image ->
            if (sizes[source] != null) {
                return@load
            }
            sizes[source] = image.size
            if (!loading) {
                sizesVersion.value += 1
            }
        }
        loading = false
        return sizes[source]
    }

    internal fun register(view: EmoteUiView, key: EmoteKey) {
        val emote = emotes[key]
        if (emote != null) {
            emote.add(view, System.nanoTime())
            startAnimating(key, emote)
        } else {
            pendingViews.getOrPut(key) { mutableListOf() }.add(WeakEmoteUiView(view))
            load(key.source) { image ->
                val views = pendingViews.remove(key) ?: return@load
                if (views.isEmpty()) {
                    return@load
                }
                val newEmote = emotes[key] ?: AnimatedEmote(this, image, key)
                emotes[key] = newEmote
                val now = System.nanoTime()
                for (weakView in views) {
                    weakView.view?.let { newEmote.add(it, now) }
                }
                startAnimating(key, newEmote)
                evictIfNeeded(now)
            }
        }
    }

    internal fun unregister(view: EmoteUiView, key: EmoteKey) {
        val emote = emotes[key]
        if (emote != null) {
            emote.remove(view)
            if (!emote.isUsed()) {
                animatingEmotes.remove(key)
            }
        }
        pendingViews[key]?.removeAll { it.view == null || it.view === view }
        updateDisplayLink()
    }

    internal fun addFramesBytes(bytes: Int) {
        framesBytes += bytes
    }

    private fun removeFramesBytes(bytes: Int) {
        framesBytes -= bytes
    }

    private fun startAnimating(key: EmoteKey, emote: AnimatedEmote) {
        if (emote.isAnimated()) {
            animatingEmotes[key] = emote
        }
        updateDisplayLink()
    }

    private fun load(source: ChatImageSource, onLoaded: (EmoteImage) -> Unit) {
        if (source is ChatImageSource.Url) {
            load(source.url, onLoaded)
            return
        }
        val localImage = localImages[source]
        if (localImage != null) {
            onLoaded(localImage)
            return
        }
        val image = render(source) ?: return
        localImages[source] = image
        onLoaded(image)
    }

    private fun render(source: ChatImageSource): EmoteImage? {
        return when (source) {
            is ChatImageSource.Url -> null
            is ChatImageSource.Asset -> {
                val id = context.resources.getIdentifier(source.name, "drawable", context.packageName)
                if (id == 0) {
                    null
                } else {
                    BitmapFactory.decodeResource(context.resources, id)?.let { EmoteImage(it, null) }
                }
            }
            is ChatImageSource.Symbol -> renderSymbol(source.name, source.size, source.color)
        }
    }

    private fun renderSymbol(name: String, pointSize: Float, color: Color): EmoteImage? {
        val resourceId = context.resources.getIdentifier(
            name.replace(".", "_"),
            "drawable",
            context.packageName,
        )
        if (resourceId == 0) {
            return null
        }
        val drawable = context.getDrawable(resourceId) ?: return null
        drawable.setTint(color.toArgb())
        val width = max((pointSize * symbolRenderScale).roundToInt(), 1)
        val intrinsicWidth = max(drawable.intrinsicWidth, 1)
        val intrinsicHeight = max(drawable.intrinsicHeight, 1)
        val height = max((width.toFloat() * intrinsicHeight / intrinsicWidth).roundToInt(), 1)
        val context = makeBitmapContext(width, height) ?: return null
        drawable.setBounds(0, 0, width, height)
        drawable.draw(context.canvas)
        return EmoteImage(context.bitmap, null)
    }

    private fun load(url: String, onLoaded: (EmoteImage) -> Unit) {
        val existingHandlers = loadingHandlers[url]
        if (existingHandlers != null) {
            existingHandlers.add(onLoaded)
            return
        }
        val failedTime = failedUrls[url]
        if (failedTime != null && secondsBetween(failedTime, System.nanoTime()) < 60.0) {
            return
        }
        loadingHandlers[url] = mutableListOf(onLoaded)
        httpClient.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainScope.launch {
                    loadingHandlers.remove(url)
                    failedUrls[url] = System.nanoTime()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bytes = runCatching { response.use { it.body?.bytes() } }.getOrNull()
                val image = bytes?.let { decodeEmoteImage(it) }
                mainScope.launch {
                    val handlers = loadingHandlers.remove(url) ?: return@launch
                    if (image == null || image.width <= 0 || image.height <= 0) {
                        failedUrls[url] = System.nanoTime()
                        return@launch
                    }
                    failedUrls.remove(url)
                    for (handler in handlers) {
                        handler(image)
                    }
                }
            }
        })
    }

    private fun decodeEmoteImage(bytes: ByteArray): EmoteImage? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val movie = runCatching { Movie.decodeByteArray(bytes, 0, bytes.size) }
            .getOrNull()
            ?.takeIf { it.duration() > 0 }
        return EmoteImage(bitmap, movie)
    }

    private fun updateDisplayLink() {
        val isAnimating = animatingEmotes.isNotEmpty()
        if (isAnimating && tickJob == null) {
            tickJob = mainScope.launch {
                while (isActive) {
                    tick()
                    delay(66L)
                }
            }
        } else if (!isAnimating && tickJob != null) {
            tickJob?.cancel()
            tickJob = null
        }
    }

    private fun tick() {
        val now = System.nanoTime()
        val unusedKeys = mutableListOf<EmoteKey>()
        for ((key, emote) in animatingEmotes) {
            if (emote.isUsed()) {
                emote.update(now)
            } else {
                unusedKeys.add(key)
            }
        }
        for (key in unusedKeys) {
            animatingEmotes.remove(key)
        }
        if (unusedKeys.isNotEmpty()) {
            updateDisplayLink()
        }
        evictIfNeeded(now)
    }

    private fun evictIfNeeded(now: Long) {
        if (framesBytes <= maxFramesBytes) {
            return
        }
        if (secondsBetween(latestEvictTime, now) <= 1.0) {
            return
        }
        latestEvictTime = now
        localImages.clear()
        val oldest = emotes.entries
            .filter { !it.value.isUsed() }
            .sortedBy { it.value.lastUsedTime }
            .take(50)
        Log.d(emotesPlayerTag, "emotes-player: Evicting ${oldest.size} emotes")
        for ((key, emote) in oldest) {
            emotes.remove(key)
            animatingEmotes.remove(key)
            removeFramesBytes(emote.framesBytes)
        }
    }
}

class EmoteUiView {
    var onLoaded: (() -> Unit)? = null
    private var source: ChatImageSource? = null
    private var animated = true
    private var borderColor: Color? = null
    private var borderWidth = 0f
    private var key: EmoteKey? = null
    private var loaded = false
    private val _frame = MutableStateFlow<Bitmap?>(null)

    val frame: StateFlow<Bitmap?> = _frame.asStateFlow()

    private var boundsWidth = 0
    private var boundsHeight = 0
    private var displayScale = 1f

    fun setEmote(
        source: ChatImageSource,
        animated: Boolean = true,
        borderColor: Color? = null,
        borderWidth: Float = 0f,
    ) {
        this.source = source
        this.animated = animated
        this.borderColor = borderColor
        this.borderWidth = borderWidth
        updateKey()
    }

    fun unregister() {
        val key = key
        if (key != null) {
            EmotesPlayer.shared.unregister(this, key)
            this.key = null
        }
    }

    internal fun setFrame(image: Bitmap?) {
        _frame.value = image
        if (image != null && !loaded) {
            loaded = true
            onLoaded?.invoke()
        }
    }

    fun onSizeChanged(width: Int, height: Int, displayScale: Float) {
        boundsWidth = width
        boundsHeight = height
        this.displayScale = displayScale
        updateKey()
    }

    private fun updateKey() {
        val source = source
        if (source == null) {
            unregister()
            setFrame(null)
            return
        }
        val scale = if (displayScale > 0f) displayScale else 1f
        val height = (boundsHeight * scale).roundToInt()
        if (height <= 0) {
            return
        }
        var border: EmoteBorder? = null
        val borderColor = borderColor
        if (borderColor != null && borderWidth > 0f) {
            border = EmoteBorder(borderColor, (borderWidth * scale).roundToInt())
        }
        val key = EmoteKey(source, animated, height, border)
        if (key == this.key) {
            return
        }
        unregister()
        this.key = key
        loaded = false
        EmotesPlayer.shared.register(this, key)
    }
}

@Composable
fun EmoteUiView(
    source: ChatImageSource,
    animated: Boolean = true,
    borderColor: Color? = null,
    borderWidth: Float = 0f,
    modifier: Modifier = Modifier,
    onLoaded: (() -> Unit)? = null,
) {
    val view = remember { EmoteUiView() }
    val density = LocalDensity.current
    val frame by view.frame.collectAsState()
    SideEffect {
        view.onLoaded = onLoaded
    }
    LaunchedEffect(source, animated, borderColor, borderWidth) {
        view.setEmote(source, animated, borderColor, borderWidth)
    }
    DisposableEffect(view) {
        onDispose {
            view.unregister()
        }
    }
    Box(
        modifier = modifier.onSizeChanged {
            val scale = if (density.density > 0f) density.density else 1f
            view.onSizeChanged(
                (it.width / scale).roundToInt(),
                (it.height / scale).roundToInt(),
                scale,
            )
        },
    ) {
        val bitmap = frame
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
