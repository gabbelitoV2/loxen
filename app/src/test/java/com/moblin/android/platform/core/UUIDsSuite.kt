package com.moblin.android.platform.core

import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Test

class UUIDsSuite {
    private val directory: File = Files.createTempDirectory("uuids").toFile()
    private val id = UUID.fromString("01a0ef9a-4139-77e5-b439-168a47f0ae38")

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun uuidStringIsUpperCaseLikeSwift() {
        assertEquals("01A0EF9A-4139-77E5-B439-168A47F0AE38", id.uuidString)
    }

    @Test
    fun filesAreNamedLikeSwiftNamesThem() {
        assertEquals("01A0EF9A-4139-77E5-B439-168A47F0AE38", uuidFile(directory, id).name)
        assertEquals("01A0EF9A-4139-77E5-B439-168A47F0AE38.mp4", uuidFile(directory, id, ".mp4").name)
    }

    @Test
    fun aFileLoxenWroteInLowerCaseIsRenamed() {
        File(directory, "$id.mp4").writeText("media")
        val file = uuidFile(directory, id, ".mp4")
        assertEquals("media", file.readText())
        assertEquals(listOf("01A0EF9A-4139-77E5-B439-168A47F0AE38.mp4"), directory.list()!!.toList())
    }

    @Test
    fun onlyALeadingUuidIsUpperCased() {
        assertEquals("01A0EF9A-4139-77E5-B439-168A47F0AE38 0", upperCaseLeadingUuid("01a0ef9a-4139-77e5-b439-168a47f0ae38 0"))
        assertEquals("01A0EF9A-4139-77E5-B439-168A47F0AE38", upperCaseLeadingUuid("01a0ef9a-4139-77e5-b439-168a47f0ae38"))
        assertEquals("Built-in bottom 3", upperCaseLeadingUuid("Built-in bottom 3"))
        assertEquals("", upperCaseLeadingUuid(""))
    }

    @Test
    fun aFileFromIosIsLeftAsItIs() {
        File(directory, "01A0EF9A-4139-77E5-B439-168A47F0AE38").writeText("model")
        assertEquals("model", uuidFile(directory, id).readText())
        assertEquals(listOf("01A0EF9A-4139-77E5-B439-168A47F0AE38"), directory.list()!!.toList())
    }
}
