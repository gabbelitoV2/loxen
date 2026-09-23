package com.moblin.android.integrations.gopro

import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumResultGeneric
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseGetApEntries
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProLens
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun ByteArray.hexString(): String =
    joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }

@RunWith(RobolectricTestRunner::class)
class GoProBleProtocolSuite {
    @Test
    fun packetizesSinglePacketCommand() {
        val packets = goProBlePackets(payload = goProSetShutterMessage(on = true))
        assertEquals(1, packets.size)
        assertContentEquals(byteArrayOf(0x20, 0x03, 0x01, 0x01, 0x01), packets[0])
    }

    @Test
    fun packetizesAndAccumulatesLongMessage() {
        val message = ByteArray(64) { it.toByte() }
        val packets = goProBlePackets(payload = message)
        assertEquals(4, packets.size)
        assertEquals(20, packets[0].size)
        assertContentEquals(byteArrayOf(0x20, 0x40), packets[0].copyOfRange(0, 2))
        assertTrue(packets.drop(1).all { it.firstOrNull() == 0x80.toByte() })
        val accumulator = GoProBleMessageAccumulator()
        for (packet in packets.dropLast(1)) {
            assertNull(accumulator.append(packet = packet))
        }
        assertContentEquals(message, accumulator.append(packet = packets.last()))
    }

    @Test
    fun buildsPairingAndWifiCommands() {
        assertEquals("0301080012064d6f626c696e", goProPairingCompleteMessage().hexString())
        assertEquals(
            "02050a064d6f626c696e12067365637265745001",
            goProConnectToWifiMessage(ssid = "Moblin", password = "secret").hexString()
        )
    }

    @Test
    fun buildsScanCommands() {
        assertEquals("0202", goProStartScanMessage().hexString())
        assertEquals(
            "0203080010641803",
            goProGetApEntriesMessage(scanId = 3, startIndex = 0, maximumEntries = 100).hexString()
        )
        assertEquals(
            "02040a064d6f626c696e",
            goProConnectToProvisionedWifiMessage(ssid = "Moblin").hexString()
        )
    }

    @Test
    fun buildsLiveStreamModeCommand() {
        assertEquals(
            "f1790a0872746d703a2f2f611000180c38a00640f02e48f02e",
            goProSetLiveStreamModeMessage(
                url = "rtmp://a",
                resolution = SettingsGoProLaunchLiveStreamResolution.r1080p,
                bitrate = 6_000_000u,
                lens = SettingsGoProLens.auto
            )
                .hexString()
        )
        assertEquals(
            "f1790a0872746d703a2f2f611000180c38a00640f02e48f02e5004",
            goProSetLiveStreamModeMessage(
                url = "rtmp://a",
                resolution = SettingsGoProLaunchLiveStreamResolution.r1080p,
                bitrate = 6_000_000u,
                lens = SettingsGoProLens.linear
            )
                .hexString()
        )
    }

    @Test
    fun parsesScanEntries() {
        val response = OpenGopro_ResponseGetApEntries(
            serializedBytes = byteArrayOf(
                0x08, 0x01, 0x10, 0x03, 0x1A, 0x0E, 0x0A, 0x05, 0x4F, 0x74,
                0x68, 0x65, 0x72, 0x10, 0x02, 0x20, 0xBC.toByte(), 0x28, 0x28, 0x01,
                0x1A, 0x0F, 0x0A, 0x06, 0x4D, 0x6F, 0x62, 0x6C, 0x69, 0x6E,
                0x10, 0x03, 0x20, 0x85.toByte(), 0x13, 0x28, 0x03,
            ),
        )
        assertEquals(OpenGopro_EnumResultGeneric.resultSuccess, response.result)
        assertEquals(3, response.scanID)
        assertEquals(2, response.entries.size)
        assertEquals("Other", response.entries.firstOrNull()?.ssid)
        assertEquals(2, response.entries.firstOrNull()?.signalStrengthBars)
        assertEquals(5180, response.entries.firstOrNull()?.signalFrequencyMhz)
        assertEquals(false, response.entries.firstOrNull()?.isConfigured())
        assertEquals("Moblin", response.entries.lastOrNull()?.ssid)
        assertEquals(2437, response.entries.lastOrNull()?.signalFrequencyMhz)
        assertEquals(true, response.entries.lastOrNull()?.isConfigured())
        assertEquals(false, response.entries.lastOrNull()?.isUnsupportedType())
    }
}
