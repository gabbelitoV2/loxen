package com.moblin.android.platform.scenekit

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class SceneKitTest {
    private fun assertClose(expected: Vec3, actual: Vec3, tolerance: Float = 1e-5f) {
        assertTrue(abs(expected.x - actual.x) <= tolerance, "x: $expected != $actual")
        assertTrue(abs(expected.y - actual.y) <= tolerance, "y: $expected != $actual")
        assertTrue(abs(expected.z - actual.z) <= tolerance, "z: $expected != $actual")
    }

    @Test
    fun eulerAnglesApplyRollThenYawThenPitch() {
        val node = SCNNode()
        node.eulerAngles = SCNVector3(0.3f, -0.5f, 0.7f)
        val v = Vec3(0.2f, 0.4f, -0.9f)
        val expected = Quat.fromAxisAngle(1f, 0f, 0f, 0.3f).act(
            Quat.fromAxisAngle(0f, 1f, 0f, -0.5f).act(Quat.fromAxisAngle(0f, 0f, 1f, 0.7f).act(v))
        )
        assertClose(expected, node.worldOrientation.act(v))
        val angles = node.eulerAngles
        assertEquals(0.3f, angles.x, 1e-5f)
        assertEquals(-0.5f, angles.y, 1e-5f)
        assertEquals(0.7f, angles.z, 1e-5f)
    }

    @Test
    fun rollRotatesCounterClockwiseAroundZ() {
        val node = SCNNode()
        node.eulerAngles = SCNVector3(0f, 0f, (PI / 6).toFloat())
        assertClose(Vec3(cos(PI / 6).toFloat(), sin(PI / 6).toFloat(), 0f), node.worldOrientation.act(Vec3(1f, 0f, 0f)))
    }

    @Test
    fun axisAngleRotationTurnsTheCameraAround() {
        val camera = SCNNode()
        camera.position = SCNVector3(0, 1.2, -1.8)
        camera.rotation = SCNVector4(0, 1, 0, PI.toFloat())
        val world = camera.worldMatrix()
        val forward = Vec3(-world[8], -world[9], -world[10])
        assertClose(Vec3(0f, 0f, 1f), forward)
        assertClose(Vec3(0f, 1.2f, -1.8f), camera.worldPosition)
    }

    @Test
    fun worldTransformsFollowTheHierarchy() {
        val root = SCNNode()
        root.position = SCNVector3(1f, 2f, 3f)
        root.eulerAngles = SCNVector3(0f, (PI / 2).toFloat(), 0f)
        val child = SCNNode()
        child.position = SCNVector3(0f, 0f, 1f)
        root.addChildNode(child)
        assertClose(Vec3(2f, 2f, 3f), child.worldPosition)
        assertClose(Vec3(2f, 2f, 3f), root.convertPositionToWorld(Vec3(0f, 0f, 1f)))
        assertClose(Vec3(0f, 0f, 1f), root.convertPositionFromWorld(Vec3(2f, 2f, 3f)))
        val target = Quat.fromAxisAngle(1f, 0f, 0f, 0.4f)
        child.worldOrientation = target
        val world = child.worldOrientation
        val v = Vec3(0.1f, 0.5f, 0.3f)
        assertClose(target.act(v), world.act(v))
    }

    @Test
    fun matrixDecompositionRoundTrips() {
        val translation = Vec3(0.5f, -1f, 2f)
        val rotation = Quat.fromEuler(0.2f, 0.4f, -0.3f)
        val scale = Vec3(1f, 2f, 0.5f)
        val node = SCNNode()
        node.setTransformMatrix(Mat4.compose(translation, rotation, scale))
        val matrix = node.localMatrix()
        val expected = Mat4.compose(translation, rotation, scale)
        for (i in 0 until 16) {
            assertEquals(expected[i], matrix[i], 1e-5f)
        }
    }

    @Test
    fun rotationBetweenUnitVectors() {
        val from = Vec3(1f, 0f, 0f)
        val to = Vec3(0f, 1f, 0f)
        assertClose(to, Quat.rotation(from, to).act(from))
        val opposite = Quat.rotation(from, Vec3(-1f, 0f, 0f))
        assertClose(Vec3(-1f, 0f, 0f), opposite.act(from))
        val obtuse = Vec3(-0.6f, 0.8f, 0f)
        assertClose(obtuse, Quat.rotation(from, obtuse).act(from))
    }

    @Test
    fun rotationTowardsAShortVectorTurnsLessAsInSimd() {
        val from = Vec3(1f, 0f, 0f)
        val to = Vec3(0f, 0.05f, 0f)
        val rotated = Quat.rotation(from, to).act(from)
        assertTrue(rotated.y > 0f)
        assertTrue(rotated.y < 0.2f, "simd_quaternion does not normalize its arguments: $rotated")
    }

    private fun glb(json: String, binary: ByteArray? = null): ByteArray {
        var jsonBytes = json.toByteArray()
        while (jsonBytes.size % 4 != 0) {
            jsonBytes += ' '.code.toByte()
        }
        val binaryBytes = binary ?: ByteArray(0)
        val total = 12 + 8 + jsonBytes.size + if (binary == null) 0 else 8 + binaryBytes.size
        val buffer = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x46546C67)
        buffer.putInt(2)
        buffer.putInt(total)
        buffer.putInt(jsonBytes.size)
        buffer.putInt(0x4E4F534A)
        buffer.put(jsonBytes)
        if (binary != null) {
            buffer.putInt(binaryBytes.size)
            buffer.putInt(0x004E4942)
            buffer.put(binaryBytes)
        }
        return buffer.array()
    }

    private fun parseGlbJson(data: ByteArray): JsonObject {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0x46546C67, buffer.getInt(0))
        assertEquals(data.size, buffer.getInt(8))
        val length = buffer.getInt(12)
        assertEquals(0x4E4F534A, buffer.getInt(16))
        return Json.parseToJsonElement(String(data, 20, length)).jsonObject
    }

    private val vrmJson = """
        {
          "asset": {"version": "2.0"},
          "extensionsUsed": ["VRM", "KHR_texture_transform"],
          "scene": 0,
          "scenes": [{"nodes": [0, 6]}],
          "nodes": [
            {"name": "Armature", "children": [1]},
            {"name": "Hips", "translation": [0, 1, 0], "rotation": [0, 0, 0, 1], "children": [2]},
            {"name": "Spine", "translation": [0, 0.2, 0], "rotation": [0, 0, 0, 1], "children": [3, 4]},
            {"name": "Neck", "translation": [0, 0.3, 0], "rotation": [0, 0, 0, 1]},
            {"name": "Hair", "translation": [0.1, 0.1, 0], "rotation": [0, 0, 0, 1], "children": [5]},
            {"name": "HairTip", "translation": [0.1, 0, 0], "rotation": [0, 0, 0, 1]},
            {"name": "Body", "mesh": 0}
          ],
          "meshes": [
            {
              "name": "Face",
              "primitives": [
                {"attributes": {"POSITION": 0, "COLOR_0": 1}, "material": 0, "targets": [{"POSITION": 0}, {"POSITION": 0}]},
                {"attributes": {"POSITION": 0}, "material": 1},
                {"attributes": {"POSITION": 0}}
              ]
            }
          ],
          "materials": [
            {
              "name": "Skin",
              "alphaMode": "MASK",
              "doubleSided": true,
              "emissiveFactor": [1, 1, 1],
              "pbrMetallicRoughness": {
                "baseColorFactor": [0.5, 0.5, 0.5, 1],
                "baseColorTexture": {"index": 0, "extensions": {"KHR_texture_transform": {"scale": [2, 2]}}}
              }
            },
            {"name": "Hair", "pbrMetallicRoughness": {"baseColorFactor": [1, 0, 0, 1]}}
          ],
          "extensions": {
            "VRM": {
              "meta": {"title": "Test"},
              "materialProperties": [
                {"name": "Skin", "shader": "VRM/MToon", "renderQueue": 2450, "floatProperties": {}, "keywordMap": {},
                 "tagMap": {"RenderType": "TransparentCutout"}, "textureProperties": {"_MainTex": 0}, "vectorProperties": {}},
                {"name": "Hair", "shader": "VRM/UnlitTransparent", "renderQueue": 3000, "floatProperties": {}, "keywordMap": {},
                 "tagMap": {}, "textureProperties": {}, "vectorProperties": {}}
              ],
              "humanoid": {
                "armStretch": 0.05, "feetSpacing": 0, "hasTranslationDoF": false, "legStretch": 0.05,
                "lowerArmTwist": 0.5, "lowerLegTwist": 0.5, "upperArmTwist": 0.5, "upperLegTwist": 0.5,
                "humanBones": [
                  {"bone": "hips", "node": 1, "useDefaultValues": true},
                  {"bone": "chest", "node": 2, "useDefaultValues": true},
                  {"bone": "spine", "node": 2, "useDefaultValues": true},
                  {"bone": "neck", "node": 3, "useDefaultValues": true}
                ]
              },
              "blendShapeMaster": {
                "blendShapeGroups": [
                  {"name": "A", "presetName": "a", "binds": [{"mesh": 0, "index": 1, "weight": 100}], "materialValues": []},
                  {"name": "Blink", "presetName": "blink", "binds": [{"mesh": 0, "index": 0, "weight": 50}], "isBinary": true},
                  {"name": "Smile", "presetName": "unknown", "binds": [{"mesh": 0, "index": 0, "weight": 100}]}
                ]
              },
              "firstPerson": {
                "firstPersonBone": 3, "firstPersonBoneOffset": {"x": 0, "y": 0.06, "z": 0},
                "meshAnnotations": [{"firstPersonFlag": "Auto", "mesh": 0}], "lookAtTypeName": "Bone"
              },
              "secondaryAnimation": {
                "boneGroups": [
                  {"bones": [4], "center": -1, "colliderGroups": [], "comment": "hair", "dragForce": 0.4,
                   "gravityDir": {"x": 0, "y": -1, "z": 0}, "gravityPower": 1, "hitRadius": 0.02, "stiffiness": 1}
                ],
                "colliderGroups": []
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun vrmSceneMirrorsVrmKitNodes() {
        val loader = VRMSceneLoader(withData = glb(vrmJson))
        val scene = loader.loadScene(withFilament = false)
        val vrmNode = scene.vrmNode
        assertSame(vrmNode, scene.rootNode.childNodes.single())
        assertEquals(listOf("Armature", "Body"), vrmNode.childNodes.map { it.name })
        val body = vrmNode.childNodes[1]
        val face = body.childNodes.single()
        assertEquals("Face", face.name)
        assertEquals(3, face.childNodes.size)
        assertEquals(listOf(2450, 3001, 0), face.childNodes.map { it.renderingOrder })
        assertEquals("Hips", vrmNode.humanoid.node(Humanoid.Bones.hips)?.name)
        assertEquals("Spine", vrmNode.humanoid.node(Humanoid.Bones.spine)?.name)
        assertEquals("Neck", vrmNode.humanoid.node(Humanoid.Bones.neck)?.name)
        assertNull(vrmNode.humanoid.node(Humanoid.Bones.upperChest))
        assertClose(Vec3(0f, 1.5f, 0f), vrmNode.humanoid.node(Humanoid.Bones.neck)!!.worldPosition)
    }

    @Test
    fun blendShapesScaleByBindWeightAndRoundBinaryClips() {
        val scene = VRMSceneLoader(withData = glb(vrmJson)).loadScene(withFilament = false)
        val node = scene.vrmNode
        val weights = assertNotNull(scene.meshNodes[0]?.morphWeights)
        assertEquals(2, weights.size)
        node.setBlendShape(value = 0.7, `for` = BlendShapeKey.preset(BlendShapePreset.a))
        assertEquals(0.7f, weights[1], 1e-6f)
        node.setBlendShape(value = 0.4, `for` = BlendShapeKey.preset(BlendShapePreset.blink))
        assertEquals(0f, weights[0])
        node.setBlendShape(value = 0.6, `for` = BlendShapeKey.preset(BlendShapePreset.blink))
        assertEquals(0.5f, weights[0], 1e-6f)
        node.setBlendShape(value = 0.25, `for` = BlendShapeKey.custom("Smile"))
        assertEquals(0.25f, weights[0], 1e-6f)
        assertEquals(0.25, node.blendShape(`for` = BlendShapeKey.custom("Smile")), 1e-6)
    }

    @Test
    fun springBonesFallWithGravity() {
        val scene = VRMSceneLoader(withData = glb(vrmJson)).loadScene(withFilament = false)
        val node = scene.vrmNode
        val hair = assertNotNull(scene.gltfNodes[4])
        val tip = assertNotNull(scene.gltfNodes[5])
        val before = tip.worldPosition
        node.update(at = 10.0)
        assertClose(before, tip.worldPosition)
        for (i in 1..30) {
            node.update(at = 10.0 + i / 30.0)
        }
        val after = tip.worldPosition
        assertTrue(after.y < before.y, "the hair tip falls: $before -> $after")
        assertClose(hair.worldPosition, scene.gltfNodes[4]!!.worldPosition)
        assertEquals(0.1f, (after - hair.worldPosition).length(), 1e-4f)
    }

    @Test
    fun filamentModelUsesVrmKitMaterials() {
        val loader = VRMSceneLoader(withData = glb(vrmJson))
        loader.loadScene(withFilament = false)
        val model = loader.filamentModel()
        assertEquals(7, model.nodeCount)
        assertEquals(listOf(-1, -1, -1, -1, -1, -1, 0), model.nodeMeshes.toList())
        assertEquals(listOf(2), model.meshTargetCounts.toList())
        assertEquals(listOf(false, true, false), model.meshBlendPrimitives[0].toList())
        assertEquals(listOf(2450, 3001, 0), model.meshRenderOrders[0].toList())
        val json = parseGlbJson(model.glb)
        assertNull(json["extensions"])
        val used = json["extensionsUsed"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("KHR_materials_unlit"), used)
        val nodes = json["nodes"]!!.jsonArray
        assertEquals((0 until 7).map { "moblin:$it" }, nodes.map { it.jsonObject["name"]!!.jsonPrimitive.content })
        val materials = json["materials"]!!.jsonArray
        assertEquals(3, materials.size)
        val skin = materials[0].jsonObject
        assertEquals("MASK", skin["alphaMode"]!!.jsonPrimitive.content)
        assertTrue(skin["extensions"]!!.jsonObject.containsKey("KHR_materials_unlit"))
        assertNull(skin["emissiveFactor"])
        val skinPbr = skin["pbrMetallicRoughness"]!!.jsonObject
        assertEquals(listOf(0.5f, 0.5f, 0.5f, 1f), skinPbr["baseColorFactor"]!!.jsonArray.map { it.jsonPrimitive.float })
        assertFalse(skinPbr["baseColorTexture"]!!.jsonObject.containsKey("extensions"))
        val hair = materials[1].jsonObject
        assertEquals("BLEND", hair["alphaMode"]!!.jsonPrimitive.content)
        assertEquals(
            listOf(1f, 0f, 0f, 1f),
            hair["pbrMetallicRoughness"]!!.jsonObject["baseColorFactor"]!!.jsonArray.map { it.jsonPrimitive.float }
        )
        val primitives = json["meshes"]!!.jsonArray[0].jsonObject["primitives"]!!.jsonArray
        assertFalse(primitives[0].jsonObject["attributes"]!!.jsonObject.containsKey("COLOR_0"))
        assertEquals("2", primitives[2].jsonObject["material"]!!.jsonPrimitive.content)
    }

    @Test
    fun filamentModelKeepsOnlyMorphTargetAttributesCgltfCanParse() {
        val json = vrmJson
            .replace(
                "\"targets\": [{\"POSITION\": 0}, {\"POSITION\": 0}]",
                "\"targets\": [{\"POSITION\": 0, \"extra\": {\"name\": \"a\"}, \"TEXCOORD_0\": -1}, " +
                    "{\"NORMAL\": 0, \"JOINTS_0\": -1}]"
            )
            .replace("{\"attributes\": {\"POSITION\": 0}, \"material\": 1}",
                     "{\"attributes\": {\"POSITION\": 0}, \"material\": 1, \"targets\": []}")
        val loader = VRMSceneLoader(withData = glb(json))
        loader.loadScene(withFilament = false)
        val model = loader.filamentModel()
        assertEquals(listOf(2), model.meshTargetCounts.toList())
        val primitives = parseGlbJson(model.glb)["meshes"]!!.jsonArray[0].jsonObject["primitives"]!!.jsonArray
        assertEquals(
            listOf(mapOf("POSITION" to "0"), mapOf("NORMAL" to "0")),
            primitives[0].jsonObject["targets"]!!.jsonArray.map { target ->
                target.jsonObject.mapValues { it.value.jsonPrimitive.content }
            },
        )
        assertNull(primitives[1].jsonObject["targets"])
    }

    @Test
    fun filamentModelGivesSisterPrimitivesZeroMorphTargets() {
        val json = vrmJson.replace(
            "\"materials\": [",
            "\"accessors\": [{\"bufferView\": 0, \"componentType\": 5126, \"count\": 3, \"type\": \"VEC3\"}], " +
                "\"bufferViews\": [{\"buffer\": 0, \"byteLength\": 6}], \"buffers\": [{\"byteLength\": 6}], " +
                "\"materials\": ["
        )
        val loader = VRMSceneLoader(withData = glb(json, ByteArray(6)))
        loader.loadScene(withFilament = false)
        val model = loader.filamentModel()
        val gltf = parseGlbJson(model.glb)
        val primitives = gltf["meshes"]!!.jsonArray[0].jsonObject["primitives"]!!.jsonArray
        for (index in 1..2) {
            assertEquals(
                listOf("1", "1"),
                primitives[index].jsonObject["targets"]!!.jsonArray.map { it.jsonObject["POSITION"]!!.jsonPrimitive.content },
            )
        }
        val zero = gltf["accessors"]!!.jsonArray[1].jsonObject
        assertEquals("3", zero["count"]!!.jsonPrimitive.content)
        assertEquals("1", zero["bufferView"]!!.jsonPrimitive.content)
        val view = gltf["bufferViews"]!!.jsonArray[1].jsonObject
        assertEquals("8", view["byteOffset"]!!.jsonPrimitive.content)
        assertEquals("36", view["byteLength"]!!.jsonPrimitive.content)
        assertEquals("44", gltf["buffers"]!!.jsonArray[0].jsonObject["byteLength"]!!.jsonPrimitive.content)
        val buffer = ByteBuffer.wrap(model.glb).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(44, buffer.getInt(20 + buffer.getInt(12)))
    }

    @Test
    fun filamentModelDeclaresTheWholeBinaryChunk() {
        val json = vrmJson.replace("\"materials\": [", "\"buffers\": [{\"byteLength\": 4}], \"materials\": [")
        val loader = VRMSceneLoader(withData = glb(json, ByteArray(16)))
        loader.loadScene(withFilament = false)
        val buffers = parseGlbJson(loader.filamentModel().glb)["buffers"]!!.jsonArray
        assertEquals("16", buffers[0].jsonObject["byteLength"]!!.jsonPrimitive.content)
    }

    @Test
    fun filamentModelDropsNodeExtensionsAndRejectsSkinsFilamentCannotRender() {
        fun model(jointCount: Int): VrmFilamentModel {
            val joints = List(jointCount) { 1 + it % 5 }.joinToString(", ")
            val json = vrmJson
                .replace("{\"name\": \"Body\", \"mesh\": 0}", "{\"name\": \"Body\", \"mesh\": 0, \"skin\": 0}")
                .replace(
                    "{\"name\": \"Neck\", \"translation\": [0, 0.3, 0], \"rotation\": [0, 0, 0, 1]}",
                    "{\"name\": \"Neck\", \"translation\": [0, 0.3, 0], \"rotation\": [0, 0, 0, 1], " +
                        "\"extensions\": {\"KHR_lights_punctual\": {\"light\": 0}}}"
                )
                .replace("\"materials\": [", "\"skins\": [{\"joints\": [$joints]}], \"materials\": [")
            val loader = VRMSceneLoader(withData = glb(json))
            loader.loadScene(withFilament = false)
            return loader.filamentModel()
        }
        val small = model(jointCount = 5)
        assertEquals(5, small.largestSkin)
        val nodes = parseGlbJson(small.glb)["nodes"]!!.jsonArray
        assertEquals("moblin:3", nodes[3].jsonObject["name"]!!.jsonPrimitive.content)
        assertNull(nodes[3].jsonObject["extensions"])
        assertEquals("0", nodes[6].jsonObject["skin"]!!.jsonPrimitive.content)
        val large = model(jointCount = 257)
        assertEquals(257, large.largestSkin)
        val scene = VRMSceneLoader(withData = glb(vrmJson)).loadScene(withFilament = false)
        assertNull(FilamentVrmAsset.load(scene, large))
    }

    @Test
    fun vrm1FilesAreRejectedLikeVrmKit() {
        val json = """{"asset": {"version": "2.0"}, "extensions": {"VRMC_vrm": {}}}"""
        val error = assertFailsWith<VRMError> { VRMSceneLoader(withData = glb(json)) }
        assertEquals("keyNotFound(VRM)", error.message)
        assertFailsWith<VRMError> { VRMSceneLoader(withData = ByteArray(8)) }
    }

    @Test
    fun missingHumanoidFieldsAreRejected() {
        val json = vrmJson.replace("\"armStretch\": 0.05, ", "")
        assertFailsWith<VRMError> { VRMSceneLoader(withData = glb(json)) }
    }

    @Test
    fun emptySpringBoneGroupDisablesAllSpringBones() {
        val json = vrmJson.replace(
            "\"boneGroups\": [",
            "\"boneGroups\": [{\"bones\": [], \"center\": -1, \"colliderGroups\": [], \"dragForce\": 0.4, " +
                "\"gravityDir\": {\"x\": 0, \"y\": -1, \"z\": 0}, \"gravityPower\": 1, \"hitRadius\": 0.02, " +
                "\"stiffiness\": 1},"
        )
        val scene = VRMSceneLoader(withData = glb(json)).loadScene(withFilament = false)
        val tip = scene.gltfNodes[5]!!
        val before = tip.worldPosition
        for (i in 0..30) {
            scene.vrmNode.update(at = 10.0 + i / 30.0)
        }
        assertClose(before, tip.worldPosition)
    }
}
