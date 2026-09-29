package com.moblin.android.platform.codable

import com.moblin.android.remotecontrol.RemoteControlMessageToAssistant
import com.moblin.android.remotecontrol.RemoteControlMessageToStreamer
import com.moblin.android.remotecontrol.RemoteControlResult
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class SwiftCaseSuite {
    @Test
    fun casesWithoutAssociatedValuesEncodeAsAnObjectWithAnEmptyObject() {
        assertEquals("""{"ok":{}}""", swiftCase("ok").toString())
    }

    @Test
    fun casesDecodeFromTheSwiftObjectAndFromAPlainString() {
        assertEquals(RemoteControlResult.WrongPassword, swiftCase(Json.parseToJsonElement("""{"wrongPassword":{}}"""), RemoteControlResult::fromName))
        assertEquals(RemoteControlResult.Ok, swiftCase(JsonPrimitive("ok"), RemoteControlResult::fromName))
        assertFailsWith<SerializationException> {
            swiftCase(Json.parseToJsonElement("""{"maybe":{}}"""), RemoteControlResult::fromName)
        }
    }

    @Test
    fun theStreamerUnderstandsTheIdentifiedMessageOfMoblinsAssistant() {
        val message = RemoteControlMessageToStreamer.fromJson("""{"identified":{"result":{"ok":{}}}}""")
        assertEquals(RemoteControlMessageToStreamer.Identified(RemoteControlResult.Ok), message)
    }

    @Test
    fun responsesCarryTheResultTheWayMoblinWritesIt() {
        val json = RemoteControlMessageToAssistant.Response(id = 7, result = RemoteControlResult.Ok).toJsonElement()
        assertEquals("""{"ok":{}}""", json.getValue("response").jsonObject.getValue("result").toString())
        val identified = RemoteControlMessageToStreamer.Identified(RemoteControlResult.WrongPassword).toJsonElement()
        assertEquals("""{"wrongPassword":{}}""", identified.getValue("identified").jsonObject.getValue("result").toString())
    }
}
