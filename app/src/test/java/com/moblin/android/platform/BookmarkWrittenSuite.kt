package com.moblin.android.platform

import java.io.File
import java.nio.file.Files
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookmarkWrittenSuite {
    private val directory: File = Files.createTempDirectory("bookmark-written").toFile()

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun aDownloadWaitsUntilTheWriterClosesTheFile() {
        val file = File(directory, "recording.mp4")
        val output = Bookmark.openOutput(file.path, file)!!
        output.write(ByteArray(10))
        thread {
            Thread.sleep(300)
            output.write(ByteArray(5))
            output.close()
        }
        val started = System.nanoTime()
        Bookmark.awaitWritten(File(file.path))
        val waitedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue(waitedMs in 250..2_000, "Waited $waitedMs ms")
        assertEquals(15, file.length())
    }

    @Test
    fun aFileThatIsNeverClosedIsWaitedForOnlyUntilTheTimeout() {
        val file = File(directory, "live.mp4")
        val output = Bookmark.openOutput(file.path, file)!!
        val started = System.nanoTime()
        Bookmark.awaitWritten(file, timeoutMs = 200)
        assertTrue((System.nanoTime() - started) / 1_000_000 < 1_000)
        output.close()
        val closed = System.nanoTime()
        Bookmark.awaitWritten(file)
        assertTrue((System.nanoTime() - closed) / 1_000_000 < 100)
    }
}
