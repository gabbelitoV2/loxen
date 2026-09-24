package com.moblin.android.platform.live2d

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class Live2DParserTest {
    @Test
    fun sectionAndClassCountsMatchAyagami() {
        assertEquals(listOf(23, 23, 23, 32, 35, 38), Moc3Version.entries.map { moc3ClassCount(it) })
        assertEquals(listOf(101, 102, 102, 137, 152, 167), Moc3Version.entries.map { moc3SectionCount(it) })
    }

    @Test
    fun badMagicAndVersionAreRejected() {
        val badMagic = assertFailsWith<Moc3Exception> { parseMoc3("MOC4".toByteArray() + ByteArray(60)) }
        assertTrue(badMagic.message!!.startsWith("Invalid magic value"))
        val header = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
        header.put("MOC3".toByteArray())
        header.putInt(7)
        val badVersion = assertFailsWith<Moc3Exception> { parseMoc3(header.array()) }
        assertEquals("Unknown/unsupported version 7", badVersion.message)
    }

    @Test
    fun paddingBeforeSectionsMustBeZero() {
        val data = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        data.put("MOC3".toByteArray())
        data.putInt(1)
        data.put(8, 1)
        val error = assertFailsWith<Moc3Exception> { parseMoc3(data.array()) }
        assertTrue(error.message!!.startsWith("Invalid padding"), error.message)
    }

    @Test
    fun model3RequiresMocAndTextures() {
        val references = parseModel3(
            """{"Version": 3, "FileReferences": {"Moc": "a.moc3", "Textures": ["t/0.png"], "Physics": null}}""".toByteArray()
        )
        assertEquals("a.moc3", references.moc)
        assertEquals(listOf("t/0.png"), references.textures)
        assertNull(references.physics)
        assertFailsWith<Exception> { parseModel3("""{"Version": 3, "FileReferences": {"Moc": "a.moc3"}}""".toByteArray()) }
        assertFailsWith<Exception> {
            parseModel3("""{"Version": 3.5, "FileReferences": {"Moc": "a", "Textures": []}}""".toByteArray())
        }
    }

    private val physics = """
        {
          "Version": 3,
          "Meta": {
            "PhysicsSettingCount": 1,
            "EffectiveForces": {"Gravity": {"X": 0, "Y": -1}, "Wind": {"X": 0, "Y": 0}},
            "PhysicsDictionary": [{"Id": "PhysicsSetting1", "Name": "Hair"}]
          },
          "PhysicsSettings": [
            {
              "Id": "PhysicsSetting1",
              "Input": [
                {"Source": {"Target": "Parameter", "Id": "ParamAngleX"}, "Weight": 60, "Type": "X", "Reflect": false}
              ],
              "Output": [
                {"Destination": {"Target": "Parameter", "Id": "ParamHair"}, "VertexIndex": 1, "Scale": 1.5,
                 "Weight": 100, "Type": "Angle", "Reflect": false},
                {"Destination": {"Target": "Parameter", "Id": "ParamHair"}, "VertexIndex": 3, "Scale": 1,
                 "Weight": 100, "Type": "Angle", "Reflect": false}
              ],
              "Vertices": [
                {"Position": {"X": 0, "Y": 0}, "Mobility": 1, "Delay": 1, "Acceleration": 1, "Radius": 0},
                {"Position": {"X": 0, "Y": 10}, "Mobility": 0.95, "Delay": 0.9, "Acceleration": 1.5, "Radius": 10}
              ],
              "Normalization": {
                "Position": {"Minimum": -10, "Default": 0, "Maximum": 10},
                "Angle": {"Minimum": -10, "Default": 0, "Maximum": 10}
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun physics3IsParsedStrictly() {
        val config = parsePhysics3(physics.toByteArray())
        assertNull(config.fps)
        assertEquals(-1f, config.gravityY)
        assertEquals(1, config.settings.size)
        assertEquals(2, config.settings[0].outputs.size)
        assertFailsWith<Exception> { parsePhysics3(physics.replace("\"Type\": \"X\"", "\"Type\": \"Y\"").toByteArray()) }
        assertFailsWith<Exception> {
            parsePhysics3(physics.replace("\"VertexIndex\": 1,", "\"VertexIndex\": 1.5,").toByteArray())
        }
        assertFailsWith<Exception> {
            parsePhysics3(physics.replace("\"Name\": \"Hair\"", "\"Label\": \"Hair\"").toByteArray())
        }
    }

    @Test
    fun missingFilesGiveAyagamiErrors() {
        val directory = Files.createTempDirectory("live2d").toFile()
        try {
            val model3 = File(directory, "m.model3.json")
            val missing = assertFailsWith<AyagamiError> { AyagamiModel(model3) }
            assertTrue(missing.message!!.startsWith("Failed to open model3.json"))
            model3.writeText("""{"Version": 3, "FileReferences": {"Moc": "m.moc3", "Textures": []}}""")
            val noMoc = assertFailsWith<AyagamiError> { AyagamiModel(model3) }
            assertTrue(noMoc.message!!.startsWith("Failed to open"), noMoc.message)
            File(directory, "m.moc3").writeBytes("MOC3".toByteArray() + ByteArray(4))
            val badMoc = assertFailsWith<AyagamiError> { AyagamiModel(model3) }
            assertTrue(badMoc.message!!.startsWith("Failed to parse moc3"), badMoc.message)
        } finally {
            directory.deleteRecursively()
        }
    }
}
