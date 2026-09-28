package com.moblin.android.platform.loxen

import com.moblin.android.various.MoblinSettingsUrl
import java.io.File
import java.net.URLDecoder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.junit.Test

class LoxenReadmeSuite {
    private val readme: String by lazy {
        listOf(File("../README.md"), File("README.md")).first { it.isFile }.readText().replace("\r\n", "\n")
    }

    private fun examples(): List<String> = readme.lines().filter { it.startsWith("loxen://?") }

    private fun jsonBlocks(): List<String> =
        Regex("```json\n(.*?)\n```", RegexOption.DOT_MATCHES_ALL).findAll(readme).map { it.groupValues[1] }.toList()

    private fun decoded(url: String): String = URLDecoder.decode(url.substringAfter("?"), "UTF-8")

    @Test
    fun everyCustomUrlExampleImportsInLoxen() {
        val examples = examples()
        assertEquals(3, examples.size)
        val settings = examples.map { MoblinSettingsUrl.fromString(decoded(it)) }
        val stream = assertNotNull(settings[0].streams).single()
        assertEquals("BELABOX UK", stream.name)
        assertEquals("H.265/HEVC", assertNotNull(stream.video?.codec).rawValue)
        assertEquals("foobar", stream.obs?.webSocketPassword)
        assertEquals(listOf("Mute", "Draw"), assertNotNull(settings[1].quickButtons?.buttons).map { it.type.rawValue })
        assertEquals("https://example.com", settings[2].webBrowser?.home)
        assertEquals("ws://192.168.1.10:2345", settings[2].remoteControl?.streamer?.url)
    }

    @Test
    fun theDecodedExamplesAreThePrettyPrintedBlobs() {
        val examples = examples().map { Json.parseToJsonElement(decoded(it)) }
        val blocks = jsonBlocks().map { Json.parseToJsonElement(it) }
        assertEquals(examples, blocks)
    }

    @Test
    fun theReadmeCreditsMoblin() {
        assertTrue(Loxen.attribution in readme.replace("\n", " "))
        assertTrue("## Import settings using loxen:// or moblin:// (custom URL)" in readme)
    }
}
