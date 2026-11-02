package com.moblin.android.videoeffects.vtuber

import com.moblin.android.media.haishinkit.media.processorPipelineQueue
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.log.Log
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
import com.moblin.android.videoeffects.toPixels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI

private data class VTuberVrmPose(
    var face: VTuberFace = VTuberFace(),
    var time: Double = 0.0,
    var timeDelta: Double = 0.0,
    var armsAngle: Double = PI / 2.5,
)

private class VTuberVrmRenderer private constructor(private val scene: VRMScene) {
    private val renderer = SCNRenderer(device = null)
    private val camera = SCNNode()

    init {
        camera.camera = SCNCamera()
        camera.position = SCNVector3(0, 0, -1.8)
        camera.rotation = SCNVector4(0, 1, 0, PI.toFloat())
        scene.rootNode.addChildNode(camera)
        renderer.scene = scene
    }

    fun setCamera(fieldOfView: Double, positionY: Double) {
        camera.camera?.fieldOfView = fieldOfView
        camera.position = SCNVector3(0, positionY, -1.8)
    }

    fun render(pose: VTuberVrmPose, size: Int, onComplete: (EffectImage?) -> Unit) {
        update(pose = pose)
        scene.vrmNode.update(at = pose.time)
        val vTuberImage = renderer.snapshot(
            atTime = pose.time,
            with = CGSize(width = size, height = size),
            antialiasingMode = SCNAntialiasingMode.none,
        )
        onComplete(vTuberImage.cgImage.toEffectImage())
    }

    private fun update(pose: VTuberVrmPose) {
        val face = pose.face
        val node = scene.vrmNode
        node.setBlendShape(value = face.mouthOpen, `for` = BlendShapeKey.preset(BlendShapePreset.a))
        node.setBlendShape(value = 1 - face.leftEyeOpen, `for` = BlendShapeKey.preset(BlendShapePreset.blink))
        val neckYAngle = face.sideAngle * 0.8
        val neckZAngle = face.rotationAngle * 0.8
        setBone(Humanoid.Bones.neck, yAngle = -neckYAngle, zAngle = -neckZAngle)
        setBone(Humanoid.Bones.spine, yAngle = -neckYAngle / 3, zAngle = -neckZAngle / 3)
        var angle = Math.IEEEremainder(pose.time, PI * 2)
        if (angle < 0) {
            angle *= -1
        }
        angle -= PI / 2
        angle *= 0.5
        val armAngle = (angle * 0.1) + pose.armsAngle
        setBone(Humanoid.Bones.leftUpperArm, yAngle = 0.0, zAngle = armAngle)
        setBone(Humanoid.Bones.rightUpperArm, yAngle = 0.0, zAngle = -armAngle)
    }

    private fun setBone(bone: Humanoid.Bones, yAngle: Double, zAngle: Double) {
        val node = scene.vrmNode.humanoid.node(`for` = bone) ?: return
        node.eulerAngles = SCNVector3(0, yAngle, zAngle)
    }

    companion object {
        suspend fun load(vrm: String): VTuberVrmRenderer? {
            return try {
                VTuberVrmRenderer(scene = VRMSceneLoader(withURL = vrm).loadScene())
            } catch (error: Throwable) {
                Log.i("VTuberVrmEffect", "v-tuber: Failed to load VRM file with error: $error")
                return null
            }
        }
    }
}

class VTuberVrmEffect(
    vrm: String,
    private var cameraFieldOfView: Double,
    private var cameraPositionY: Double,
) : VTuberEffect() {
    private var renderer: VTuberVrmRenderer? = null
    private var pose = VTuberVrmPose()
    private var rendering = false
    private var renderedImage: EffectImage? = null

    init {
        CoroutineScope(Dispatchers.Default).launch {
            val renderer = VTuberVrmRenderer.load(vrm = vrm) ?: return@launch
            processorPipelineQueue.launch {
                this@VTuberVrmEffect.renderer = renderer
                updateCamera()
            }
        }
    }

    override fun setModelSettings(cameraFieldOfView: Double, cameraPositionY: Double, armsAngle: Double) {
        this.cameraFieldOfView = cameraFieldOfView
        this.cameraPositionY = cameraPositionY
        pose.armsAngle = Math.toRadians(armsAngle)
        updateCamera()
    }

    private fun updateCamera() {
        val renderer = renderer ?: return
        val cameraFieldOfView = cameraFieldOfView
        val cameraPositionY = cameraPositionY
        renderer.setCamera(fieldOfView = cameraFieldOfView, positionY = cameraPositionY)
    }

    override fun isModelLoaded(): Boolean {
        return renderer != null
    }

    override fun updateModel(face: VTuberFace, time: Double, timeDelta: Double) {
        pose.face = face
        pose.time = time
        pose.timeDelta += timeDelta
    }

    override fun renderModel(time: Double, size: CGSize): EffectImage? {
        val renderer = renderer ?: return null
        val sceneWidget = sceneWidget ?: return null
        if (!rendering) {
            rendering = true
            val pose = pose.copy(face = pose.face.copy())
            this.pose.timeDelta = 0.0
            val layoutSize = sceneWidget.layout.size
            val side = minOf(
                toPixels(layoutSize, size.width),
                toPixels(layoutSize, size.height),
            )
            val size = 2 * (side.coerceIn(64.0, 2048.0) / 2).toInt()
            renderer.render(pose = pose, size = size) { image ->
                processorPipelineQueue.launch {
                    if (image != null) {
                        this@VTuberVrmEffect.renderedImage = image
                    }
                    this@VTuberVrmEffect.rendering = false
                }
            }
        }
        return renderedImage
    }
}
