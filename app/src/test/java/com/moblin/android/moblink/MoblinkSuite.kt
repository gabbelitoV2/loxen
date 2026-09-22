package com.moblin.android.moblink

import kotlin.test.assertEquals
import kotlin.test.fail
import org.junit.Test

class MoblinkSuite {
    @Test
    fun decodeStatusResponse() {
        val message = MoblinkMessageToStreamer.fromJson(
            """
            {"response":{"id":5,"data":{"status":{"thermalState":"white","batteryPercentage":30,"temperature":39}},"result":{"ok":{}}}}
            """.trimIndent(),
        )
        when (message) {
            is MoblinkMessageToStreamer.Response -> {
                assertEquals(5, message.id)
                assertEquals("ok", message.result.rawValue)
                when (val data = message.data) {
                    is MoblinkResponse.Status -> {
                        assertEquals(30, data.batteryPercentage)
                        assertEquals("white", data.thermalState?.rawValue)
                    }
                    else -> fail("Expected status")
                }
            }
            else -> fail("Expected response")
        }
    }
}
