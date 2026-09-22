package com.moblin.android.videoeffects.vtuber

import android.util.Log
import android.util.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.videoeffects.EffectImage
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "VTuberLive2DEffect"

private data class Live2DLoaded(
    val model: Any,
    val renderer: Live2DRenderer,
    val pool: Any,
)

class VTuberLive2DEffect(directory: File) : VTuberEffect() {
    private var loaded: Live2DLoaded? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            val result = load(directory) ?: return@launch
            processorPipelineQueue.launch {
                loaded = result
            }
        }
    }

    private companion object {
        fun load(directory: File): Live2DLoaded? {
            val model3File = findModel3(directory)
            if (model3File == null) {
                Log.i(TAG, "v-tuber: No model3.json found")
                return null
            }
            TODO("no Android counterpart for Ayagami")
        }

        fun findModel3(directory: File): File? = directory.walkTopDown().firstOrNull {
            it.name.endsWith(".model3.json") && !it.path.contains("__MACOSX")
        }
    }

    override fun isModelLoaded(): Boolean {
        return loaded != null
    }

    override fun updateModel(face: VTuberFace, time: Double, timeDelta: Double) {
        val model = loaded?.model ?: return
        val angleX = face.sideAngle * 30
        val parameters = mapOf(
            "ParamAngleX" to angleX.toFloat(),
            "ParamAngleZ" to (-(face.rotationAngle * 180.0 / PI)).toFloat(),
            "ParamBodyAngleX" to (angleX / 3).toFloat(),
            "ParamMouthOpenY" to face.mouthOpen.toFloat(),
            "ParamEyeLOpen" to face.leftEyeOpen.toFloat(),
            "ParamEyeROpen" to face.rightEyeOpen.toFloat(),
            "ParamBreath" to (0.5 - cos(time / 2 * PI) / 2).toFloat(),
        )
        TODO("no Android counterpart for Ayagami")
    }

    override fun renderModel(time: Double, size: Size): EffectImage? {
        val loaded = loaded ?: return null
        TODO("no Android counterpart for Ayagami")
    }
}
