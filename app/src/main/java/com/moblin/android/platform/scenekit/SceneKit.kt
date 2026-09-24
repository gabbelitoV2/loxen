package com.moblin.android.platform.scenekit

import android.graphics.Bitmap
import android.util.Log
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.metalpetal.MTLDevice
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.acos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val TAG = "MoblinEffects"

internal object SceneKitLog {
    private val loggedMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun once(key: String, message: String) {
        if (loggedMessages.size > 1000) {
            loggedMessages.clear()
        }
        if (!loggedMessages.add(key)) {
            return
        }
        try {
            Log.i(TAG, message)
        } catch (_: Throwable) {
        }
    }
}

class SCNVector3(val x: Float, val y: Float, val z: Float) {
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(x: Number, y: Number, z: Number) : this(x.toFloat(), y.toFloat(), z.toFloat())

    fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z): SCNVector3 {
        return SCNVector3(x, y, z)
    }

    override fun equals(other: Any?): Boolean {
        return other is SCNVector3 && x == other.x && y == other.y && z == other.z
    }

    override fun hashCode(): Int {
        var result = normalizedHash(x)
        result = 31 * result + normalizedHash(y)
        return 31 * result + normalizedHash(z)
    }

    override fun toString(): String {
        return "SCNVector3(x: $x, y: $y, z: $z)"
    }

    companion object {
        val zero: SCNVector3 = SCNVector3(0f, 0f, 0f)
    }
}

class SCNVector4(val x: Float, val y: Float, val z: Float, val w: Float) {
    constructor(x: Double, y: Double, z: Double, w: Double) : this(
        x.toFloat(),
        y.toFloat(),
        z.toFloat(),
        w.toFloat()
    )

    constructor(x: Number, y: Number, z: Number, w: Number) : this(
        x.toFloat(),
        y.toFloat(),
        z.toFloat(),
        w.toFloat()
    )

    fun copy(x: Float = this.x, y: Float = this.y, z: Float = this.z, w: Float = this.w): SCNVector4 {
        return SCNVector4(x, y, z, w)
    }

    override fun equals(other: Any?): Boolean {
        return other is SCNVector4 && x == other.x && y == other.y && z == other.z && w == other.w
    }

    override fun hashCode(): Int {
        var result = normalizedHash(x)
        result = 31 * result + normalizedHash(y)
        result = 31 * result + normalizedHash(z)
        return 31 * result + normalizedHash(w)
    }

    override fun toString(): String {
        return "SCNVector4(x: $x, y: $y, z: $z, w: $w)"
    }

    companion object {
        val zero: SCNVector4 = SCNVector4(0f, 0f, 0f, 0f)
    }
}

typealias SCNQuaternion = SCNVector4

private fun normalizedHash(value: Float): Int {
    return if (value == 0f) 0 else value.hashCode()
}

val SCNVector3Zero: SCNVector3
    get() = SCNVector3.zero

val SCNVector4Zero: SCNVector4
    get() = SCNVector4.zero

fun SCNVector3Make(x: Float, y: Float, z: Float): SCNVector3 {
    return SCNVector3(x, y, z)
}

fun SCNVector4Make(x: Float, y: Float, z: Float, w: Float): SCNVector4 {
    return SCNVector4(x, y, z, w)
}

enum class SCNAntialiasingMode {
    none,
    multisampling2X,
    multisampling4X,
    multisampling8X,
    multisampling16X,
}

class SCNCamera {
    var name: String? = null
    var fieldOfView: Double = 60.0
    var zNear: Double = 1.0
    var zFar: Double = 100.0
    var usesOrthographicProjection: Boolean = false
    var orthographicScale: Double = 1.0
}

open class SCNNode {
    var name: String? = null
    var camera: SCNCamera? = null
    var isHidden: Boolean = false
    var opacity: Double = 1.0

    private var translation: Vec3 = Vec3.zero
    private var quaternion: Quat = Quat.identity
    private var scaling: Vec3 = Vec3.one

    internal var morphWeights: FloatArray? = null
    internal var renderingOrder: Int = 0

    var position: SCNVector3
        get() = translation.toSCNVector3()
        set(value) {
            translation = Vec3.of(value)
        }

    var orientation: SCNQuaternion
        get() = quaternion.toSCNVector4()
        set(value) {
            quaternion = Quat.of(value)
        }

    var rotation: SCNVector4
        get() {
            val q = quaternion
            val length = sqrt(q.lengthSquared())
            if (length == 0f) {
                return SCNVector4(0f, 0f, 0f, 0f)
            }
            val w = (q.w / length).coerceIn(-1f, 1f)
            val angle = 2f * acos(w)
            val s = sqrt(1f - w * w)
            return if (s < 1e-6f) {
                SCNVector4(1f, 0f, 0f, 0f)
            } else {
                SCNVector4(q.x / length / s, q.y / length / s, q.z / length / s, angle)
            }
        }
        set(value) {
            quaternion = Quat.fromAxisAngle(value.x, value.y, value.z, value.w)
        }

    var eulerAngles: SCNVector3
        get() = eulerFromQuat(quaternion).toSCNVector3()
        set(value) {
            quaternion = Quat.fromEuler(value.x, value.y, value.z)
        }

    var scale: SCNVector3
        get() = scaling.toSCNVector3()
        set(value) {
            scaling = Vec3.of(value)
        }

    internal var localTranslation: Vec3
        get() = translation
        set(value) {
            translation = value
        }

    internal var localRotation: Quat
        get() = quaternion
        set(value) {
            quaternion = value
        }

    internal val localScale: Vec3
        get() = scaling

    internal fun setTransformMatrix(matrix: FloatArray) {
        val (t, r, s) = Mat4.decompose(matrix)
        translation = t
        quaternion = r
        scaling = s
    }

    private val children = mutableListOf<SCNNode>()

    var parent: SCNNode? = null
        private set

    val childNodes: List<SCNNode>
        get() = synchronized(children) { children.toList() }

    internal val childCount: Int
        get() = synchronized(children) { children.size }

    internal val firstChild: SCNNode?
        get() = synchronized(children) { children.firstOrNull() }

    fun addChildNode(child: SCNNode) {
        if (child === this) {
            return
        }
        child.removeFromParentNode()
        synchronized(children) {
            children.add(child)
        }
        child.parent = this
    }

    fun insertChildNode(child: SCNNode, at: Int) {
        if (child === this) {
            return
        }
        child.removeFromParentNode()
        synchronized(children) {
            children.add(at.coerceIn(0, children.size), child)
        }
        child.parent = this
    }

    fun removeFromParentNode() {
        val currentParent = parent ?: return
        synchronized(currentParent.children) {
            currentParent.children.remove(this)
        }
        parent = null
    }

    fun childNode(withName: String, recursively: Boolean): SCNNode? {
        for (child in childNodes) {
            if (child.name == withName) {
                return child
            }
        }
        if (!recursively) {
            return null
        }
        for (child in childNodes) {
            val found = child.childNode(withName = withName, recursively = true)
            if (found != null) {
                return found
            }
        }
        return null
    }

    internal fun enumerateHierarchy(block: (SCNNode) -> Unit) {
        block(this)
        for (child in childNodes) {
            child.enumerateHierarchy(block)
        }
    }

    internal fun localMatrix(): FloatArray {
        return Mat4.compose(translation, quaternion, scaling)
    }

    internal fun worldMatrix(): FloatArray {
        val local = localMatrix()
        val currentParent = parent ?: return local
        return Mat4.multiply(currentParent.worldMatrix(), local)
    }

    internal var worldPosition: Vec3
        get() {
            val world = worldMatrix()
            return Vec3(world[12], world[13], world[14])
        }
        set(value) {
            val currentParent = parent
            translation = if (currentParent == null) {
                value
            } else {
                Mat4.transformPoint(Mat4.invert(currentParent.worldMatrix()), value)
            }
        }

    internal var worldOrientation: Quat
        get() {
            val currentParent = parent ?: return quaternion
            return currentParent.worldOrientation * quaternion
        }
        set(value) {
            val currentParent = parent
            quaternion = if (currentParent == null) value else currentParent.worldOrientation.inverse() * value
        }

    internal fun convertPositionToWorld(position: Vec3): Vec3 {
        return Mat4.transformPoint(worldMatrix(), position)
    }

    internal fun convertPositionFromWorld(position: Vec3): Vec3 {
        return Mat4.transformPoint(Mat4.invert(worldMatrix()), position)
    }

    internal fun lossyScale(): Vec3 {
        val currentParent = parent ?: return scaling
        return currentParent.lossyScale().times(scaling)
    }
}

open class SCNScene {
    val rootNode: SCNNode = SCNNode()
    var isPaused: Boolean = false
}

class SCNRenderer(val device: MTLDevice?, options: Map<String, Any>? = null) {
    var scene: SCNScene? = null
    var pointOfView: SCNNode? = null
    var autoenablesDefaultLighting: Boolean = false
    var sceneTime: Double = 0.0

    private val filament = FilamentSceneRenderer()

    fun snapshot(atTime: Double, with: CGSize, antialiasingMode: SCNAntialiasingMode): Bitmap {
        sceneTime = atTime
        val width = max(pixels(with.width), 1)
        val height = max(pixels(with.height), 1)
        val currentScene = scene
        val cameraNode = pointOfView ?: currentScene?.rootNode?.let { findCameraNode(it) }
        val bitmap = if (currentScene is VRMScene && cameraNode != null) {
            filament.render(currentScene, cameraNode, width, height)
        } else {
            null
        }
        return bitmap ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun findCameraNode(node: SCNNode): SCNNode? {
        if (node.camera != null) {
            return node
        }
        for (child in node.childNodes) {
            val found = findCameraNode(child)
            if (found != null) {
                return found
            }
        }
        return null
    }
}

private fun pixels(value: Double): Int {
    if (!value.isFinite() || value <= 0.0) {
        return 0
    }
    return value.coerceAtMost(8192.0).roundToInt()
}
