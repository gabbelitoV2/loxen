package com.moblin.android.platform.live2d

import android.util.Log
import com.moblin.android.platform.simd.SIMD2
import com.moblin.android.platform.simd.SIMD3
import java.io.File
import java.net.URI
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val TAG = "MoblinEffects"

internal object Live2DLog {
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

class AyagamiError(message: String) : Exception(message)

enum class AyagamiBlendMode(val rawValue: Int) {
    normal(0),
    add(1),
    multiply(2),
    ;

    companion object {
        operator fun invoke(rawValue: Int): AyagamiBlendMode? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

class AyagamiCanvas internal constructor(val scale: Float, val center: SIMD2, val dimensions: SIMD2)

class AyagamiArtMeshInfo internal constructor(
    val textureIndex: Int,
    val vertexCount: Int,
    val texcoordOffset: Int,
    val indexRange: IntRange,
    val clips: List<Int>,
    val blendMode: AyagamiBlendMode,
    val culling: Boolean,
    val invertMask: Boolean,
)

class AyagamiArtMeshState internal constructor(
    val visible: Boolean,
    val opacity: Float,
    val multiplyColor: SIMD3,
    val screenColor: SIMD3,
    val vertices: FloatArray,
)

internal class AyagamiFileReferences(val moc: File, val textures: List<File>, val physics: File?)

internal class AyagamiModelData(
    val canvas: AyagamiCanvas,
    val texturePaths: List<File>,
    val indices: ShortArray,
    val texcoords: FloatArray,
    val artMeshes: List<AyagamiArtMeshInfo>,
    val parameterIds: Set<String>,
)

class AyagamiModel(model3Url: File) {
    constructor(model3Url: String) : this(File(model3Url))
    constructor(model3Url: URI) : this(File(model3Url))

    private val data: AyagamiModelData = loadAyagamiModel(model3Url)
    private val parameters = HashMap<String, Float>()

    val canvas: AyagamiCanvas
        get() = data.canvas

    val texturePaths: List<File>
        get() = data.texturePaths

    val indices: ShortArray
        get() = data.indices

    val texcoords: FloatArray
        get() = data.texcoords

    val artMeshCount: Int
        get() = data.artMeshes.size

    fun artMeshInfo(uid: Int): AyagamiArtMeshInfo? {
        return data.artMeshes.getOrNull(uid)
    }

    fun artMeshState(uid: Int): AyagamiArtMeshState? {
        Live2DLog.notImplemented("AyagamiModel.artMeshState")
        return null
    }

    fun drawOrder(): List<Int> {
        Live2DLog.notImplemented("AyagamiModel.drawOrder")
        return emptyList()
    }

    fun hasParameter(id: String): Boolean {
        return data.parameterIds.contains(id)
    }

    fun setParameter(id: String, value: Float) {
        synchronized(parameters) {
            parameters[id] = value
        }
    }

    fun update(deltaTime: Float) {
        Live2DLog.notImplemented("AyagamiModel.update")
    }
}

internal fun readModel3FileReferences(model3Url: File): AyagamiFileReferences {
    val text = try {
        model3Url.readText()
    } catch (error: Exception) {
        throw AyagamiError("Failed to open model3.json: ${error.message}")
    }
    val root = try {
        Json.parseToJsonElement(text) as? JsonObject
    } catch (error: Exception) {
        throw AyagamiError("Failed to parse model3.json: ${error.message}")
    } ?: throw AyagamiError("Failed to parse model3.json: not an object")
    val references = root["FileReferences"] as? JsonObject
        ?: throw AyagamiError("Failed to parse model3.json: no FileReferences")
    val base = model3Url.absoluteFile.parentFile ?: File(".")
    val moc = (references["Moc"] as? JsonPrimitive)?.contentOrNull
        ?: throw AyagamiError("Failed to parse model3.json: no Moc")
    val textures = (references["Textures"] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        ?.map { File(base, it) }
        ?: emptyList()
    val physics = (references["Physics"] as? JsonPrimitive)?.contentOrNull?.let { File(base, it) }
    return AyagamiFileReferences(File(base, moc), textures, physics)
}

private fun loadAyagamiModel(model3Url: File): AyagamiModelData {
    val references = readModel3FileReferences(model3Url)
    if (!references.moc.isFile) {
        throw AyagamiError("Failed to open ${references.moc.path}")
    }
    Live2DLog.notImplemented("AyagamiModel.init")
    throw AyagamiError("Parsing ${references.moc.name} is not implemented yet")
}
