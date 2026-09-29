package com.moblin.android.remotecontrol

import com.moblin.android.platform.Bundle
import com.moblin.android.various.utils.loadResource
import java.security.MessageDigest
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteControlWebAssetsSuite {
    private val staticFiles = listOf(
        "favicon" to "ico",
        "golf" to "html",
        "index" to "html",
        "recordings" to "html",
        "remote" to "html",
        "scoreboard" to "html",
        "volleyball" to "png",
        "app" to "css",
        "common" to "css",
        "components" to "css",
        "golf" to "css",
        "recordings" to "css",
        "remote" to "css",
        "scoreboard" to "css",
        "app" to "mjs",
        "golf" to "mjs",
        "index" to "mjs",
        "components" to "mjs",
        "recordings" to "mjs",
        "remote" to "mjs",
        "scoreboard" to "mjs",
        "utils" to "mjs",
        "vendor" to "mjs",
    )

    @Test
    fun everyStaticFileOfTheWebRemoteControlIsBundled() {
        for ((name, ext) in staticFiles) {
            val bytes = assertNotNull(Bundle.readBytes(name, ext), "$name.$ext")
            assertTrue(bytes.isNotEmpty(), "$name.$ext")
        }
    }

    @Test
    fun theRootPageLoadsItsModulesAndStyles() {
        val index = loadResource(name = "index", ext = "html").toString(Charsets.UTF_8)
        assertTrue("/js/index.mjs" in index)
        assertTrue("/css/components.css" in index)
    }

    @Test
    fun theFaviconIsLoxensAndNotMoblins() {
        val favicon = assertNotNull(Bundle.readBytes("favicon", "ico"))
        assertTrue(favicon.copyOfRange(0, 4).contentEquals(byteArrayOf(0, 0, 1, 0)))
        val sha256 = MessageDigest.getInstance("SHA-256").digest(favicon).joinToString("") { "%02x".format(it) }
        assertNotEquals("7542f3732c1a218aaa0ed08d7c2de93e497cfeab52896ed69a0f8571e83ae642", sha256)
    }
}
