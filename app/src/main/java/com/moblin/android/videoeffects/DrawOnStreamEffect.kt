package com.moblin.android.videoeffects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.media.haishinkit.media.video.VideoEffect
import com.moblin.android.media.haishinkit.media.video.VideoEffectInfo
import com.moblin.android.platform.coregraphics.CGPoint
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.coregraphics.toCGPoint
import com.moblin.android.platform.coreimage.CIFilter
import com.moblin.android.platform.coreimage.CIImage
import com.moblin.android.platform.metalpetal.MTIImage
import com.moblin.android.platform.swiftui.ImageRenderer
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.view.stream.DrawOnStreamLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun drawOnStreamCreatePath(points: List<CGPoint>): Path {
    val path = Path()
    val firstPoint = points.firstOrNull()
    if (firstPoint != null) {
        path.moveTo(firstPoint.x.toFloat(), firstPoint.y.toFloat())
    }
    if (points.size > 2) {
        for (index in 1 until points.size) {
            val mid = calculateMidPoint(points[index - 1], points[index])
            path.quadraticBezierTo(
                points[index - 1].x.toFloat(),
                points[index - 1].y.toFloat(),
                mid.x.toFloat(),
                mid.y.toFloat(),
            )
        }
    }
    val last = points.lastOrNull()
    if (last != null) {
        path.lineTo(last.x.toFloat(), last.y.toFloat())
    }
    return path
}

private fun calculateMidPoint(point1: CGPoint, point2: CGPoint): CGPoint {
    return CGPoint(x = (point1.x + point2.x) / 2, y = (point1.y + point2.y) / 2)
}

private fun transformPoint(
    point: CGPoint,
    scale: Double,
    offsetX: Double,
    offsetY: Double,
    mirror: Boolean,
    videoWidth: Double,
): CGPoint {
    var x = point.x * scale - offsetX
    if (mirror) {
        x = videoWidth - x
    }
    return CGPoint(x = x, y = point.y * scale - offsetY)
}

class DrawOnStreamEffect : VideoEffect() {
    private val filter = CIFilter.sourceOverCompositing()
    private var overlay: EffectImageCgImage? = null

    fun updateOverlay(videoSize: CGSize, size: CGSize, lines: List<DrawOnStreamLine>, mirror: Boolean) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            val drawRatio = size.width / size.height
            val videoRatio = videoSize.width / videoSize.height
            val offsetX: Double
            val offsetY: Double
            val scale: Double
            if (drawRatio > videoRatio) {
                offsetX = (drawRatio / videoRatio * videoSize.width - videoSize.width) / 2
                offsetY = 0.0
                scale = videoSize.height / size.height
            } else {
                offsetX = 0.0
                offsetY = (videoRatio / drawRatio * videoSize.height - videoSize.height) / 2
                scale = videoSize.width / size.width
            }
            val renderer = ImageRenderer(content = {
                Canvas(
                    modifier = Modifier
                        .size(videoSize.width.dp, videoSize.height.dp)
                        .background(Color.Transparent),
                ) {
                    for (line in lines) {
                        val width = line.width * scale
                        if (line.points.size > 1) {
                            drawPath(
                                path = drawOnStreamCreatePath(
                                    line.points.map { point ->
                                        transformPoint(
                                            point.toCGPoint(),
                                            scale,
                                            offsetX,
                                            offsetY,
                                            mirror,
                                            videoSize.width,
                                        )
                                    },
                                ),
                                color = line.color,
                                style = Stroke(width = width.toFloat(), miter = 10f),
                            )
                        } else {
                            val point = transformPoint(
                                line.points[0].toCGPoint(),
                                scale,
                                offsetX,
                                offsetY,
                                mirror,
                                videoSize.width,
                            )
                            val path = Path()
                            path.addOval(
                                Rect(
                                    left = point.x.toFloat(),
                                    top = point.y.toFloat(),
                                    right = (point.x + 1).toFloat(),
                                    bottom = (point.y + 1).toFloat(),
                                ),
                            )
                            drawPath(path = path, color = line.color, style = Stroke(width = width.toFloat(), miter = 10f))
                        }
                    }
                }
            })
            val uiImage = renderer.uiImage ?: return@launch
            val overlay = uiImage.cgImage.toEffectImage()
            processorPipelineQueue.launch {
                this@DrawOnStreamEffect.overlay = overlay
            }
        }
    }

    override fun execute(image: CIImage, info: VideoEffectInfo): CIImage {
        filter.inputImage = overlay?.getCiImage()
        filter.backgroundImage = image
        return filter.outputImage ?: image
    }

    override fun executeMetalPetal(image: MTIImage, info: VideoEffectInfo): MTIImage {
        val overlay = overlay?.getMetalPetalImage() ?: return image
        return overlay.positionComposited(
            position = CGPoint(x = image.extent.midX, y = image.extent.midY),
            backgroundImage = image,
        )
    }
}
