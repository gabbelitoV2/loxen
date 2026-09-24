package com.moblin.android.platform.scenekit

import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.IdentityHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sign
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
            return entries.firstOrNull { it.rawValue == rawValue }
        }

        fun fromName(name: String): BlendShapePreset {
            return invoke(name.lowercase()) ?: unknown
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

    internal fun setUp(humanBones: List<VrmHumanBone>, nodes: Array<SCNNode?>) {
        val result = HashMap<Bones, SCNNode>()
        for (humanBone in humanBones) {
            val bone = Bones.entries.firstOrNull { it.name == humanBone.bone } ?: continue
            val node = nodes.getOrNull(humanBone.node) ?: continue
            result[bone] = node
        }
        synchronized(bones) {
            bones.clear()
            bones.putAll(result)
        }
    }
}

internal class VrmHumanBone(val bone: String, val node: Int)

internal class VrmBind(val index: Int, val mesh: Int, val weight: Double)

internal class VrmBlendShapeGroup(
    val binds: List<VrmBind>?,
    val name: String,
    val presetName: String,
    val isBinary: Boolean,
)

internal class VrmBoneGroup(
    val bones: List<Int>,
    val center: Int,
    val dragForce: Double,
    val gravityDir: Vec3,
    val gravityPower: Double,
    val hitRadius: Double,
    val stiffiness: Double,
)

internal class VrmCollider(val offset: Vec3, val radius: Double)

internal class VrmColliderGroup(val node: Int, val colliders: List<VrmCollider>)

internal class VrmMaterialProperty(
    val name: String,
    val shader: String,
    val renderQueue: Int,
    val keywordMap: Map<String, Boolean>,
)

internal class VrmDocument(
    val gltf: JsonObject,
    val binary: ByteArray?,
    val materialProperties: Map<String, VrmMaterialProperty>,
    val humanBones: List<VrmHumanBone>,
    val blendShapeGroups: List<VrmBlendShapeGroup>,
    val boneGroups: List<VrmBoneGroup>,
    val colliderGroups: List<VrmColliderGroup>,
)

private const val GLB_JSON = 0x4E4F534A
private const val GLB_BIN = 0x004E4942

private class GlbReader(private val data: ByteArray) {
    var offset = 0

    fun readU32(): Long {
        if (offset + 4 > data.size) {
            throw VRMError("dataInconsistent(failed to read data)")
        }
        val value = ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL
        offset += 4
        return value
    }

    fun read(size: Long): ByteArray {
        if (size < 0 || offset + size > data.size) {
            throw VRMError("dataInconsistent(failed to read data)")
        }
        val result = data.copyOfRange(offset, offset + size.toInt())
        offset += size.toInt()
        return result
    }
}

private fun keyNotFound(key: String): VRMError {
    return VRMError("keyNotFound($key)")
}

private fun JsonObject.requiredElement(key: String): JsonElement {
    val value = this[key]
    if (value == null || value is JsonNull) {
        throw keyNotFound(key)
    }
    return value
}

private fun JsonElement.objectValue(key: String): JsonObject {
    return this as? JsonObject ?: throw VRMError("dataInconsistent($key is not an object)")
}

private fun JsonElement.arrayValue(key: String): JsonArray {
    return this as? JsonArray ?: throw VRMError("dataInconsistent($key is not an array)")
}

private fun JsonElement.numberValue(key: String): Double {
    val primitive = this as? JsonPrimitive
    if (primitive == null || primitive.isString || primitive is JsonNull) {
        throw VRMError("dataInconsistent($key is not a number)")
    }
    return when (primitive.content) {
        "true", "false" -> throw VRMError("dataInconsistent($key is not a number)")
        else -> primitive.content.toDoubleOrNull() ?: throw VRMError("dataInconsistent($key is not a number)")
    }
}

private fun JsonElement.intValue(key: String): Int {
    return numberValue(key).toInt()
}

private fun JsonElement.boolValue(key: String): Boolean {
    val primitive = this as? JsonPrimitive
    if (primitive == null || primitive.isString || primitive is JsonNull) {
        throw VRMError("dataInconsistent($key is not a boolean)")
    }
    return when (primitive.content) {
        "true", "1" -> true
        "false", "0" -> false
        else -> throw VRMError("dataInconsistent($key is not a boolean)")
    }
}

private fun JsonElement.stringValue(key: String): String {
    val primitive = this as? JsonPrimitive
    if (primitive == null || !primitive.isString) {
        throw VRMError("dataInconsistent($key is not a string)")
    }
    return primitive.content
}

private fun JsonObject.optionalElement(key: String): JsonElement? {
    val value = this[key]
    return if (value == null || value is JsonNull) null else value
}

private fun vector3(element: JsonElement, key: String): Vec3 {
    val vector = element.objectValue(key)
    return Vec3(
        vector.requiredElement("x").numberValue("x").toFloat(),
        vector.requiredElement("y").numberValue("y").toFloat(),
        vector.requiredElement("z").numberValue("z").toFloat(),
    )
}

internal fun parseVrm(data: ByteArray): VrmDocument {
    val reader = GlbReader(data)
    reader.offset = 4
    val version = reader.readU32()
    if (version != 2L) {
        throw VRMError("notSupportedVersion($version)")
    }
    val length = reader.readU32()
    val jsonLength = reader.readU32()
    val jsonType = reader.readU32()
    if (jsonType != GLB_JSON.toLong()) {
        throw VRMError("notSupportedChunkType($jsonType)")
    }
    val jsonBytes = reader.read(jsonLength)
    var binary: ByteArray? = null
    if (length > reader.offset) {
        val binaryLength = reader.readU32()
        val binaryType = reader.readU32()
        if (binaryType != GLB_BIN.toLong()) {
            throw VRMError("notSupportedChunkType($binaryType)")
        }
        binary = reader.read(binaryLength)
    }
    val gltf = try {
        Json.parseToJsonElement(String(jsonBytes, Charsets.UTF_8)).objectValue("glTF")
    } catch (error: VRMError) {
        throw error
    } catch (error: Exception) {
        throw VRMError("dataCorrupted(${error.message})")
    }
    gltf.requiredElement("asset").objectValue("asset").requiredElement("version").stringValue("version")
    val extensions = gltf.optionalElement("extensions")?.objectValue("extensions") ?: throw keyNotFound("extensions")
    val vrm = extensions.optionalElement("VRM")?.objectValue("VRM") ?: throw keyNotFound("VRM")
    vrm.requiredElement("meta").objectValue("meta")
    val materialProperties = vrm.requiredElement("materialProperties").arrayValue("materialProperties").map {
        val property = it.objectValue("materialProperty")
        property.requiredElement("floatProperties")
        property.requiredElement("vectorProperties")
        property.requiredElement("tagMap").objectValue("tagMap").forEach { (key, value) -> value.stringValue(key) }
        property.requiredElement("textureProperties").objectValue("textureProperties")
            .forEach { (key, value) -> value.intValue(key) }
        VrmMaterialProperty(
            name = property.requiredElement("name").stringValue("name"),
            shader = property.requiredElement("shader").stringValue("shader"),
            renderQueue = property.requiredElement("renderQueue").intValue("renderQueue"),
            keywordMap = property.requiredElement("keywordMap").objectValue("keywordMap")
                .mapValues { (key, value) -> value.boolValue(key) },
        )
    }
    val humanoid = vrm.requiredElement("humanoid").objectValue("humanoid")
    for (key in listOf(
        "armStretch", "feetSpacing", "legStretch", "lowerArmTwist", "lowerLegTwist", "upperArmTwist",
        "upperLegTwist",
    )) {
        humanoid.requiredElement(key).numberValue(key)
    }
    humanoid.requiredElement("hasTranslationDoF").boolValue("hasTranslationDoF")
    val humanBones = humanoid.requiredElement("humanBones").arrayValue("humanBones").map {
        val bone = it.objectValue("humanBone")
        bone.requiredElement("useDefaultValues").boolValue("useDefaultValues")
        VrmHumanBone(
            bone = bone.requiredElement("bone").stringValue("bone"),
            node = bone.requiredElement("node").intValue("node"),
        )
    }
    val blendShapeMaster = vrm.requiredElement("blendShapeMaster").objectValue("blendShapeMaster")
    val blendShapeGroups = blendShapeMaster.requiredElement("blendShapeGroups").arrayValue("blendShapeGroups").map {
        val group = it.objectValue("blendShapeGroup")
        val binds = group.optionalElement("binds")?.arrayValue("binds")?.map { bindElement ->
            val bind = bindElement.objectValue("bind")
            VrmBind(
                index = bind.requiredElement("index").intValue("index"),
                mesh = bind.requiredElement("mesh").intValue("mesh"),
                weight = bind.requiredElement("weight").numberValue("weight"),
            )
        }
        group.optionalElement("materialValues")?.arrayValue("materialValues")?.forEach { valueElement ->
            val value = valueElement.objectValue("materialValue")
            value.requiredElement("materialName").stringValue("materialName")
            value.requiredElement("propertyName").stringValue("propertyName")
            value.requiredElement("targetValue").arrayValue("targetValue").forEach { it.numberValue("targetValue") }
        }
        VrmBlendShapeGroup(
            binds = binds,
            name = group.requiredElement("name").stringValue("name"),
            presetName = group.requiredElement("presetName").stringValue("presetName"),
            isBinary = group.optionalElement("isBinary")?.boolValue("isBinary") ?: false,
        )
    }
    val firstPerson = vrm.requiredElement("firstPerson").objectValue("firstPerson")
    firstPerson.requiredElement("firstPersonBone").intValue("firstPersonBone")
    vector3(firstPerson.requiredElement("firstPersonBoneOffset"), "firstPersonBoneOffset")
    firstPerson.requiredElement("meshAnnotations").arrayValue("meshAnnotations").forEach {
        val annotation = it.objectValue("meshAnnotation")
        annotation.requiredElement("firstPersonFlag").stringValue("firstPersonFlag")
        annotation.requiredElement("mesh").intValue("mesh")
    }
    val lookAtType = firstPerson.requiredElement("lookAtTypeName").stringValue("lookAtTypeName")
    if (lookAtType !in listOf("None", "Bone", "BlendShape")) {
        throw VRMError("dataCorrupted(Cannot initialize LookAtType from invalid String value $lookAtType)")
    }
    val secondaryAnimation = vrm.requiredElement("secondaryAnimation").objectValue("secondaryAnimation")
    val boneGroups = secondaryAnimation.requiredElement("boneGroups").arrayValue("boneGroups").map {
        val group = it.objectValue("boneGroup")
        group.requiredElement("colliderGroups").arrayValue("colliderGroups").forEach { index ->
            index.intValue("colliderGroups")
        }
        group.optionalElement("comment")?.stringValue("comment")
        VrmBoneGroup(
            bones = group.requiredElement("bones").arrayValue("bones").map { bone -> bone.intValue("bones") },
            center = group.requiredElement("center").intValue("center"),
            dragForce = group.requiredElement("dragForce").numberValue("dragForce"),
            gravityDir = vector3(group.requiredElement("gravityDir"), "gravityDir"),
            gravityPower = group.requiredElement("gravityPower").numberValue("gravityPower"),
            hitRadius = group.requiredElement("hitRadius").numberValue("hitRadius"),
            stiffiness = group.requiredElement("stiffiness").numberValue("stiffiness"),
        )
    }
    val colliderGroups = secondaryAnimation.requiredElement("colliderGroups").arrayValue("colliderGroups").map {
        val group = it.objectValue("colliderGroup")
        VrmColliderGroup(
            node = group.requiredElement("node").intValue("node"),
            colliders = group.requiredElement("colliders").arrayValue("colliders").map { colliderElement ->
                val collider = colliderElement.objectValue("collider")
                VrmCollider(
                    offset = vector3(collider.requiredElement("offset"), "offset"),
                    radius = collider.requiredElement("radius").numberValue("radius"),
                )
            },
        )
    }
    val propertyMap = LinkedHashMap<String, VrmMaterialProperty>()
    for (property in materialProperties) {
        propertyMap[property.name] = property
    }
    return VrmDocument(
        gltf = gltf,
        binary = binary,
        materialProperties = propertyMap,
        humanBones = humanBones,
        blendShapeGroups = blendShapeGroups,
        boneGroups = boneGroups,
        colliderGroups = colliderGroups,
    )
}

internal class BlendShapeBinding(val mesh: SCNNode, val index: Int, val weight: Double)

internal class BlendShapeClip(
    val name: String,
    val preset: BlendShapePreset,
    val values: List<BlendShapeBinding>,
    val isBinary: Boolean,
) {
    val key: BlendShapeKey
        get() = if (preset == BlendShapePreset.unknown) BlendShapeKey.custom(name) else BlendShapeKey.preset(preset)
}

private class VrmTimer {
    private var lastUpdateTime = 0.0

    fun deltaTime(updateAtTime: Double): Double {
        if (lastUpdateTime == 0.0) {
            lastUpdateTime = updateAtTime
        }
        val deltaTime = updateAtTime - lastUpdateTime
        lastUpdateTime = updateAtTime
        return deltaTime
    }
}

open class VRMNode internal constructor() : SCNNode() {
    val humanoid: Humanoid = Humanoid()

    private val timer = VrmTimer()
    private var springBones: List<VRMSpringBone> = emptyList()
    internal var blendShapeClips: Map<BlendShapeKey, BlendShapeClip> = emptyMap()

    internal fun setUpBlendShapes(groups: List<VrmBlendShapeGroup>, meshes: Array<SCNNode?>) {
        val clips = LinkedHashMap<BlendShapeKey, BlendShapeClip>()
        for (group in groups) {
            val bindings = group.binds?.mapNotNull { bind ->
                val mesh = meshes.getOrNull(bind.mesh) ?: return@mapNotNull null
                BlendShapeBinding(mesh = mesh, index = bind.index, weight = bind.weight)
            } ?: emptyList()
            val clip = BlendShapeClip(
                name = group.name,
                preset = BlendShapePreset.fromName(group.presetName),
                values = bindings,
                isBinary = group.isBinary,
            )
            clips[clip.key] = clip
        }
        blendShapeClips = clips
    }

    internal fun setUpSpringBones(springBones: List<VRMSpringBone>) {
        this.springBones = springBones
    }

    fun setBlendShape(value: Double, `for`: BlendShapeKey) {
        val clip = blendShapeClips[`for`] ?: return
        val clipValue = if (clip.isBinary) sign(value) * floor(abs(value) + 0.5) else value
        for (binding in clip.values) {
            val weight = binding.weight / 100.0
            val weights = binding.mesh.morphWeights ?: continue
            if (binding.index in weights.indices) {
                weights[binding.index] = (weight * clipValue).toFloat()
            }
        }
    }

    fun setBlendShape(value: Float, `for`: BlendShapeKey) {
        setBlendShape(value = value.toDouble(), `for` = `for`)
    }

    fun blendShape(`for`: BlendShapeKey): Double {
        val clip = blendShapeClips[`for`] ?: return 0.0
        val binding = clip.values.firstOrNull() ?: return 0.0
        val weights = binding.mesh.morphWeights ?: return 0.0
        return weights.getOrNull(binding.index)?.toDouble() ?: 0.0
    }

    open fun update(at: Double) {
        val seconds = timer.deltaTime(updateAtTime = at)
        for (springBone in springBones) {
            springBone.update(deltaTime = seconds)
        }
    }
}

internal class VrmFilamentModel(
    val glb: ByteArray,
    val nodeCount: Int,
    val meshCount: Int,
    val nodeMeshes: IntArray,
    val meshTargetCounts: IntArray,
    val meshBlendPrimitives: Array<BooleanArray>,
    val meshRenderOrders: Array<IntArray>,
    val largestSkin: Int,
)

class VRMScene internal constructor(
    val vrmNode: VRMNode,
    internal val gltfNodes: Array<SCNNode?>,
    internal val meshNodes: Array<SCNNode?>,
    filamentModel: VrmFilamentModel?,
) : SCNScene() {
    internal val filamentAsset: FilamentVrmAsset? = filamentModel?.let { FilamentVrmAsset.load(this, it) }

    init {
        rootNode.addChildNode(vrmNode)
    }
}

class VRMSceneLoader private constructor(private val vrm: VrmDocument, val rootDirectory: File?) {
    constructor(withURL: File, rootDirectory: File? = null) : this(parseVrm(readVrmFile(withURL)), rootDirectory)

    constructor(withURL: String, rootDirectory: String? = null) : this(
        parseVrm(readVrmFile(File(withURL))),
        rootDirectory?.let { File(it) }
    )

    constructor(withURL: URI, rootDirectory: URI? = null) : this(
        parseVrm(readVrmFile(File(withURL))),
        rootDirectory?.let { File(it) }
    )

    constructor(withData: ByteArray, rootDirectory: File? = null) : this(parseVrm(withData), rootDirectory)

    private val gltf = vrm.gltf
    private val gltfNodes = gltf.optionalElement("nodes")?.arrayValue("nodes") ?: JsonArray(emptyList())
    private val gltfMeshes = gltf.optionalElement("meshes")?.arrayValue("meshes") ?: JsonArray(emptyList())
    private val gltfMaterials = gltf.optionalElement("materials")?.arrayValue("materials") ?: JsonArray(emptyList())
    private val gltfCameras = gltf.optionalElement("cameras")?.arrayValue("cameras") ?: JsonArray(emptyList())
    private val nodes = arrayOfNulls<SCNNode>(gltfNodes.size)
    private val meshes = arrayOfNulls<SCNNode>(gltfMeshes.size)

    fun loadScene(): VRMScene {
        return loadScene(withFilament = true)
    }

    internal fun loadScene(withFilament: Boolean): VRMScene {
        val sceneIndex = gltf.optionalElement("scene")?.intValue("scene") ?: 0
        val scenes = gltf.optionalElement("scenes")?.arrayValue("scenes") ?: throw keyNotFound("scenes")
        val gltfScene = scenes.getOrNull(sceneIndex)?.objectValue("scene")
            ?: throw VRMError("dataInconsistent(scenes: out of index $sceneIndex < ${scenes.size})")
        val vrmNode = VRMNode()
        for (element in gltfScene.optionalElement("nodes")?.arrayValue("nodes") ?: JsonArray(emptyList())) {
            vrmNode.addChildNode(node(element.intValue("nodes")))
        }
        vrmNode.humanoid.setUp(vrm.humanBones, nodes)
        vrmNode.setUpBlendShapes(vrm.blendShapeGroups, meshes)
        vrmNode.setUpSpringBones(makeSpringBones())
        val filamentModel = if (withFilament) filamentModel() else null
        return VRMScene(vrmNode, nodes.copyOf(), meshes.copyOf(), filamentModel)
    }

    private fun makeSpringBones(): List<VRMSpringBone> {
        val springBones = ArrayList<VRMSpringBone>()
        for (boneGroup in vrm.boneGroups) {
            if (boneGroup.bones.isEmpty()) {
                return emptyList()
            }
            val rootBones = boneGroup.bones.map { node(it) }
            val centerNode = runCatching { node(boneGroup.center) }.getOrNull()
            val colliderGroups = vrm.colliderGroups.map { group ->
                VRMSpringBoneColliderGroup(
                    node = node(group.node),
                    colliders = group.colliders.map { VRMSpringBoneSphereCollider(it.offset, it.radius.toFloat()) },
                )
            }
            springBones.add(
                VRMSpringBone(
                    center = centerNode,
                    rootBones = rootBones,
                    stiffnessForce = boneGroup.stiffiness.toFloat(),
                    gravityPower = boneGroup.gravityPower.toFloat(),
                    gravityDir = boneGroup.gravityDir,
                    dragForce = boneGroup.dragForce.toFloat(),
                    hitRadius = boneGroup.hitRadius.toFloat(),
                    colliderGroups = colliderGroups,
                )
            )
        }
        return springBones
    }

    private fun node(index: Int): SCNNode {
        if (index < 0 || index >= nodes.size) {
            throw VRMError("dataInconsistent(nodes: out of index $index < ${nodes.size})")
        }
        nodes[index]?.let { return it }
        val gltfNode = gltfNodes[index].objectValue("node")
        val node = SCNNode()
        node.name = (gltfNode["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        gltfNode.optionalElement("camera")?.let { node.camera = camera(it.intValue("camera")) }
        gltfNode.optionalElement("mesh")?.let { meshElement ->
            node.addChildNode(mesh(meshElement.intValue("mesh")))
        }
        val matrix = gltfNode.optionalElement("matrix")?.arrayValue("matrix")
        if (matrix != null) {
            if (matrix.size != 16) {
                throw VRMError("scnMatrix4Not16")
            }
            node.setTransformMatrix(FloatArray(16) { matrix[it].numberValue("matrix").toFloat() })
        } else {
            gltfNode.optionalElement("translation")?.arrayValue("translation")?.let {
                node.localTranslation = Vec3(
                    it[0].numberValue("translation").toFloat(),
                    it[1].numberValue("translation").toFloat(),
                    it[2].numberValue("translation").toFloat(),
                )
            }
            gltfNode.optionalElement("rotation")?.arrayValue("rotation")?.let {
                node.localRotation = Quat(
                    it[0].numberValue("rotation").toFloat(),
                    it[1].numberValue("rotation").toFloat(),
                    it[2].numberValue("rotation").toFloat(),
                    it[3].numberValue("rotation").toFloat(),
                )
            }
            gltfNode.optionalElement("scale")?.arrayValue("scale")?.let {
                node.scale = SCNVector3(
                    it[0].numberValue("scale").toFloat(),
                    it[1].numberValue("scale").toFloat(),
                    it[2].numberValue("scale").toFloat(),
                )
            }
        }
        nodes[index] = node
        for (child in gltfNode.optionalElement("children")?.arrayValue("children") ?: JsonArray(emptyList())) {
            node.addChildNode(node(child.intValue("children")))
        }
        return node
    }

    private fun camera(index: Int): SCNCamera {
        val gltfCamera = gltfCameras.getOrNull(index)?.objectValue("camera")
            ?: throw VRMError("dataInconsistent(cameras: out of index $index < ${gltfCameras.size})")
        val camera = SCNCamera()
        camera.name = (gltfCamera["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        when (gltfCamera.requiredElement("type").stringValue("type")) {
            "perspective" -> {
                val perspective = gltfCamera.optionalElement("perspective")?.objectValue("perspective")
                    ?: throw keyNotFound("perspective")
                camera.usesOrthographicProjection = false
                camera.fieldOfView = perspective.requiredElement("yfov").numberValue("yfov") * 180.0 / PI
                camera.zNear = perspective.requiredElement("znear").numberValue("znear")
                camera.zFar = perspective.optionalElement("zfar")?.numberValue("zfar") ?: Double.POSITIVE_INFINITY
            }
            "orthographic" -> {
                val orthographic = gltfCamera.optionalElement("orthographic")?.objectValue("orthographic")
                    ?: throw keyNotFound("orthographic")
                camera.usesOrthographicProjection = true
                camera.orthographicScale = orthographic.requiredElement("ymag").numberValue("ymag")
                camera.zNear = orthographic.requiredElement("znear").numberValue("znear")
                camera.zFar = orthographic.requiredElement("zfar").numberValue("zfar")
            }
            else -> throw VRMError("dataCorrupted(camera type)")
        }
        return camera
    }

    private fun materialProperty(materialIndex: Int?): VrmMaterialProperty? {
        val material = materialIndex?.let { gltfMaterials.getOrNull(it) as? JsonObject } ?: return null
        val name = (material["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
        return vrm.materialProperties[name]
    }

    private fun mesh(index: Int): SCNNode {
        if (index < 0 || index >= meshes.size) {
            throw VRMError("dataInconsistent(meshes: out of index $index < ${meshes.size})")
        }
        meshes[index]?.let { return it }
        val gltfMesh = gltfMeshes[index].objectValue("mesh")
        val node = SCNNode()
        node.name = (gltfMesh["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        var targetCount = 0
        for (primitiveElement in gltfMesh.requiredElement("primitives").arrayValue("primitives")) {
            val primitive = primitiveElement.objectValue("primitive")
            val primitiveNode = SCNNode()
            val property = materialProperty(primitive.optionalElement("material")?.intValue("material"))
            if (property != null && property.renderQueue != -1) {
                val lastRenderingOrder = node.childNodes.lastOrNull()?.renderingOrder ?: 0
                primitiveNode.renderingOrder = if (lastRenderingOrder == 0) {
                    property.renderQueue
                } else {
                    property.renderQueue + 1
                }
            }
            val targets = primitive.optionalElement("targets")?.arrayValue("targets")
            if (targets != null && targets.isNotEmpty()) {
                targetCount = maxOf(targetCount, targets.size)
            }
            node.addChildNode(primitiveNode)
        }
        node.morphWeights = FloatArray(targetCount)
        meshes[index] = node
        return node
    }

    internal fun filamentModel(): VrmFilamentModel {
        val meshCount = gltfMeshes.size
        val nodeMeshes = IntArray(gltfNodes.size) { index ->
            (gltfNodes[index] as? JsonObject)?.optionalElement("mesh")?.intValue("mesh") ?: -1
        }
        val targetCounts = IntArray(meshCount) { meshes[it]?.morphWeights?.size ?: 0 }
        val renderOrders = Array(meshCount) { index ->
            val meshNode = meshes[index]
            if (meshNode == null) {
                IntArray(0)
            } else {
                meshNode.childNodes.map { it.renderingOrder }.toIntArray()
            }
        }
        val skins = gltf.optionalElement("skins")?.arrayValue("skins") ?: JsonArray(emptyList())
        var largestSkin = 0
        for (element in gltfNodes) {
            val node = element as? JsonObject ?: continue
            if (node.optionalElement("mesh") == null) {
                continue
            }
            val skin = node.optionalElement("skin")?.intValue("skin") ?: continue
            val joints = (skins.getOrNull(skin) as? JsonObject)?.optionalElement("joints") as? JsonArray ?: continue
            largestSkin = maxOf(largestSkin, joints.size)
        }
        val (json, blendPrimitives) = filamentJson()
        return VrmFilamentModel(
            glb = writeGlb(json, vrm.binary),
            nodeCount = gltfNodes.size,
            meshCount = meshCount,
            nodeMeshes = nodeMeshes,
            meshTargetCounts = targetCounts,
            meshBlendPrimitives = blendPrimitives,
            meshRenderOrders = renderOrders,
            largestSkin = largestSkin,
        )
    }

    private fun filamentJson(): Pair<JsonObject, Array<BooleanArray>> {
        val root = LinkedHashMap<String, JsonElement>(gltf)
        root.remove("extensions")
        root.remove("extensionsRequired")
        val used = (gltf["extensionsUsed"] as? JsonArray)?.filterIsInstance<JsonPrimitive>()
            ?.map { it.content }?.filter { it != "VRM" && it != "KHR_texture_transform" }?.toMutableList()
            ?: mutableListOf()
        if (!used.contains("KHR_materials_unlit")) {
            used.add("KHR_materials_unlit")
        }
        root["extensionsUsed"] = JsonArray(used.map { JsonPrimitive(it) })
        root["nodes"] = JsonArray(
            gltfNodes.mapIndexed { index, element ->
                val node = LinkedHashMap<String, JsonElement>(element.objectValue("node"))
                node["name"] = JsonPrimitive(filamentNodeName(index))
                node.remove("extensions")
                JsonObject(node)
            }
        )
        val materialModes = ArrayList<String>()
        val materials = ArrayList<JsonElement>()
        for ((index, element) in gltfMaterials.withIndex()) {
            val (material, mode) = filamentMaterial(index, element.objectValue("material"))
            materials.add(material)
            materialModes.add(mode)
        }
        val defaultMaterial = materials.size
        materials.add(
            JsonObject(
                mapOf(
                    "name" to JsonPrimitive("moblin:default"),
                    "pbrMetallicRoughness" to JsonObject(
                        mapOf("baseColorFactor" to JsonArray(listOf(0, 0, 0, 1).map { JsonPrimitive(it) }))
                    ),
                    "extensions" to JsonObject(mapOf("KHR_materials_unlit" to JsonObject(emptyMap()))),
                )
            )
        )
        materialModes.add("OPAQUE")
        root["materials"] = JsonArray(materials)
        val blendPrimitives = ArrayList<BooleanArray>()
        root["meshes"] = JsonArray(
            gltfMeshes.map { meshElement ->
                val mesh = LinkedHashMap<String, JsonElement>(meshElement.objectValue("mesh"))
                val primitives = mesh.getValue("primitives").arrayValue("primitives")
                val blends = BooleanArray(primitives.size)
                mesh["primitives"] = JsonArray(
                    primitives.mapIndexed { primitiveIndex, primitiveElement ->
                        val primitive = LinkedHashMap<String, JsonElement>(primitiveElement.objectValue("primitive"))
                        val attributes = LinkedHashMap<String, JsonElement>(
                            primitive["attributes"]?.objectValue("attributes") ?: JsonObject(emptyMap())
                        )
                        attributes.remove("COLOR_0")
                        primitive["attributes"] = JsonObject(attributes)
                        val material = (primitive["material"] as? JsonPrimitive)?.content?.toIntOrNull()
                        val materialIndex = if (material == null || material !in gltfMaterials.indices) {
                            defaultMaterial
                        } else {
                            material
                        }
                        primitive["material"] = JsonPrimitive(materialIndex)
                        blends[primitiveIndex] = materialModes[materialIndex] == "BLEND"
                        JsonObject(primitive)
                    }
                )
                blendPrimitives.add(blends)
                JsonObject(mesh)
            }
        )
        return Pair(JsonObject(root), blendPrimitives.toTypedArray())
    }

    private fun filamentMaterial(index: Int, material: JsonObject): Pair<JsonObject, String> {
        val result = LinkedHashMap<String, JsonElement>()
        material["name"]?.let { result["name"] = it }
        material["doubleSided"]?.let { result["doubleSided"] = it }
        material["alphaCutoff"]?.let { result["alphaCutoff"] = it }
        val property = materialProperty(index)
        val shader = property?.shader
        val alphaMode = (material["alphaMode"] as? JsonPrimitive)?.content ?: "OPAQUE"
        val mode = if (property != null && shader == "VRM/UnlitTransparent") {
            "BLEND"
        } else if (property != null && property.keywordMap["_ALPHAPREMULTIPLY_ON"] == true) {
            "BLEND"
        } else {
            when (alphaMode) {
                "BLEND" -> "BLEND"
                "MASK" -> "MASK"
                else -> "OPAQUE"
            }
        }
        result["alphaMode"] = JsonPrimitive(mode)
        val pbr = material["pbrMetallicRoughness"] as? JsonObject
        val newPbr = LinkedHashMap<String, JsonElement>()
        if (pbr != null) {
            val factor = pbr["baseColorFactor"] as? JsonArray
            val texture = pbr["baseColorTexture"] as? JsonObject
            if (texture != null) {
                val textureInfo = LinkedHashMap<String, JsonElement>()
                texture["index"]?.let { textureInfo["index"] = it }
                texture["texCoord"]?.let { textureInfo["texCoord"] = it }
                newPbr["baseColorTexture"] = JsonObject(textureInfo)
                newPbr["baseColorFactor"] = if (shader == "VRM/MToon") {
                    factor ?: JsonArray(listOf(0, 0, 0, 0).map { JsonPrimitive(it) })
                } else {
                    JsonArray(listOf(1, 1, 1, 1).map { JsonPrimitive(it) })
                }
            } else {
                newPbr["baseColorFactor"] = factor ?: JsonArray(listOf(0, 0, 0, 0).map { JsonPrimitive(it) })
            }
        }
        result["pbrMetallicRoughness"] = JsonObject(newPbr)
        result["extensions"] = JsonObject(mapOf("KHR_materials_unlit" to JsonObject(emptyMap())))
        return Pair(JsonObject(result), mode)
    }
}

internal fun filamentNodeName(index: Int): String {
    return "moblin:$index"
}

private fun writeGlb(json: JsonObject, binary: ByteArray?): ByteArray {
    var jsonBytes = json.toString().toByteArray(Charsets.UTF_8)
    val jsonPadding = (4 - jsonBytes.size % 4) % 4
    if (jsonPadding != 0) {
        jsonBytes += ByteArray(jsonPadding) { ' '.code.toByte() }
    }
    val binaryBytes = binary ?: ByteArray(0)
    val binaryPadding = (4 - binaryBytes.size % 4) % 4
    val binaryChunkLength = if (binary == null) 0 else 8 + binaryBytes.size + binaryPadding
    val total = 12 + 8 + jsonBytes.size + binaryChunkLength
    val buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
    buffer.putInt(0x46546C67)
    buffer.putInt(2)
    buffer.putInt(total)
    buffer.putInt(jsonBytes.size)
    buffer.putInt(GLB_JSON)
    buffer.put(jsonBytes)
    if (binary != null) {
        buffer.putInt(binaryBytes.size + binaryPadding)
        buffer.putInt(GLB_BIN)
        buffer.put(binaryBytes)
        buffer.put(ByteArray(binaryPadding))
    }
    return buffer.array()
}

private fun readVrmFile(file: File): ByteArray {
    return try {
        file.readBytes()
    } catch (error: Exception) {
        throw VRMError("Cannot read VRM file ${file.path}: ${error.message}")
    }
}

internal class VRMSpringBoneSphereCollider(val offset: Vec3, val radius: Float)

internal class VRMSpringBoneColliderGroup(val node: SCNNode, val colliders: List<VRMSpringBoneSphereCollider>)

internal class VRMSpringBone(
    private val center: SCNNode?,
    private val rootBones: List<SCNNode>,
    private val stiffnessForce: Float,
    private val gravityPower: Float,
    private val gravityDir: Vec3,
    private val dragForce: Float,
    private val hitRadius: Float,
    private val colliderGroups: List<VRMSpringBoneColliderGroup>,
) {
    private class SphereCollider(val position: Vec3, val radius: Float)

    private var initialLocalRotationMap = IdentityHashMap<SCNNode, Quat>()
    private var verlet = ArrayList<Logic>()
    private var colliderList = ArrayList<SphereCollider>()

    init {
        setup()
    }

    private fun setup() {
        for ((node, rotation) in initialLocalRotationMap) {
            node.localRotation = rotation
        }
        initialLocalRotationMap = IdentityHashMap()
        verlet = ArrayList()
        for (go in rootBones) {
            go.enumerateHierarchy { initialLocalRotationMap[it] = it.localRotation }
            setupRecursive(center, go)
        }
    }

    private fun setupRecursive(center: SCNNode?, parent: SCNNode) {
        if (parent.childCount == 0) {
            val parentPosition = parent.parent?.worldPosition ?: parent.worldPosition
            val delta = parent.worldPosition - parentPosition
            val childPosition = parent.worldPosition + delta.normalized() * 0.07f
            val localChildPosition = Mat4.transformPoint(Mat4.invert(parent.worldMatrix()), childPosition)
            verlet.add(Logic(center, parent, localChildPosition))
        } else {
            val firstChild = parent.firstChild ?: return
            val localPosition = firstChild.localTranslation
            val scale = firstChild.lossyScale()
            verlet.add(
                Logic(
                    center,
                    parent,
                    Vec3(localPosition.x * scale.x, localPosition.y * scale.y, localPosition.z * scale.z),
                )
            )
        }
        for (child in parent.childNodes) {
            setupRecursive(center, child)
        }
    }

    fun update(deltaTime: Double) {
        if (verlet.isEmpty()) {
            if (rootBones.isEmpty()) {
                return
            }
            setup()
        }
        colliderList = ArrayList()
        for (group in colliderGroups) {
            for (collider in group.colliders) {
                colliderList.add(SphereCollider(group.node.convertPositionToWorld(collider.offset), collider.radius))
            }
        }
        val stiffness = stiffnessForce * deltaTime.toFloat()
        val external = gravityDir * (gravityPower * deltaTime.toFloat())
        for (logic in verlet) {
            logic.radius = hitRadius
            logic.update(center, stiffness, dragForce, external, colliderList)
        }
    }

    private class Logic(center: SCNNode?, val node: SCNNode, localChildPosition: Vec3) {
        private val length: Float
        private var currentTail: Vec3
        private var prevTail: Vec3
        private val localRotation: Quat
        private val boneAxis: Vec3
        var radius = 0.5f

        private val parentRotation: Quat
            get() = node.parent?.worldOrientation ?: Quat.identity

        init {
            val worldChildPosition = node.convertPositionToWorld(localChildPosition)
            currentTail = center?.convertPositionFromWorld(worldChildPosition) ?: worldChildPosition
            prevTail = currentTail
            localRotation = node.localRotation
            boneAxis = localChildPosition.normalized()
            length = localChildPosition.length()
        }

        fun update(
            center: SCNNode?,
            stiffnessForce: Float,
            dragForce: Float,
            external: Vec3,
            colliders: List<SphereCollider>,
        ) {
            val currentTail = center?.convertPositionToWorld(this.currentTail) ?: this.currentTail
            val prevTail = center?.convertPositionToWorld(this.prevTail) ?: this.prevTail
            var nextTail = currentTail +
                (currentTail - prevTail) * (1f - dragForce) +
                (parentRotation * localRotation).act(boneAxis) * stiffnessForce +
                external
            val position = node.worldPosition
            nextTail = position + (nextTail - position).normalized() * length
            nextTail = collision(colliders, nextTail)
            this.prevTail = center?.convertPositionFromWorld(currentTail) ?: currentTail
            this.currentTail = center?.convertPositionFromWorld(nextTail) ?: nextTail
            node.worldOrientation = applyRotation(nextTail)
        }

        private fun applyRotation(nextTail: Vec3): Quat {
            val rotation = parentRotation * localRotation
            return Quat.rotation(rotation.act(boneAxis), nextTail - node.worldPosition) * rotation
        }

        private fun collision(colliders: List<SphereCollider>, nextTail: Vec3): Vec3 {
            var result = nextTail
            for (collider in colliders) {
                val r = radius + collider.radius
                if ((result - collider.position).lengthSquared() <= r * r) {
                    val normal = (result - collider.position).normalized()
                    val positionFromCollider = collider.position + normal * (radius + collider.radius)
                    val position = node.worldPosition
                    result = position + (positionFromCollider - position).normalized() * length
                }
            }
            return result
        }
    }
}
