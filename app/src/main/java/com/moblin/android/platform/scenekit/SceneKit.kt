package com.moblin.android.platform.scenekit

import android.graphics.Bitmap
import android.util.Log
import com.moblin.android.platform.coregraphics.CGSize
import com.moblin.android.platform.metalpetal.MTLDevice
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.roundToInt

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

    fun notImplemented(member: String) {
        once("notImplemented:$member", "$member not implemented yet")
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
    var position: SCNVector3 = SCNVector3.zero
    var rotation: SCNVector4 = SCNVector4.zero
    var eulerAngles: SCNVector3 = SCNVector3.zero
    var orientation: SCNQuaternion = SCNVector4(0f, 0f, 0f, 1f)
    var scale: SCNVector3 = SCNVector3(1f, 1f, 1f)
    var camera: SCNCamera? = null
    var isHidden: Boolean = false
    var opacity: Double = 1.0

    private val children = mutableListOf<SCNNode>()

    var parent: SCNNode? = null
        private set

    val childNodes: List<SCNNode>
        get() = synchronized(children) { children.toList() }

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

    fun snapshot(atTime: Double, with: CGSize, antialiasingMode: SCNAntialiasingMode): Bitmap {
        SceneKitLog.notImplemented("SCNRenderer.snapshot")
        sceneTime = atTime
        val width = max(pixels(with.width), 1)
        val height = max(pixels(with.height), 1)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
}

private fun pixels(value: Double): Int {
    if (!value.isFinite() || value <= 0.0) {
        return 0
    }
    return value.coerceAtMost(8192.0).roundToInt()
}
