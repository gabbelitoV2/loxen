package com.moblin.android.platform.blacksharklib

import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import org.junit.Test

private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

private fun unsigned(data: ByteArray?, index: Int): Int? = data?.get(index)?.toInt()?.and(0xFF)

class BlackSharkLibSuite {
    @Test
    fun manufacturerDataIsRecognised() {
        val manufacturerData = bytes(0x8F, 0x03, 0x01, 0x00) + "BSMC4PROBR42".toByteArray()
        assertTrue(BlackSharkLib.isBlackSharkDevice(manufacturerData))
        assertTrue(BlackSharkLib.isBlackSharkDevice(bytes(0x8F, 0x03)))
    }

    @Test
    fun foreignManufacturerDataIsRejected() {
        assertFalse(BlackSharkLib.isBlackSharkDevice(bytes(0x4C, 0x00, 0x02)))
        assertFalse(BlackSharkLib.isBlackSharkDevice(bytes(0x8F)))
        assertFalse(BlackSharkLib.isBlackSharkDevice(ByteArray(0)))
    }

    @Test
    fun serviceAndCharacteristicIdsAreUnchanged() {
        assertEquals(UUID.fromString("0000A0A0-3C17-D293-8E48-14FE2E4DA212"), BlackSharkLib.getServiceUUID())
        assertContentEquals(bytes(0xA0, 0x02), BlackSharkLib.getReadCharacteristicsUUID())
        assertContentEquals(bytes(0xA0, 0x01), BlackSharkLib.getWriteCharacteristicsUUID())
    }

    @Test
    fun detectsModelFromAdvertisedName() {
        assertEquals(BlackSharkLib.Model.pro5, BlackSharkLib.detectModel(advertisedName = "Black Shark MagCooler 5pro"))
        assertEquals(BlackSharkLib.Model.pro4, BlackSharkLib.detectModel(advertisedName = "Black Shark MagCooler 4pro"))
        assertEquals(BlackSharkLib.Model.pro5, BlackSharkLib.detectModel(advertisedName = "BLACK SHARK MAGCOOLER 5PRO"))
        assertNull(BlackSharkLib.detectModel(advertisedName = null))
        assertNull(BlackSharkLib.detectModel(advertisedName = ""))
        assertNull(BlackSharkLib.detectModel())
        assertNull(BlackSharkLib.detectModel(advertisedName = "Black Shark MagCool"))
        assertNull(BlackSharkLib.detectModel(advertisedName = "Black Shark MagCooler 6pro"))
    }

    @Test
    fun parsesFourProCoolingState() {
        val frame = bytes(0x8A, 0x06, 0x00, 0x00, 0x01, 0x08, 0x00, 0x1C, 0x00, 0x00)
        val state = assertIs<BlackSharkLib.CoolingState>(BlackSharkLib.parseMessages(frame))
        assertEquals(BlackSharkLib.Model.pro4, state.model)
        assertEquals(8, state.phoneTemperature)
        assertEquals(28, state.heatsinkTemperature)
        assertNull(state.fanRPM)
        assertNull(state.powerLevel)
        assertContentEquals(frame, state.rawData)
        val signed = BlackSharkLib.parseMessages(bytes(0x8A, 0x06, 0x00, 0x00, 0x01, 0xFF, 0x00, 0x3D, 0x00, 0x00))
        assertEquals(-1, assertIs<BlackSharkLib.CoolingState>(signed).phoneTemperature)
        assertEquals(61, signed.heatsinkTemperature)
    }

    @Test
    fun parsesFanState() {
        val frame = bytes(0x86, 0x02, 0x10, 0x00, 0x14, 0x14)
        val state = assertIs<BlackSharkLib.FanState>(BlackSharkLib.parseMessages(frame))
        assertEquals(80, state.speed)
        assertNull(state.model)
        assertContentEquals(frame, state.rawData)
        assertEquals(0, assertIs<BlackSharkLib.FanState>(BlackSharkLib.parseMessages(bytes(0x86, 0x02, 0x10, 0x00, 0xFA, 0x21))).speed)
    }

    @Test
    fun parsesFiveProCoolingState() {
        for ((frame, expected) in listOf(
            bytes(0x89, 0x06, 0x20, 0x00, 0x02, 0x32, 0x58, 0x11, 0x1C) to listOf(2, 50, 4440, 28),
            bytes(0x89, 0x06, 0x20, 0x00, 0x00, 0x33, 0x10, 0x0E, 0x13) to listOf(0, 51, 3600, 19),
            bytes(0x89, 0x06, 0x20, 0x00, 0xFF, 0x33, 0x4C, 0x0E, 0x15) to listOf(-1, 51, 3660, 21),
            bytes(0x89, 0x06, 0x20, 0x00, 0xEC, 0x3D, 0xDE, 0x12, 0x1C) to listOf(-20, 61, 4830, 28),
        )) {
            val state = assertIs<BlackSharkLib.CoolingState>(BlackSharkLib.parseMessages(frame))
            assertEquals(BlackSharkLib.Model.pro5, state.model)
            assertEquals(expected, listOf(state.phoneTemperature, state.heatsinkTemperature, state.fanRPM, state.powerLevel))
            assertContentEquals(frame, state.rawData)
        }
    }

    @Test
    fun malformedFramesAreUnknownMessages() {
        for (frame in listOf(
            ByteArray(0),
            bytes(0x8A),
            bytes(0x8A, 0x06, 0x00),
            bytes(0x8A, 0x06, 0x00, 0x00, 0x01, 0x08, 0x00),
            bytes(0x86, 0x02, 0x10, 0x00),
            bytes(0x89, 0x06, 0x20, 0x00, 0x02, 0x32, 0x58, 0x11),
            bytes(0xAF, 0x01, 0x20, 0x00, 0x01, 0x02, 0x03, 0x04),
        )) {
            val message = assertIs<BlackSharkLib.UnknownMessage>(BlackSharkLib.parseMessages(frame))
            assertContentEquals(frame, message.rawData)
        }
    }

    @Test
    fun parsedFramesDoNotShareTheCallersBuffer() {
        val frame = bytes(0x89, 0x06, 0x20, 0x00, 0x02, 0x32, 0x58, 0x11, 0x1C)
        val state = BlackSharkLib.parseMessages(frame)
        frame[4] = 0x7F
        assertEquals(2, unsigned(state.rawData, 4))
    }

    @Test
    fun fourProCommands() {
        assertContentEquals(bytes(0x05, 0x06, 0x00, 0x00, 0x00), BlackSharkLib.getCoolingMetadataCommand())
        assertContentEquals(bytes(0x05, 0x06, 0x00, 0x00, 0x00), BlackSharkLib.getCoolingMetadataCommand(model = BlackSharkLib.Model.pro4))
        for ((percentage, expected) in listOf(100 to 0x00, 75 to 0x19, 50 to 0x32, 1 to 0x63, 0 to 0xFB)) {
            assertContentEquals(bytes(0x05, 0x02, 0x00, 0x00, expected), BlackSharkLib.getSetFanSpeedCommand(percentage))
            assertContentEquals(bytes(0x05, 0x05, 0x00, 0x00, expected), BlackSharkLib.getSetCoolingPowerCommand(percentage))
        }
        assertNull(BlackSharkLib.getSetFanSpeedCommand(-1))
        assertNull(BlackSharkLib.getSetFanSpeedCommand(101))
        assertNull(BlackSharkLib.getSetCoolingPowerCommand(-1))
        assertNull(BlackSharkLib.getSetCoolingPowerCommand(101))
    }

    @Test
    fun fourProLedCommands() {
        val payload = BlackSharkLib.getSetLEDColorCommand(0x4D, 0xFF, 0x0C, brightness = 100)
        assertContentEquals(
            bytes(0x2F, 0x01, 0x20, 0x00, 0x06, 0x00, 0xFF, 0xFF, 0xFF, 0x00, 0x01, 0x4D, 0xFF, 0x0C) + ByteArray(33),
            payload,
        )
        assertEquals(47, payload?.size)
        val scaled = BlackSharkLib.getSetLEDColorCommand(255, 100, 0, brightness = 50)
        assertEquals(listOf(127, 50, 0), listOf(unsigned(scaled, 11), unsigned(scaled, 12), unsigned(scaled, 13)))
        val off = BlackSharkLib.getTurnOffLEDCommand()
        assertContentEquals(
            bytes(0x2F, 0x01, 0x20, 0x00, 0x01, 0x00, 0xFF, 0xFF, 0xFF, 0x00, 0x01, 0x00, 0x00, 0x00) + ByteArray(33),
            off,
        )
        assertEquals(47, off.size)
    }

    @Test
    fun fiveProCommands() {
        assertContentEquals(bytes(0x05, 0x06, 0x20, 0x00, 0x00), BlackSharkLib.getCoolingMetadataCommand(model = BlackSharkLib.Model.pro5))
        for (intensity in 1..5) {
            assertContentEquals(
                bytes(0x06, 0x05, 0x00, 0x00, 0x04, intensity),
                BlackSharkLib.getSetCustomModeCommand(intensity = intensity, model = BlackSharkLib.Model.pro5),
            )
        }
        for (intensity in listOf(0, 6, -1)) {
            assertNull(BlackSharkLib.getSetCustomModeCommand(intensity = intensity, model = BlackSharkLib.Model.pro5))
        }
        assertContentEquals(bytes(0x05, 0x07, 0x00, 0x00, 0x01), BlackSharkLib.getSetCoolingEnabledCommand(false, model = BlackSharkLib.Model.pro5))
        assertContentEquals(bytes(0x05, 0x07, 0x00, 0x00, 0x00), BlackSharkLib.getSetCoolingEnabledCommand(true, model = BlackSharkLib.Model.pro5))
    }

    @Test
    fun fiveProLedCommands() {
        val payload = BlackSharkLib.getSetLEDColorCommand(0x4D, 0xFF, 0x0C, brightness = 100, model = BlackSharkLib.Model.pro5)
        assertContentEquals(
            bytes(0x10, 0x01, 0x10, 0x00, 0x00, 0x09, 0xFF, 0x00, 0x64, 0x01, 0x4D, 0xFF, 0x0C, 0x00, 0x00, 0x00),
            payload,
        )
        val scaled = BlackSharkLib.getSetLEDColorCommand(255, 100, 0, brightness = 50, model = BlackSharkLib.Model.pro5)
        assertEquals(listOf(127, 50, 0), listOf(unsigned(scaled, 10), unsigned(scaled, 11), unsigned(scaled, 12)))
        assertContentEquals(
            bytes(0x10, 0x01, 0x10, 0x00, 0x00, 0x09, 0xFF, 0x00, 0x64, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
            BlackSharkLib.getTurnOffLEDCommand(model = BlackSharkLib.Model.pro5),
        )
    }

    @Test
    fun commandsAreGatedByModel() {
        assertNull(BlackSharkLib.getSetFanSpeedCommand(50, model = BlackSharkLib.Model.pro5))
        assertNull(BlackSharkLib.getSetCoolingPowerCommand(50, model = BlackSharkLib.Model.pro5))
        assertNull(BlackSharkLib.getSetCustomModeCommand(intensity = 3, model = BlackSharkLib.Model.pro4))
        assertNull(BlackSharkLib.getSetCoolingEnabledCommand(false, model = BlackSharkLib.Model.pro4))
        assertContentEquals(
            BlackSharkLib.getSetLEDColorCommand(1, 2, 3, brightness = 100),
            BlackSharkLib.getSetLEDColorCommand(1, 2, 3, brightness = 100, model = BlackSharkLib.Model.pro4),
        )
    }

    @Test
    fun ledInputIsValidatedAndClamped() {
        for (model in BlackSharkLib.Model.entries) {
            assertNull(BlackSharkLib.getSetLEDColorCommand(0, 0, 0, brightness = -1, model = model))
            assertNull(BlackSharkLib.getSetLEDColorCommand(0, 0, 0, brightness = 101, model = model))
            assertNotNull(BlackSharkLib.getSetLEDColorCommand(Int.MAX_VALUE, Int.MIN_VALUE, 0, brightness = 50, model = model))
        }
        val pro4 = BlackSharkLib.getSetLEDColorCommand(300, -20, 999, brightness = 100)
        assertEquals(listOf(255, 0, 255), listOf(unsigned(pro4, 11), unsigned(pro4, 12), unsigned(pro4, 13)))
        val pro5 = BlackSharkLib.getSetLEDColorCommand(300, -20, 999, brightness = 100, model = BlackSharkLib.Model.pro5)
        assertEquals(listOf(255, 0, 255), listOf(unsigned(pro5, 10), unsigned(pro5, 11), unsigned(pro5, 12)))
    }
}
