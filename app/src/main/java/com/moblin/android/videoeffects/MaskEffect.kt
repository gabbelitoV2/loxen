package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import com.moblin.android.platform.video.CVPixelBuffer as Image
import com.moblin.android.common.various.RgbColor
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.various.settings.SettingsMaskBackgroundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class MaskEffectPoint(var x: Double, var y: Double)

data class MaskEffectSettings(
    var points: List<MaskEffectPoint>,
    var inverted: Boolean,
    var tension: Double,
    var backgroundType: SettingsMaskBackgroundType,
    var backgroundColor: RgbColor,
    var backgroundColor2: RgbColor,
)

private val checkerboardSquareCount: Float = 20.0f

fun makeCatmullRomPath(points: List<com.moblin.android.platform.coregraphics.CGPoint>, tension: Double): Path = makeCatmullRomPath(points.map { PointF(it.x.toFloat(), it.y.toFloat()) }, tension.toFloat()); fun makeCatmullRomPath(points: List<PointF>, tension: Float): Path {
    val numberOfPoints = points.size
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until numberOfPoints) {
        val point0 = points[(i - 1 + numberOfPoints) % numberOfPoints]
        val point1 = points[i]
        val point2 = points[(i + 1) % numberOfPoints]
        val point3 = points[(i + 2) % numberOfPoints]
        val cpoint1x = point1.x + (point2.x - point0.x) * tension
        val cpoint1y = point1.y + (point2.y - point0.y) * tension
        val cpoint2x = point2.x - (point3.x - point1.x) * tension
        val cpoint2y = point2.y - (point3.y - point1.y) * tension
        path.cubicTo(cpoint1x, cpoint1y, cpoint2x, cpoint2y, point2.x, point2.y)
    }
    path.close()
    return path
}

private fun makeCiColor(color: RgbColor): FloatArray {
    return floatArrayOf(
        color.red.toFloat() / 255.0f,
        color.green.toFloat() / 255.0f,
        color.blue.toFloat() / 255.0f,
        1.0f,
    )
}

private fun makeCgColor(color: RgbColor): Int {
    return Color.rgb(color.red.toInt(), color.green.toInt(), color.blue.toInt())
}

private fun makeMtiColor(color: RgbColor): FloatArray {
    return floatArrayOf(
        color.red.toFloat() / 255.0f,
        color.green.toFloat() / 255.0f,
        color.blue.toFloat() / 255.0f,
        1.0f,
    )
}

private fun makeCheckerboardImage(extent: RectF, settings: MaskEffectSettings): Bitmap? {
    val width = extent.width().toInt()
    val height = extent.height().toInt()
    if (width <= 0 || height <= 0) {
        return null
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.style = Paint.Style.FILL
    paint.color = makeCgColor(settings.backgroundColor)
    canvas.drawRect(0.0f, 0.0f, width.toFloat(), height.toFloat(), paint)
    paint.color = makeCgColor(settings.backgroundColor2)
    val squareSide = minOf(width, height).toDouble() / checkerboardSquareCount.toDouble()
    val columns = ceil(width.toDouble() / squareSide).toInt() + 2
    val rows = ceil(height.toDouble() / squareSide).toInt() + 2
    for (row in 0 until rows) {
        for (column in 0 until columns) {
            if ((row + column) % 2 == 1) {
                val x = width / 2.0 + (column - columns / 2) * squareSide
                val y = height / 2.0 + (row - rows / 2) * squareSide
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + squareSide).toFloat(),
                    (y + squareSide).toFloat(),
                    paint,
                )
            }
        }
    }
    return bitmap
}

class MaskEffect : VideoEffect() {
    private var settings: MaskEffectSettings? = null
    private var cachedSettings: MaskEffectSettings? = null
    private var cachedExtent: RectF = RectF()
    private var cachedMaskImage: Bitmap? = null
    private var cachedBackgroundImage: Bitmap? = null
    private val filterMetalPetal: Any by lazy { TODO("OpenGL ES port") }
    private var cachedMetalPetalSettings: MaskEffectSettings? = null
    private var cachedMetalPetalExtent: RectF = RectF()
    private var cachedMetalPetalMask: Bitmap? = null
    private var cachedMetalPetalBackgroundImage: Bitmap? = null

    fun setSettings(settings: MaskEffectSettings) {
        processorPipelineQueue.launch {
            this@MaskEffect.settings = settings
            cachedSettings = null
            cachedMaskImage = null
            cachedBackgroundImage = null
            cachedMetalPetalSettings = null
            cachedMetalPetalMask = null
            cachedMetalPetalBackgroundImage = null
        }
    }

    private fun makeMaskImage(extent: RectF, settings: MaskEffectSettings): Bitmap? {
        return makeMaskCgImage(extent, settings)
    }

    private fun makeMaskCgImage(extent: RectF, settings: MaskEffectSettings): Bitmap? {
        if (settings.points.size < 3) {
            return null
        }
        val width = extent.width().toInt()
        val height = extent.height().toInt()
        if (width <= 0 || height <= 0) {
            return null
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        val backgroundGray = if (settings.inverted) 1.0 else 0.0
        val polygonGray = if (settings.inverted) 0.0 else 1.0
        val backgroundGrayValue = (backgroundGray * 255.0).toInt()
        val polygonGrayValue = (polygonGray * 255.0).toInt()
        paint.color = Color.rgb(backgroundGrayValue, backgroundGrayValue, backgroundGrayValue)
        canvas.drawRect(0.0f, 0.0f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.rgb(polygonGrayValue, polygonGrayValue, polygonGrayValue)
        val screenPoints = settings.points.map {
            PointF((it.x * width).toFloat(), (it.y * height).toFloat())
        }
        val path = makeCatmullRomPath(screenPoints, settings.tension.toFloat())
        canvas.drawPath(path, paint)
        return bitmap
    }

    private fun makeBackgroundImage(extent: RectF, settings: MaskEffectSettings): Bitmap {
        val width = extent.width().toInt()
        val height = extent.height().toInt()
        return when (settings.backgroundType) {
            SettingsMaskBackgroundType.transparent ->
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            SettingsMaskBackgroundType.solid ->
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(makeCgColor(settings.backgroundColor))
                }
            SettingsMaskBackgroundType.checkerboard ->
                makeCheckerboardImage(extent, settings)
                    ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }

    private fun makeMetalPetalBackgroundImage(
        extent: RectF,
        settings: MaskEffectSettings,
    ): Bitmap {
        val width = extent.width().toInt()
        val height = extent.height().toInt()
        return when (settings.backgroundType) {
            SettingsMaskBackgroundType.transparent ->
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            SettingsMaskBackgroundType.solid ->
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(makeCgColor(settings.backgroundColor))
                }
            SettingsMaskBackgroundType.checkerboard ->
                makeCheckerboardImage(extent, settings)
                    ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
    }

    override fun executeMetalPetal(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }

    override fun execute(image: Image, info: VideoEffectInfo): Image {
        TODO()
    }
}
