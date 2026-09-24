package com.moblin.android.platform.scenekit

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assume
import org.junit.Test

class VrmSampleTest {
    private val root = File(System.getenv("MOBLIN_VRM_SAMPLES") ?: "build/vrm-samples")

    private fun check(name: String) {
        val file = File(root, name)
        Assume.assumeTrue("No sample $name", file.isFile)
        val loader = VRMSceneLoader(withURL = file)
        val scene = loader.loadScene(withFilament = false)
        val node = scene.vrmNode
        for (bone in listOf(Humanoid.Bones.neck, Humanoid.Bones.spine, Humanoid.Bones.leftUpperArm, Humanoid.Bones.rightUpperArm)) {
            assertNotNull(node.humanoid.node(bone), "$name $bone")
        }
        assertTrue(node.blendShapeClips.containsKey(BlendShapeKey.preset(BlendShapePreset.a)), name)
        assertTrue(node.blendShapeClips.containsKey(BlendShapeKey.preset(BlendShapePreset.blink)), name)
        node.setBlendShape(value = 0.8, `for` = BlendShapeKey.preset(BlendShapePreset.a))
        assertTrue(node.blendShape(`for` = BlendShapeKey.preset(BlendShapePreset.a)) > 0.0, name)
        node.humanoid.node(Humanoid.Bones.neck)?.eulerAngles = SCNVector3(0.0, -0.3, -0.2)
        val positions = scene.gltfNodes.filterNotNull()
        for (i in 0..60) {
            node.update(at = 100.0 + i / 30.0)
        }
        for (candidate in positions) {
            val position = candidate.worldPosition
            assertTrue(position.x.isFinite() && position.y.isFinite() && position.z.isFinite(), "$name ${candidate.name}")
        }
        val model = loader.filamentModel()
        val buffer = ByteBuffer.wrap(model.glb).order(ByteOrder.LITTLE_ENDIAN)
        val json = Json.parseToJsonElement(String(model.glb, 20, buffer.getInt(12))).jsonObject
        assertEquals(scene.gltfNodes.size, json["nodes"]!!.jsonArray.size)
        val binaryOffset = 20 + buffer.getInt(12)
        assertEquals(0x004E4942, buffer.getInt(binaryOffset + 4))
        println("$name: ${scene.gltfNodes.size} nodes, ${model.meshCount} meshes, ${node.blendShapeClips.size} clips, glb ${model.glb.size} bytes")
    }

    @Test
    fun aliciaSolid() = check("AliciaSolid.vrm")

    @Test
    fun seedSanIsVrm1AndRejectedLikeVrmKit() {
        val file = File(root, "Seed-san.vrm")
        Assume.assumeTrue("No sample Seed-san.vrm", file.isFile)
        val error = kotlin.runCatching { VRMSceneLoader(withURL = file) }.exceptionOrNull()
        assertEquals("keyNotFound(VRM)", error?.message)
    }
}
