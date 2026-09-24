package com.moblin.android.platform.live2d

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Assume
import org.junit.Test

class Live2DReferenceTest {
    private val root = File(System.getenv("MOBLIN_LIVE2D_REFERENCE") ?: "build/live2d-reference")

    private fun fnv(values: IntArray): Long {
        var hash = 1469598103934665603uL
        for (value in values) {
            hash = (hash xor value.toULong().and(0xffffffffuL)) * 1099511628211uL
        }
        return hash.toLong()
    }

    private fun compare(name: String) {
        val directory = File(root, name)
        val referenceFile = File(directory, "reference.bin")
        Assume.assumeTrue("No reference for $name", referenceFile.isFile)
        val reference = ByteBuffer.wrap(referenceFile.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        val model = AyagamiModel(File(directory, "$name.model3.json"))
        assertEquals(reference.float, model.canvas.scale)
        assertEquals(reference.float, model.canvas.center.x)
        assertEquals(reference.float, model.canvas.center.y)
        assertEquals(reference.float, model.canvas.dimensions.x)
        assertEquals(reference.float, model.canvas.dimensions.y)
        val count = reference.int
        assertEquals(count, model.artMeshCount)
        for (uid in 0 until count) {
            val info = assertNotNull(model.artMeshInfo(uid))
            assertEquals(reference.int, info.textureIndex, "texture $uid")
            assertEquals(reference.int, info.vertexCount, "vertexCount $uid")
            assertEquals(reference.int, info.texcoordOffset, "texcoordOffset $uid")
            assertEquals(reference.int, info.indexRange.first, "index start $uid")
            assertEquals(reference.int, info.indexRange.last + 1, "index end $uid")
            assertEquals(reference.int, info.blendMode.ordinal, "blend $uid")
            assertEquals(reference.int == 1, info.culling, "culling $uid")
            assertEquals(reference.int == 1, info.invertMask, "invert $uid")
            val clipCount = reference.int
            val clips = List(clipCount) { reference.int }
            assertEquals(clips, info.clips, "clips $uid")
        }
        assertEquals(reference.int, model.indices.size)
        assertEquals(reference.long, fnv(IntArray(model.indices.size) { model.indices[it].toInt() and 0xffff }))
        assertEquals(reference.int, model.texcoords.size / 2)
        assertEquals(reference.long, fnv(IntArray(model.texcoords.size) { model.texcoords[it].toRawBits() }))
        for (parameter in listOf(
            "ParamAngleX", "ParamAngleZ", "ParamBodyAngleX", "ParamMouthOpenY", "ParamEyeLOpen",
            "ParamEyeROpen", "ParamBreath", "NoSuchParam",
        )) {
            assertEquals(reference.int == 1, model.hasParameter(parameter), parameter)
        }
        val frames = reference.int
        var time = 0.0
        var maximumError = 0.0
        var mismatches = 0
        for (frame in 0 until frames) {
            val dt = if (frame % 7 == 3) 0.25 else if (frame % 5 == 1) 0.0 else 1.0 / 30.0
            time += dt
            val side = sin(time * 1.3) * 0.9
            val rotation = sin(time * 0.7) * 0.4
            val mouth = maxOf(sin(time * 2.1) * 0.5 + 0.5, 0.0)
            val eyeLeft = if (frame % 11 < 2) 0.0 else 1.0
            val eyeRight = if (frame % 13 < 3) 0.2 else 1.0
            val angleX = side * 30.0
            model.setParameter("ParamAngleX", angleX.toFloat())
            model.setParameter("ParamAngleZ", (-Math.toDegrees(rotation)).toFloat())
            model.setParameter("ParamBodyAngleX", (angleX / 3.0).toFloat())
            model.setParameter("ParamMouthOpenY", mouth.toFloat())
            model.setParameter("ParamEyeLOpen", eyeLeft.toFloat())
            model.setParameter("ParamEyeROpen", eyeRight.toFloat())
            model.setParameter("ParamBreath", (0.5 - cos(time / 2.0 * PI) / 2.0).toFloat())
            model.update(deltaTime = dt.toFloat())
            val orderCount = reference.int
            val order = List(orderCount) { reference.int }
            assertEquals(order, model.drawOrder(), "$name draw order frame $frame")
            for (uid in 0 until count) {
                val present = reference.int == 1
                val state = model.artMeshState(uid)
                assertEquals(present, state != null, "$name state $uid frame $frame")
                if (!present || state == null) {
                    continue
                }
                assertEquals(reference.int == 1, state.visible, "$name visible $uid frame $frame")
                val expected = FloatArray(7) { reference.float }
                val actual = floatArrayOf(
                    state.opacity,
                    state.multiplyColor.x,
                    state.multiplyColor.y,
                    state.multiplyColor.z,
                    state.screenColor.x,
                    state.screenColor.y,
                    state.screenColor.z,
                )
                for (i in 0 until 7) {
                    val error = abs(expected[i] - actual[i]).toDouble()
                    maximumError = max(maximumError, error)
                    if (error > 1e-4) {
                        mismatches += 1
                    }
                }
                val vertexCount = reference.int
                assertEquals(vertexCount, state.vertices.size / 2, "$name vertex count $uid frame $frame")
                for (i in 0 until 2 * vertexCount) {
                    val value = reference.float
                    val error = abs(value - state.vertices[i]).toDouble() / max(1.0, abs(value).toDouble())
                    maximumError = max(maximumError, error)
                    if (error > 1e-3) {
                        mismatches += 1
                    }
                }
            }
        }
        println("$name: maximum relative error $maximumError, $mismatches mismatches over $frames frames")
        assertEquals(0, mismatches, "$name mismatches (maximum error $maximumError)")
        assertTrue(!reference.hasRemaining())
    }

    @Test
    fun haru() = compare("Haru")

    @Test
    fun hiyori() = compare("Hiyori")

    @Test
    fun mao() = compare("Mao")

    @Test
    fun mark() = compare("Mark")

    @Test
    fun natori() = compare("Natori")

    @Test
    fun ren() = compare("Ren")

    @Test
    fun rice() = compare("Rice")

    @Test
    fun wanko() = compare("Wanko")
}
