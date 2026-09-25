package com.moblin.android.platform.corebluetooth

import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class CBUUIDSuite {
    @Test
    fun sixteenBitUuidsEqualTheirFullFormLikeApple() {
        val short = CBUUID(string = "180d")
        val full = CBUUID(string = "0000180D-0000-1000-8000-00805F9B34FB")
        assertEquals("180D", short.uuidString)
        assertEquals("180D", full.uuidString)
        assertEquals(short, full)
        assertEquals(short.hashCode(), full.hashCode())
        assertEquals(short, CBUUID(nsuuid = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")))
        assertContentEquals(byteArrayOf(0x18, 0x0D), short.data)
        assertEquals(short, CBUUID(data = byteArrayOf(0x18, 0x0D)))
        assertEquals("180D", short.toString())
    }

    @Test
    fun thirtyTwoBitUuidsKeepEightDigits() {
        val uuid = CBUUID(string = "1234abcd")
        assertEquals("1234ABCD", uuid.uuidString)
        assertEquals(uuid, CBUUID(string = "1234ABCD-0000-1000-8000-00805F9B34FB"))
        assertContentEquals(byteArrayOf(0x12, 0x34, 0xAB.toByte(), 0xCD.toByte()), uuid.data)
        assertEquals(uuid, CBUUID(data = uuid.data))
        assertEquals(CBUUID(string = "FFF5"), CBUUID(data = byteArrayOf(0, 0, 0xFF.toByte(), 0xF5.toByte())))
    }

    @Test
    fun customUuidsAreUppercaseAndCompareByValue() {
        val text = "b5f90072-aa8d-11e3-9046-0002a5d5c51b"
        val uuid = CBUUID(string = text)
        assertEquals(text.uppercase(), uuid.uuidString)
        assertEquals(uuid, CBUUID(string = text.uppercase()))
        assertEquals(uuid, CBUUID(nsuuid = UUID.fromString(text)))
        assertEquals(16, uuid.data.size)
        assertEquals(uuid, CBUUID(data = uuid.data))
        assertNotEquals(uuid, CBUUID(string = "B5F90073-AA8D-11E3-9046-0002A5D5C51B"))
        assertNotEquals(CBUUID(string = "180D"), CBUUID(string = "180F"))
    }

    @Test
    fun uuidsWorkAsSetMembersAndMapKeysLikeSwiftHashable() {
        val notifyIds = setOf(CBUUID(string = "FFF4"), CBUUID(string = "b5f90073-aa8d-11e3-9046-0002a5d5c51b"))
        assertTrue(CBUUID(nsuuid = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")) in notifyIds)
        assertTrue(CBUUID(string = "B5F90073-AA8D-11E3-9046-0002A5D5C51B") in notifyIds)
        assertFalse(CBUUID(string = "FFF5") in notifyIds)
        val characteristics = mapOf(CBUUID(string = "2A37") to "measurement")
        assertEquals("measurement", characteristics[CBUUID(string = "00002a37-0000-1000-8000-00805f9b34fb")])
        val matched = when (CBUUID(string = "2a37")) {
            CBUUID(string = "2A38") -> "location"
            CBUUID(string = "2A37") -> "measurement"
            else -> "other"
        }
        assertEquals("measurement", matched)
    }

    @Test
    fun invalidUuidsAreRejected() {
        for (text in listOf("", "18D", "180G", "12345", "0000180D00001000800000805F9B34FB", "b5f90072-aa8d-11e3-9046-0002a5d5c51")) {
            assertFailsWith<IllegalArgumentException>(text) { CBUUID(string = text) }
        }
        assertFailsWith<IllegalArgumentException> { CBUUID(data = byteArrayOf(1, 2, 3)) }
    }

    @Test
    fun characteristicPropertiesAreAnOptionSet() {
        val properties = CBCharacteristicProperties.notify + CBCharacteristicProperties.indicate
        assertTrue(properties.contains(CBCharacteristicProperties.notify))
        assertTrue(properties.contains(CBCharacteristicProperties.indicate))
        assertFalse(properties.contains(CBCharacteristicProperties.write))
        assertTrue(properties.contains(CBCharacteristicProperties(0)))
        assertEquals(0x30, properties.rawValue)
        assertEquals(CBCharacteristicProperties.notify, properties.intersection(CBCharacteristicProperties.notify))
        assertTrue(CBCharacteristicProperties(0).isEmpty)
        assertEquals(properties, CBCharacteristicProperties.notify.union(CBCharacteristicProperties.indicate))
    }

    @Test
    fun gattStatusesBecomeCoreBluetoothErrors() {
        assertNull(gattError(0))
        assertEquals(CBATTError.Code.insufficientAuthentication, assertIs<CBATTError>(gattError(5)).code)
        assertEquals(CBATTError.Code.insufficientEncryption, assertIs<CBATTError>(gattError(0x0F)).code)
        assertEquals(CBError.Code.unknown, assertIs<CBError>(gattError(133)).code)
        assertEquals(CBError.Code.connectionTimeout, assertIs<CBError>(disconnectionError(0x08)).code)
        assertEquals(CBError.Code.peripheralDisconnected, assertIs<CBError>(disconnectionError(0x13)).code)
    }

    @Test
    fun descriptorValuesHaveTheTypesCoreBluetoothUses() {
        assertEquals(1, descriptorValue(CBUUID(string = "2902"), byteArrayOf(1, 0)))
        assertEquals("Heart", descriptorValue(CBUUID(string = "2901"), "Heart".toByteArray()))
        assertContentEquals(byteArrayOf(7), descriptorValue(CBUUID(string = "2904"), byteArrayOf(7)) as ByteArray)
    }
}
