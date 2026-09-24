package com.moblin.android.platform.live2d

import android.util.Log
import com.moblin.android.platform.simd.SIMD2
import com.moblin.android.platform.simd.SIMD3
import java.io.File
import java.math.BigInteger
import java.net.URI
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
}

class AyagamiError(message: String) : Exception(message)

enum class AyagamiBlendMode {
    normal,
    add,
    multiply,
}

class AyagamiCanvas internal constructor(val scale: Float, val center: SIMD2, val dimensions: SIMD2)

class AyagamiArtMeshInfo internal constructor(
    val textureIndex: Int,
    val vertexCount: Int,
    val texcoordOffset: Int,
    val indexRange: IntRange,
    val blendMode: AyagamiBlendMode,
    val culling: Boolean,
    val invertMask: Boolean,
    val clips: List<Int>,
)

class AyagamiArtMeshState internal constructor(
    val visible: Boolean,
    val opacity: Float,
    val multiplyColor: SIMD3,
    val screenColor: SIMD3,
    val vertices: FloatArray,
)

internal class Model3References(val moc: String, val textures: List<String>, val physics: String?)

class AyagamiModel(model3Url: File) {
    constructor(model3Url: String) : this(File(model3Url))
    constructor(model3Url: URI) : this(File(model3Url))

    private val data: Live2DModelData
    private val driver: Live2DDriver
    private val physics: Live2DPhysicsEngine?
    private var needsSettle: Boolean
    private val userPose: Live2DPose
    private val physicsPose: Live2DPose
    private var drawOrderList: IntArray = IntArray(0)

    val canvas: AyagamiCanvas
    val texturePaths: List<File>
    val artMeshCount: Int

    init {
        val modelBytes = readModelFile(model3Url, "Failed to open model3.json")
        val references = try {
            parseModel3(modelBytes)
        } catch (error: Exception) {
            throw AyagamiError("Failed to parse model3.json: ${error.message}")
        }
        val base = model3Url.absoluteFile.parentFile ?: File(".")
        val mocFile = File(base, references.moc)
        val mocData = readModelFile(mocFile, "Failed to open ${mocFile.path}")
        val file = try {
            parseMoc3(mocData)
        } catch (error: Moc3Exception) {
            throw AyagamiError("Failed to parse moc3: ${error.message}")
        } catch (error: RuntimeException) {
            throw AyagamiError("Failed to parse moc3: $error")
        }
        texturePaths = references.textures.map { File(base, it) }
        physics = references.physics?.let { name ->
            val physicsFile = File(base, name)
            val physicsBytes = readModelFile(physicsFile, "Failed to open ${physicsFile.path}")
            val config = try {
                parsePhysics3(physicsBytes)
            } catch (error: Exception) {
                throw AyagamiError("Failed to parse physics3.json: ${error.message}")
            }
            Live2DPhysicsEngine(config)
        }
        data = try {
            Live2DModelData(file)
        } catch (error: RuntimeException) {
            throw AyagamiError("Failed to parse moc3: $error")
        }
        artMeshCount = data.artMeshCount
        canvas = AyagamiCanvas(
            scale = data.canvas.scale,
            center = SIMD2(data.canvas.centerX, data.canvas.centerY),
            dimensions = SIMD2(data.canvas.width, data.canvas.height),
        )
        userPose = Live2DPose(Live2DPoseMap.fromModel(data))
        physicsPose = userPose.clone()
        driver = Live2DDriver(data)
        needsSettle = physics != null
        try {
            updateInternal(0f)
        } catch (error: Exception) {
            throw AyagamiError("Failed to drive model: ${error.message}")
        }
    }

    val indices: ShortArray
        get() = data.vertexIndices

    val texcoords: FloatArray
        get() = data.texCoords

    fun artMeshInfo(uid: Int): AyagamiArtMeshInfo? {
        if (uid < 0 || uid >= data.artMeshCount) {
            return null
        }
        val indexRange = data.artMeshIndices[uid]
        val blendMode = when (data.simpleBlendMode(uid)) {
            1 -> AyagamiBlendMode.add
            2 -> AyagamiBlendMode.multiply
            else -> AyagamiBlendMode.normal
        }
        return AyagamiArtMeshInfo(
            textureIndex = data.artMeshTexture[uid],
            vertexCount = data.artMeshVertexCount[uid],
            texcoordOffset = data.texcoordOffset(uid),
            indexRange = indexRange.start until indexRange.start + indexRange.count,
            blendMode = blendMode,
            culling = data.culling(uid),
            invertMask = data.invertMask(uid),
            clips = data.artMeshClips[uid].toList(),
        )
    }

    fun hasParameter(id: String): Boolean {
        return userPose.hasParam(id)
    }

    fun setParameter(id: String, value: Float) {
        userPose.setParam(id, value)
    }

    fun update(deltaTime: Float) {
        try {
            updateInternal(deltaTime.coerceIn(0f, 0.1f))
        } catch (error: Exception) {
            Live2DLog.once("update:${error.message}", "v-tuber: Live2D update failed: $error")
        }
    }

    fun drawOrder(): List<Int> {
        return drawOrderList.toList()
    }

    internal fun drawOrderArray(): IntArray {
        return drawOrderList
    }

    fun artMeshState(uid: Int): AyagamiArtMeshState? {
        val state = driver.artMeshState(uid) ?: return null
        val visual = state.visual
        return AyagamiArtMeshState(
            visible = visual.visible,
            opacity = visual.opacity,
            multiplyColor = SIMD3(visual.multiplyR, visual.multiplyG, visual.multiplyB),
            screenColor = SIMD3(visual.screenR, visual.screenG, visual.screenB),
            vertices = state.vertices,
        )
    }

    private fun updateInternal(deltaTime: Float) {
        val engine = physics
        if (engine != null) {
            physicsPose.update(userPose)
            if (needsSettle) {
                engine.settle(physicsPose)
                needsSettle = false
            }
            engine.update(physicsPose, deltaTime)
            val pose = physicsPose.clone()
            pose.update(userPose)
            driver.setPose(pose)
        } else {
            driver.setPose(userPose)
        }
        driver.drive()
        val order = ArrayList<Int>()
        collectDrawOrder(null, order)
        drawOrderList = order.toIntArray()
    }

    private fun collectDrawOrder(part: Int?, order: MutableList<Int>) {
        val nodes = driver.drawNodes(part) ?: return
        for (node in nodes) {
            when (node) {
                is DrawNode.ArtMesh -> order.add(node.uid)
                is DrawNode.OffscreenPart -> collectDrawOrder(node.uid, order)
            }
        }
    }
}

private fun readModelFile(file: File, message: String): ByteArray {
    return try {
        file.readBytes()
    } catch (error: Exception) {
        throw AyagamiError("$message: ${error.message}")
    }
}

private class MetaException(message: String) : Exception(message)

private fun JsonElement.asObject(name: String): JsonObject {
    return this as? JsonObject ?: throw MetaException("invalid type for $name, expected a map")
}

private fun JsonObject.required(key: String): JsonElement {
    return this[key] ?: throw MetaException("missing field `$key`")
}

private fun JsonElement.asString(name: String): String {
    val primitive = this as? JsonPrimitive
    if (primitive == null || primitive is JsonNull || !primitive.isString) {
        throw MetaException("invalid type for $name, expected a string")
    }
    return primitive.content
}

private fun JsonElement.asOptionalString(name: String): String? {
    if (this is JsonNull) {
        return null
    }
    return asString(name)
}

private fun JsonElement.asFloat(name: String): Float {
    val primitive = this as? JsonPrimitive
    if (primitive == null || primitive is JsonNull || primitive.isString) {
        throw MetaException("invalid type for $name, expected f32")
    }
    val content = primitive.content
    if (content == "true" || content == "false") {
        throw MetaException("invalid type for $name, expected f32")
    }
    return content.toDoubleOrNull()?.toFloat() ?: throw MetaException("invalid type for $name, expected f32")
}

private val integerPattern = Regex("-?[0-9]+")

private fun JsonElement.asU32(name: String): Long {
    val primitive = this as? JsonPrimitive
    if (primitive == null || primitive is JsonNull || primitive.isString) {
        throw MetaException("invalid type for $name, expected u32")
    }
    val content = primitive.content
    if (!integerPattern.matches(content)) {
        throw MetaException("invalid type for $name, expected u32")
    }
    val value = BigInteger(content)
    if (value.signum() < 0 || value > BigInteger.valueOf(0xffffffffL)) {
        throw MetaException("invalid value for $name, expected u32")
    }
    return value.toLong()
}

private fun JsonElement.asBool(name: String): Boolean {
    val primitive = this as? JsonPrimitive
    if (primitive == null || primitive is JsonNull || primitive.isString) {
        throw MetaException("invalid type for $name, expected a boolean")
    }
    return when (primitive.content) {
        "true" -> true
        "false" -> false
        else -> throw MetaException("invalid type for $name, expected a boolean")
    }
}

private fun JsonElement.asArray(name: String): JsonArray {
    return this as? JsonArray ?: throw MetaException("invalid type for $name, expected a sequence")
}

private fun parseJson(bytes: ByteArray): JsonElement {
    return Json.parseToJsonElement(String(bytes, Charsets.UTF_8))
}

internal fun parseModel3(bytes: ByteArray): Model3References {
    val root = parseJson(bytes).asObject("Model3")
    root.required("Version").asU32("Version")
    val references = root.required("FileReferences").asObject("FileReferences")
    val moc = references.required("Moc").asString("Moc")
    val textures = references.required("Textures").asArray("Textures").map { it.asString("Textures") }
    val physics = references["Physics"]?.asOptionalString("Physics")
    references["DisplayInfo"]?.asOptionalString("DisplayInfo")
    return Model3References(moc, textures, physics)
}

private fun parseTarget(element: JsonElement, name: String): String {
    val target = element.asObject(name)
    val type = target.required("Target").asString("Target")
    if (type != "Parameter") {
        throw MetaException("unknown variant `$type`, expected `Parameter`")
    }
    return target.required("Id").asString("Id")
}

private fun parseRange(element: JsonElement, name: String): PhysicsRange {
    val range = element.asObject(name)
    return PhysicsRange(
        minimum = range.required("Minimum").asFloat("Minimum"),
        default = range.required("Default").asFloat("Default"),
        maximum = range.required("Maximum").asFloat("Maximum"),
    )
}

private fun parseVector(element: JsonElement, name: String): Pair<Float, Float> {
    val vector = element.asObject(name)
    return Pair(vector.required("X").asFloat("X"), vector.required("Y").asFloat("Y"))
}

internal fun parsePhysics3(bytes: ByteArray): Physics3Config {
    val root = parseJson(bytes).asObject("Physics3")
    root.required("Version").asU32("Version")
    val meta = root.required("Meta").asObject("Meta")
    val fpsElement = meta["Fps"]
    val fps = if (fpsElement == null || fpsElement is JsonNull) null else fpsElement.asFloat("Fps")
    val forces = meta.required("EffectiveForces").asObject("EffectiveForces")
    val gravity = parseVector(forces.required("Gravity"), "Gravity")
    parseVector(forces.required("Wind"), "Wind")
    for (entry in meta.required("PhysicsDictionary").asArray("PhysicsDictionary")) {
        val named = entry.asObject("PhysicsDictionary")
        named.required("Id").asString("Id")
        named.required("Name").asString("Name")
    }
    val settings = root.required("PhysicsSettings").asArray("PhysicsSettings").map { settingElement ->
        val setting = settingElement.asObject("PhysicsSetting")
        val id = setting.required("Id").asString("Id")
        val inputs = setting.required("Input").asArray("Input").map { inputElement ->
            val input = inputElement.asObject("Input")
            val source = parseTarget(input.required("Source"), "Source")
            val weight = input.required("Weight").asFloat("Weight")
            val isAngle = when (val type = input.required("Type").asString("Type")) {
                "X" -> false
                "Angle" -> true
                else -> throw MetaException("unknown variant `$type`, expected `X` or `Angle`")
            }
            val reflect = input.required("Reflect").asBool("Reflect")
            PhysicsInputConfig(source, weight, isAngle, reflect)
        }
        val outputs = setting.required("Output").asArray("Output").map { outputElement ->
            val output = outputElement.asObject("Output")
            PhysicsOutputConfig(
                destinationId = parseTarget(output.required("Destination"), "Destination"),
                vertexIndex = output.required("VertexIndex").asU32("VertexIndex"),
                scale = output.required("Scale").asFloat("Scale"),
                weight = output.required("Weight").asFloat("Weight"),
                reflect = output.required("Reflect").asBool("Reflect"),
            )
        }
        val vertices = setting.required("Vertices").asArray("Vertices").map { vertexElement ->
            val vertex = vertexElement.asObject("Vertex")
            PhysicsVertexConfig(
                mobility = vertex.required("Mobility").asFloat("Mobility"),
                delay = vertex.required("Delay").asFloat("Delay"),
                acceleration = vertex.required("Acceleration").asFloat("Acceleration"),
                radius = vertex.required("Radius").asFloat("Radius"),
            )
        }
        val normalization = setting.required("Normalization").asObject("Normalization")
        PhysicsSettingConfig(
            id = id,
            inputs = inputs,
            outputs = outputs,
            vertices = vertices,
            position = parseRange(normalization.required("Position"), "Position"),
            angle = parseRange(normalization.required("Angle"), "Angle"),
        )
    }
    return Physics3Config(fps = fps, gravityX = gravity.first, gravityY = gravity.second, settings = settings)
}
