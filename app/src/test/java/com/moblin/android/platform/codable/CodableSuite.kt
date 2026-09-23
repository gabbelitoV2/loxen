package com.moblin.android.platform.codable

import java.time.Instant
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

private enum class CodableSuiteMode { first, second }

@Serializable(with = CodableSuiteChild.Serializer::class)
private class CodableSuiteChild {
    var name = "child"
    val enabled = MutableStateFlow(true)

    fun encode(): JsonObject = encodeContainer {
        encode("name", name)
        encode("enabled", enabled)
    }

    companion object {
        fun decode(container: JsonObject): CodableSuiteChild {
            val child = CodableSuiteChild()
            child.name = container.decode("name", "child")
            child.enabled.value = container.decode("enabled", true)
            return child
        }
    }

    object Serializer : kotlinx.serialization.KSerializer<CodableSuiteChild> by JsonObjectSerializer(
        "CodableSuiteChild",
        { it.encode() },
        { decode(it) },
    )
}

class CodableSuite {
    @Test
    fun uuidIsUppercase() {
        val id = UUID.fromString("e621e1f8-c36c-495a-93fc-0c247a3e6e5f")
        val json = codableJson.encodeToString(UUIDSerializer, id)
        assertEquals("\"E621E1F8-C36C-495A-93FC-0C247A3E6E5F\"", json)
        assertEquals(id, codableJson.decodeFromString(UUIDSerializer, json))
    }

    @Test
    fun dateIsSecondsSince2001() {
        val date = Instant.ofEpochSecond(978_307_200L + 10L, 500_000_000L)
        val json = codableJson.encodeToString(AppleDateSerializer, date)
        assertEquals("10.5", json)
        assertEquals(date, codableJson.decodeFromString(AppleDateSerializer, json))
    }

    @Test
    fun dataIsBase64() {
        val json = codableJson.encodeToString(DataSerializer, byteArrayOf(1, 2, 3))
        assertEquals("\"AQID\"", json)
        assertContentEquals(byteArrayOf(1, 2, 3), codableJson.decodeFromString(DataSerializer, json))
    }

    @Test
    fun containerRoundTripAndDefaults() {
        val child = CodableSuiteChild()
        child.name = "a/b"
        child.enabled.value = false
        val json = codableJson.encodeToString(CodableSuiteChild.Serializer, child)
        assertEquals("{\"name\":\"a/b\",\"enabled\":false}", json)
        val decoded = codableJson.decodeFromString(CodableSuiteChild.Serializer, json)
        assertEquals("a/b", decoded.name)
        assertEquals(false, decoded.enabled.value)
        val fallback = codableJson.decodeFromString(CodableSuiteChild.Serializer, "{\"name\":5,\"enabled\":\"x\"}")
        assertEquals("child", fallback.name)
        assertEquals(true, fallback.enabled.value)
    }

    @Test
    fun reifiedDecodeOfContextualAndLists() {
        val id = UUID.randomUUID()
        val container = encodeContainer {
            encode("id", id)
            encode("ids", listOf(id))
            encodeIfPresent<String>("missing", null)
            encode("children", listOf(CodableSuiteChild()), ListSerializer(CodableSuiteChild.Serializer))
        }
        assertEquals(id, container.decode("id", UUID.randomUUID()))
        assertEquals(listOf(id), container.decode("ids", emptyList<UUID>()))
        assertEquals(null, container.decodeIfPresent<String>("missing"))
        assertEquals(1, container.decode("children", ListSerializer(CodableSuiteChild.Serializer), emptyList()).size)
        assertEquals(CodableSuiteMode.first, container.decode("mode", CodableSuiteMode.first))
        assertEquals(3, codableJson.parseToJsonElement(container.toString()).jsonObject.size)
    }

    @Test
    fun quotedPrimitivesFallBackLikeSwift() {
        val container = codableJson.parseToJsonElement(
            "{\"a\":\"5\",\"b\":\"true\",\"c\":5,\"d\":\"1-1-1-1-1\",\"e\":7,\"f\":true}",
        ).jsonObject
        assertEquals(1, container.decode("a", 1))
        assertEquals(false, container.decode("b", false))
        assertEquals("x", container.decode("c", "x"))
        val id = UUID.randomUUID()
        assertEquals(id, container.decode("d", id))
        assertEquals(7L, container.decode("e", 0L))
        assertEquals(true, container.decode("f", false))
        assertEquals(CodableSuiteMode.first, container.decode("e", CodableSuiteMode.first))
    }
}
