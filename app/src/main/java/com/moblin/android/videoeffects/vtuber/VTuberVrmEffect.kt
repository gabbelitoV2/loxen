package com.moblin.android.videoeffects.vtuber

import android.util.Log
import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.scenekit.BlendShapeKey
import com.moblin.android.platform.scenekit.BlendShapePreset
import com.moblin.android.platform.scenekit.Humanoid
import com.moblin.android.platform.scenekit.SCNAntialiasingMode
import com.moblin.android.platform.scenekit.SCNCamera
import com.moblin.android.platform.scenekit.SCNNode
import com.moblin.android.platform.scenekit.SCNRenderer
import com.moblin.android.platform.scenekit.SCNVector3
import com.moblin.android.platform.scenekit.SCNVector4
import com.moblin.android.platform.scenekit.VRMScene
import com.moblin.android.platform.scenekit.VRMSceneLoader
import com.moblin.android.platform.uikit.cgImage
import com.moblin.android.videoeffects.EffectImage
import com.moblin.android.videoeffects.toEffectImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI

class VTuberVrmEffect(
    vrm: String,
    cameraFieldOfView: Double,
    cameraPositionY: Double,
) : VTuberEffect() {
    private var scene: VRMScene? = null
    private var armsAngle: Double = PI / 2.5
    private val renderer = SCNRenderer(device = null)
    private var cameraNode: SCNNode? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            val scene = try {
                VRMSceneLoader(withURL = vrm).loadScene()
            } catch (error: Throwable) {
                Log.i("VTuberVrmEffect", "v-tuber: Failed to load VRM file with error: $error")
                return@launch
            }
            processorPipelineQueue.launch {
                val camera = SCNCamera()
                camera.fieldOfView = cameraFieldOfView
                val cameraNode = SCNNode()
                cameraNode.camera = camera
                cameraNode.position = SCNVector3(0, cameraPositionY, -1.8)
                cameraNode.rotation = SCNVector4(0, 1, 0, PI.toFloat())
                scene.rootNode.addChildNode(cameraNode)
                renderer.scene = scene
                val node = scene.vrmNode
                node.humanoid.node(`for` = Humanoid.Bones.leftUpperArm)?.eulerAngles =
                    SCNVector3(0, 0, 40 * PI / 180)
                node.humanoid.node(`for` = Humanoid.Bones.rightUpperArm)?.eulerAngles =
                    SCNVector3(0, 0, -40 * PI / 180)
                this@VTuberVrmEffect.scene = scene
                this@VTuberVrmEffect.cameraNode = cameraNode
            }
        }
    }

    override fun setModelSettings(cameraFieldOfView: Double, cameraPositionY: Double, armsAngle: Double) {
        cameraNode?.camera?.fieldOfView = cameraFieldOfView
        cameraNode?.position = SCNVector3(0, cameraPositionY, -1.8)
        this.armsAngle = Math.toRadians(armsAngle)
    }

    override fun isModelLoaded(): Boolean {
        return scene != null
    }

    override fun updateModel(face: VTuberFace, time: Double, timeDelta: Double) {
        val node = scene?.vrmNode ?: return
        node.setBlendShape(value = face.mouthOpen, `for` = BlendShapeKey.preset(BlendShapePreset.a))
        node.setBlendShape(value = 1 - face.leftEyeOpen, `for` = BlendShapeKey.preset(BlendShapePreset.blink))
        val neckYAngle = face.sideAngle * 0.8
        val neckZAngle = face.rotationAngle * 0.8
        node.humanoid.node(`for` = Humanoid.Bones.neck)?.eulerAngles = SCNVector3(0, -neckYAngle, -neckZAngle)
        node.humanoid.node(`for` = Humanoid.Bones.spine)?.eulerAngles =
            SCNVector3(0, -neckYAngle / 3, -neckZAngle / 3)
        var angle = Math.IEEEremainder(time, PI * 2)
        if (angle < 0) {
            angle *= -1
        }
        angle -= PI / 2
        angle *= 0.5
        val armAngle = (angle * 0.1) + armsAngle
        node.humanoid.node(`for` = Humanoid.Bones.leftUpperArm)?.eulerAngles = SCNVector3(0, 0, armAngle)
        node.humanoid.node(`for` = Humanoid.Bones.rightUpperArm)?.eulerAngles = SCNVector3(0, 0, -armAngle)
    }

    override fun renderModel(time: Double, size: CGSize): EffectImage? {
        val node = scene?.vrmNode ?: return null
        node.update(at = time)
        val factor = (maxOf(size.width, size.height) / 1920)
        val vTuberImage = renderer.snapshot(
            atTime = time,
            with = CGSize(width = 600 * factor, height = 600 * factor),
            antialiasingMode = SCNAntialiasingMode.none,
        )
        return vTuberImage.cgImage.toEffectImage()
    }
}
