package com.moblin.android.videoeffects.vtuber

import android.util.Log
import android.util.Size
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.videoeffects.EffectImage
import kotlin.math.PI
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VTuberVrmEffect(
    vrm: String,
    cameraFieldOfView: Double,
    cameraPositionY: Double,
) : VTuberEffect() {
    private var scene: Any? = null
    private var armsAngle: Double = PI / 2.5
    private var renderer: Any? = null
    private var cameraNode: Any? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val loadedScene: Any? = runCatching<Any?> {
                TODO(
                    "VRMSceneKit has no Android counterpart: load the VRM scene from the file " +
                        "$vrm and return the scene"
                )
            }.getOrElse { error ->
                Log.i("VTuberVrmEffect", "v-tuber: Failed to load VRM file with error: $error")
                return@launch
            }
            processorPipelineQueue.launch {
                scene = loadedScene
                TODO(
                    "SceneKit has no Android counterpart: create the camera node with " +
                        "fieldOfView $cameraFieldOfView at y $cameraPositionY, add it to the VRM " +
                        "scene, cache the humanoid nodes and assign scene and cameraNode"
                )
            }
        }
    }

    override fun setModelSettings(
        cameraFieldOfView: Double,
        cameraPositionY: Double,
        armsAngle: Double,
    ) {
        this.armsAngle = armsAngle * PI / 180.0
        TODO(
            "SceneKit has no Android counterpart: set fieldOfView $cameraFieldOfView and " +
                "position y $cameraPositionY on the VRM camera node"
        )
    }

    override fun isModelLoaded(): Boolean {
        return scene != null
    }

    override fun updateModel(face: VTuberFace, time: Double, timeDelta: Double) {
        if (scene == null) {
            return
        }
        var angle = time % (PI * 2)
        if (angle < 0) {
            angle *= -1
        }
        angle -= PI / 2
        angle *= 0.5
        val armAngle = (angle * 0.1) + armsAngle
        val mouthOpen = face.mouthOpen
        val blink = 1.0 - face.leftEyeOpen.toDouble()
        val neckYAngle = face.sideAngle.toDouble() * 0.8
        val neckZAngle = face.rotationAngle.toDouble() * 0.8
        TODO(
            "SceneKit has no Android counterpart: apply the blend shapes ($mouthOpen, $blink) " +
                "and the humanoid euler angles (neck $neckYAngle/$neckZAngle, upper arms " +
                "${-armAngle}/$armAngle) to the VRM nodes"
        )
    }

    override fun renderModel(time: Double, size: Size): EffectImage? {
        if (scene == null) {
            return null
        }
        val factor = max(size.width, size.height) / 1920.0
        val width = (600 * factor).toInt()
        val height = (600 * factor).toInt()
        TODO(
            "SceneKit has no Android counterpart: render the VRM scene at $time into a " +
                "${width}x$height image and convert it to an EffectImage"
        )
    }
}
