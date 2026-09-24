package com.moblin.android.videoeffects.vtuber

import android.util.Log
import androidx.compose.ui.geometry.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.live2d.AyagamiModel
import com.moblin.android.platform.live2d.Live2DRenderer
import com.moblin.android.platform.video.CVPixelBufferPool
import com.moblin.android.platform.video.CVPixelBufferPoolCreate
import com.moblin.android.platform.video.CVPixelBufferPoolCreatePixelBuffer
import com.moblin.android.platform.video.kCVPixelBufferHeightKey
import com.moblin.android.platform.video.kCVPixelBufferIOSurfacePropertiesKey
import com.moblin.android.platform.video.kCVPixelBufferMetalCompatibilityKey
import com.moblin.android.platform.video.kCVPixelBufferPixelFormatTypeKey
import com.moblin.android.platform.video.kCVPixelBufferWidthKey
import com.moblin.android.platform.video.kCVPixelFormatType_32BGRA
import com.moblin.android.videoeffects.EffectImage
import com.moblin.android.videoeffects.EffectImagePixelBuffer
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class Live2DLoaded(
    val model: AyagamiModel,
    val renderer: Live2DRenderer,
    val pool: CVPixelBufferPool,
)

class VTuberLive2DEffect(directory: File) : VTuberEffect() {
    private var loaded: Live2DLoaded? = null
    private var renderedPixelBuffer: com.moblin.android.platform.video.CVPixelBuffer? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            val newLoaded = load(directory) ?: return@launch
            processorPipelineQueue.launch {
                loaded = newLoaded
            }
        }
    }

    companion object {
        private fun load(directory: File): Live2DLoaded? {
            val model3Url = findModel3(directory)
            if (model3Url == null) {
                Log.i("VTuberLive2DEffect", "v-tuber: No model3.json found")
                return null
            }
            val model = try {
                AyagamiModel(model3Url = model3Url)
            } catch (error: Throwable) {
                Log.i("VTuberLive2DEffect", "v-tuber: Failed to load Live2D model with error: $error")
                return null
            }
            val renderer = Live2DRenderer(model = model) ?: return null
            val dimensions = model.canvas.dimensions
            if (dimensions.x <= 0F || dimensions.y <= 0F) {
                Log.i("VTuberLive2DEffect", "v-tuber: Bad Live2D canvas dimensions $dimensions")
                return null
            }
            val height = 800
            val width = 2 * (height.toDouble() * (dimensions.x / dimensions.y).toDouble() / 2.0).toInt()
            val attributes: Map<String, Any> = mapOf(
                kCVPixelBufferPixelFormatTypeKey to kCVPixelFormatType_32BGRA,
                kCVPixelBufferIOSurfacePropertiesKey to emptyMap<String, Any>(),
                kCVPixelBufferMetalCompatibilityKey to true,
                kCVPixelBufferWidthKey to width,
                kCVPixelBufferHeightKey to height,
            )
            val pool = CVPixelBufferPoolCreate(attributes) ?: return null
            return Live2DLoaded(model = model, renderer = renderer, pool = pool)
        }

        private fun findModel3(directory: File): File? {
            for (url in directory.walk()) {
                if (url.name.endsWith(".model3.json") && !url.path.contains("__MACOSX")) {
                    return url
                }
            }
            return null
        }
    }

    override fun isModelLoaded(): Boolean {
        return loaded != null
    }

    override fun updateModel(face: VTuberFace, time: Double, timeDelta: Double) {
        val model = loaded?.model ?: return
        val angleX = face.sideAngle * 30
        model.setParameter("ParamAngleX", angleX.toFloat())
        model.setParameter("ParamAngleZ", (-Math.toDegrees(face.rotationAngle)).toFloat())
        model.setParameter("ParamBodyAngleX", (angleX / 3).toFloat())
        model.setParameter("ParamMouthOpenY", face.mouthOpen.toFloat())
        model.setParameter("ParamEyeLOpen", face.leftEyeOpen.toFloat())
        model.setParameter("ParamEyeROpen", face.rightEyeOpen.toFloat())
        model.setParameter("ParamBreath", (0.5 - cos(time / 2 * PI) / 2).toFloat())
        model.update(deltaTime = timeDelta.toFloat())
    }

    override fun renderModel(time: Double, size: CGSize): EffectImage? {
        val currentLoaded = loaded ?: return null
        val pixelBuffer = CVPixelBufferPoolCreatePixelBuffer(currentLoaded.pool) ?: return null
        currentLoaded.renderer.render(model = currentLoaded.model, into = pixelBuffer)
        renderedPixelBuffer = com.moblin.android.platform.video.swapLease(renderedPixelBuffer, pixelBuffer) { it }
        return EffectImagePixelBuffer(pixelBuffer = pixelBuffer)
    }
}
