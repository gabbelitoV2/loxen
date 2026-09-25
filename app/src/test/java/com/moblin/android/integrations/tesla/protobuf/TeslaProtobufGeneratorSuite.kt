package com.moblin.android.integrations.tesla.protobuf

import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TeslaProtobufGeneratorSuite {
    private fun python(): String? = listOf("python", "python3").firstOrNull { command ->
        runCatching {
            val process = ProcessBuilder(command, "--version").redirectErrorStream(true).start()
            process.inputStream.readBytes()
            process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0
        }.getOrDefault(false)
    }

    @Test
    fun generatorCheckIsClean() {
        val root = generateSequence(File("").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "tools/pbswift.py").isFile }
            ?: return assumeTrue("tools/pbswift.py not found", false)
        val python = python() ?: return assumeTrue("python not found", false)
        val process = ProcessBuilder(python, "tools/pbswift.py", "--check")
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        assertTrue(process.waitFor(120, TimeUnit.SECONDS))
        assumeFalse("no upstream checkout: $output", "directory not found" in output)
        assertEquals(0, process.exitValue(), output)
        assertTrue("up to date" in output, output)
    }
}
