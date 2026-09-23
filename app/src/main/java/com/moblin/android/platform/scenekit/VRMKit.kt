package com.moblin.android.platform.scenekit

import java.io.File
import java.net.URI

class VRMError(message: String) : Exception(message)

enum class BlendShapePreset(val rawValue: String) {
    unknown("unknown"),
    neutral("neutral"),
    a("a"),
    i("i"),
    u("u"),
    e("e"),
    o("o"),
    blink("blink"),
    joy("joy"),
    angry("angry"),
    sorrow("sorrow"),
    `fun`("fun"),
    lookUp("lookup"),
    lookDown("lookdown"),
    lookLeft("lookleft"),
    lookRight("lookright"),
    blinkL("blink_l"),
    blinkR("blink_r"),
    ;

    companion object {
        operator fun invoke(rawValue: String): BlendShapePreset? {
            return entries.firstOrNull { it.rawValue == rawValue.lowercase() }
        }
    }
}

sealed class BlendShapeKey {
    data class preset(val preset: BlendShapePreset) : BlendShapeKey()

    data class custom(val name: String) : BlendShapeKey()
}

class Humanoid {
    enum class Bones {
        hips,
        leftUpperLeg,
        rightUpperLeg,
        leftLowerLeg,
        rightLowerLeg,
        leftFoot,
        rightFoot,
        spine,
        chest,
        neck,
        head,
        leftShoulder,
        rightShoulder,
        leftUpperArm,
        rightUpperArm,
        leftLowerArm,
        rightLowerArm,
        leftHand,
        rightHand,
        leftToes,
        rightToes,
        leftEye,
        rightEye,
        jaw,
        leftThumbProximal,
        leftThumbIntermediate,
        leftThumbDistal,
        leftIndexProximal,
        leftIndexIntermediate,
        leftIndexDistal,
        leftMiddleProximal,
        leftMiddleIntermediate,
        leftMiddleDistal,
        leftRingProximal,
        leftRingIntermediate,
        leftRingDistal,
        leftLittleProximal,
        leftLittleIntermediate,
        leftLittleDistal,
        rightThumbProximal,
        rightThumbIntermediate,
        rightThumbDistal,
        rightIndexProximal,
        rightIndexIntermediate,
        rightIndexDistal,
        rightMiddleProximal,
        rightMiddleIntermediate,
        rightMiddleDistal,
        rightRingProximal,
        rightRingIntermediate,
        rightRingDistal,
        rightLittleProximal,
        rightLittleIntermediate,
        rightLittleDistal,
        upperChest,
        ;

        val rawValue: String
            get() = name
    }

    private val bones = HashMap<Bones, SCNNode>()

    fun node(`for`: Bones): SCNNode? {
        return synchronized(bones) { bones[`for`] }
    }

    internal fun setUp(nodes: Map<Bones, SCNNode>) {
        synchronized(bones) {
            bones.clear()
            bones.putAll(nodes)
        }
    }
}

open class VRMNode internal constructor() : SCNNode() {
    val humanoid: Humanoid = Humanoid()

    private val blendShapeValues = HashMap<BlendShapeKey, Double>()

    fun setBlendShape(value: Double, `for`: BlendShapeKey) {
        synchronized(blendShapeValues) {
            blendShapeValues[`for`] = value
        }
    }

    fun setBlendShape(value: Float, `for`: BlendShapeKey) {
        setBlendShape(value = value.toDouble(), `for` = `for`)
    }

    fun blendShape(`for`: BlendShapeKey): Double {
        return synchronized(blendShapeValues) { blendShapeValues[`for`] ?: 0.0 }
    }

    open fun update(at: Double) {
        SceneKitLog.notImplemented("VRMNode.update")
    }
}

class VRMScene internal constructor(val vrmNode: VRMNode) : SCNScene() {
    init {
        rootNode.addChildNode(vrmNode)
    }
}

class VRMSceneLoader private constructor(private val source: File?, private val data: ByteArray?, val rootDirectory: File?) {
    constructor(withURL: File, rootDirectory: File? = null) : this(checkedFile(withURL), null, rootDirectory)

    constructor(withURL: String, rootDirectory: String? = null) : this(
        checkedFile(File(withURL)),
        null,
        rootDirectory?.let { File(it) }
    )

    constructor(withURL: URI, rootDirectory: URI? = null) : this(
        checkedFile(File(withURL)),
        null,
        rootDirectory?.let { File(it) }
    )

    constructor(withData: ByteArray, rootDirectory: File? = null) : this(null, withData, rootDirectory)

    fun loadScene(): VRMScene {
        SceneKitLog.notImplemented("VRMSceneLoader.loadScene")
        val name = source?.name ?: "${data?.size ?: 0} bytes"
        throw VRMError("Loading VRM models is not implemented yet ($name)")
    }
}

private fun checkedFile(file: File): File {
    if (!file.isFile || !file.canRead()) {
        throw VRMError("Cannot read VRM file ${file.path}")
    }
    return file
}
