package com.moblin.android.videoeffects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.view.stream.DrawOnStreamLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun drawOnStreamCreatePath(points: List<Offset>): Path {
    val path = Path()
    points.firstOrNull()?.let { path.moveTo(it.x, it.y) }
    if (points.size > 2) {
        for (index in 1 until points.size) {
            val mid = calculateMidPoint(points[index - 1], points[index])
            path.quadraticBezierTo(points[index - 1].x, points[index - 1].y, mid.x, mid.y)
        }
    }
    points.lastOrNull()?.let { path.lineTo(it.x, it.y) }
    return path
}

private fun calculateMidPoint(point1: Offset, point2: Offset): Offset {
    return Offset((point1.x + point2.x) / 2, (point1.y + point2.y) / 2)
}

private fun transformPoint(
    point: Offset,
    scale: Double,
    offsetX: Double,
    offsetY: Double,
    mirror: Boolean,
    videoWidth: Double
): Offset {
    var x = point.x * scale - offsetX
    if (mirror) {
        x = videoWidth - x
    }
    return Offset(x.toFloat(), (point.y * scale - offsetY).toFloat())
}

class DrawOnStreamEffect : VideoEffect() {
    private val filter = PorterDuff.Mode.SRC_OVER
    private var overlay: EffectImageCgImage? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun updateOverlay(videoSize: Size, size: Size, lines: List<DrawOnStreamLine>, mirror: Boolean) {
        mainScope.launch {
            val drawRatio = size.width / size.height
            val videoRatio = videoSize.width / videoSize.height
            val offsetX: Double
            val offsetY: Double
            val scale: Double
            if (drawRatio > videoRatio) {
                offsetX = ((drawRatio / videoRatio * videoSize.width - videoSize.width) / 2).toDouble()
                offsetY = 0.0
                scale = (videoSize.height / size.height).toDouble()
            } else {
                offsetX = 0.0
                offsetY = ((videoRatio / drawRatio * videoSize.height - videoSize.height) / 2).toDouble()
                scale = (videoSize.width / size.width).toDouble()
            }
            val bitmap = Bitmap.createBitmap(
                videoSize.width.toInt(),
                videoSize.height.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            for (line in lines) {
                val width = line.width.toDouble() * scale
                paint.color = line.color.toArgb()
                paint.strokeWidth = width.toFloat()
                if (line.points.size > 1) {
                    val path = drawOnStreamCreatePath(line.points.map { point ->
                        transformPoint(point, scale, offsetX, offsetY, mirror, videoSize.width.toDouble())
                    })
                    canvas.drawPath(path.asAndroidPath(), paint)
                } else {
                    val point = transformPoint(
                        line.points[0],
                        scale,
                        offsetX,
                        offsetY,
                        mirror,
                        videoSize.width.toDouble()
                    )
                    val path = Path()
                    path.addOval(Rect(point, Size(1f, 1f)))
                    canvas.drawPath(path.asAndroidPath(), paint)
                }
            }
            val newOverlay = bitmap.toEffectImage()
            processorPipelineQueue.launch {
                this@DrawOnStreamEffect.overlay = newOverlay
            }
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage = TODO("OpenGL ES port")

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage = TODO("OpenGL ES port")
}
