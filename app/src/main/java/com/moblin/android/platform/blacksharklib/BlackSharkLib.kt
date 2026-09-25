package com.moblin.android.platform.blacksharklib

import android.util.Log
import java.util.UUID

private const val TAG = "BlackSharkLib"

private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

object BlackSharkLib {
    private val manufacturerDataIdentifier = bytes(0x8F, 0x03)

    fun isBlackSharkDevice(manufacturerData: ByteArray): Boolean {
        if (manufacturerData.size < manufacturerDataIdentifier.size) {
            return false
        }
        return manufacturerDataIdentifier.indices.all { manufacturerData[it] == manufacturerDataIdentifier[it] }
    }

    private val serviceUUID: UUID = UUID.fromString("0000A0A0-3C17-D293-8E48-14FE2E4DA212")

    fun getServiceUUID(): UUID {
        return serviceUUID
    }

    enum class Model {
        pro4,
        pro5,
    }

    fun detectModel(advertisedName: String? = null): Model? {
        val name = advertisedName?.lowercase() ?: return null
        if (name.contains("5pro")) {
            return Model.pro5
        }
        if (name.contains("4pro")) {
            return Model.pro4
        }
        return null
    }

    interface Message {
        val rawData: ByteArray
    }

    class CoolingState(
        override val rawData: ByteArray,
        val model: Model,
        val phoneTemperature: Int,
        val heatsinkTemperature: Int,
        val fanRPM: Int?,
        val powerLevel: Int?,
    ) : Message {
        override fun toString(): String {
            return "CoolingState(model: $model, phoneTemperature: $phoneTemperature, " +
                "heatsinkTemperature: $heatsinkTemperature, fanRPM: $fanRPM, powerLevel: $powerLevel)"
        }
    }

    class FanState(override val rawData: ByteArray, val speed: Int) : Message {
        val model: Model? = null

        override fun toString(): String {
            return "FanState(speed: $speed)"
        }
    }

    class UnknownMessage(override val rawData: ByteArray) : Message {
        override fun toString(): String {
            return "UnknownMessage(rawData: ${rawData.joinToString(" ") { "%02x".format(it) }})"
        }
    }

    private val readCharacteristicsUUID = bytes(0xA0, 0x02)

    fun getReadCharacteristicsUUID(): ByteArray {
        return readCharacteristicsUUID.copyOf()
    }

    fun parseMessages(data: ByteArray): Message {
        val rawData = data.copyOf()
        val bytes = IntArray(rawData.size) { rawData[it].toInt() and 0xFF }
        if (bytes.size <= 2) {
            return UnknownMessage(rawData = rawData)
        }
        if (bytes[1] == 0x02 && bytes[2] == 0x10) {
            if (bytes.size <= 4) {
                return UnknownMessage(rawData = rawData)
            }
            val speed = maxOf(0, minOf(100, 100 - bytes[4]))
            return FanState(rawData = rawData, speed = speed)
        }
        if (bytes[1] == 0x06 && bytes[2] == 0x00) {
            if (bytes.size <= 7) {
                return UnknownMessage(rawData = rawData)
            }
            return CoolingState(
                rawData = rawData,
                model = Model.pro4,
                phoneTemperature = rawData[5].toInt(),
                heatsinkTemperature = rawData[7].toInt(),
                fanRPM = null,
                powerLevel = null,
            )
        }
        if (bytes[0] == 0x89 && bytes[1] == 0x06) {
            if (bytes.size <= 8) {
                return UnknownMessage(rawData = rawData)
            }
            return CoolingState(
                rawData = rawData,
                model = Model.pro5,
                phoneTemperature = rawData[4].toInt(),
                heatsinkTemperature = rawData[5].toInt(),
                fanRPM = bytes[6] or (bytes[7] shl 8),
                powerLevel = bytes[8],
            )
        }
        return UnknownMessage(rawData = rawData)
    }

    private val writeCharacteristicsUUID = bytes(0xA0, 0x01)

    fun getWriteCharacteristicsUUID(): ByteArray {
        return writeCharacteristicsUUID.copyOf()
    }

    fun getCoolingMetadataCommand(model: Model = Model.pro4): ByteArray {
        return when (model) {
            Model.pro4 -> bytes(0x05, 0x06, 0x00, 0x00, 0x00)
            Model.pro5 -> bytes(0x05, 0x06, 0x20, 0x00, 0x00)
        }
    }

    fun getSetFanSpeedCommand(percentage: Int, model: Model = Model.pro4): ByteArray? {
        if (model != Model.pro4) {
            Log.e(TAG, "ERROR: Fan speed is not controllable on a 5 Pro. Use getSetCustomModeCommand.")
            return null
        }
        if (percentage < 0 || percentage > 100) {
            Log.e(TAG, "ERROR: Invalid percentage value. Must be between 0 and 100")
            return null
        }
        val value = if (percentage == 0) 0xFB else 100 - percentage
        return bytes(0x05, 0x02, 0x00, 0x00, value)
    }

    fun getSetCoolingPowerCommand(percentage: Int, model: Model = Model.pro4): ByteArray? {
        if (model != Model.pro4) {
            Log.e(TAG, "ERROR: A 5 Pro takes an intensity, not a percentage. Use getSetCustomModeCommand.")
            return null
        }
        if (percentage < 0 || percentage > 100) {
            Log.e(TAG, "ERROR: Invalid percentage value. Must be between 0 and 100")
            return null
        }
        val value = if (percentage == 0) 0xFB else 100 - percentage
        return bytes(0x05, 0x05, 0x00, 0x00, value)
    }

    fun getSetCustomModeCommand(intensity: Int, model: Model = Model.pro4): ByteArray? {
        if (model != Model.pro5) {
            Log.e(TAG, "ERROR: Custom mode is only supported on the 5 Pro")
            return null
        }
        if (intensity < 1 || intensity > 5) {
            Log.e(TAG, "ERROR: Invalid intensity. Must be between 1 and 5")
            return null
        }
        return bytes(0x06, 0x05, 0x00, 0x00, 0x04, intensity)
    }

    fun getSetCoolingEnabledCommand(enabled: Boolean, model: Model = Model.pro4): ByteArray? {
        if (model != Model.pro5) {
            Log.e(TAG, "ERROR: Switching cooling separately is only supported on the 5 Pro")
            return null
        }
        return bytes(0x05, 0x07, 0x00, 0x00, if (enabled) 0x00 else 0x01)
    }

    fun getSetLEDColorCommand(red: Int, green: Int, blue: Int, brightness: Int, model: Model = Model.pro4): ByteArray? {
        if (brightness < 0 || brightness > 100) {
            Log.e(TAG, "ERROR: Invalid brightness value. Must be between 0 and 100")
            return null
        }
        val color = LEDColor(red = red, green = green, blue = blue).scaled(brightness = brightness)
        if (model == Model.pro5) {
            return pro5SolidColorFrame(color)
        }
        return bytes(
            0x2F, 0x01, 0x20, 0x00,
            0x06,
            0x00, 0xFF, 0xFF, 0xFF, 0x00, 0x01,
            color.red, color.green, color.blue,
        ) + ByteArray(33)
    }

    fun getTurnOffLEDCommand(model: Model = Model.pro4): ByteArray {
        if (model == Model.pro5) {
            return pro5SolidColorFrame(LEDColor(red = 0, green = 0, blue = 0))
        }
        return bytes(
            0x2F, 0x01, 0x20, 0x00,
            0x01,
            0x00, 0xFF, 0xFF, 0xFF, 0x00, 0x01,
            0x00, 0x00, 0x00,
        ) + ByteArray(33)
    }

    private fun pro5SolidColorFrame(color: LEDColor): ByteArray {
        return bytes(
            0x10, 0x01, 0x10, 0x00,
            0x00, 0x09, 0xFF, 0x00,
            0x64,
            0x01,
            color.red, color.green, color.blue,
            0x00, 0x00, 0x00,
        )
    }

    private class LEDColor(red: Int, green: Int, blue: Int) {
        val red = red.coerceIn(0, 255)
        val green = green.coerceIn(0, 255)
        val blue = blue.coerceIn(0, 255)

        fun scaled(brightness: Int): LEDColor {
            val scale = brightness.coerceIn(0, 100).toDouble() / 100.0
            return LEDColor(
                red = (red.toDouble() * scale).toInt(),
                green = (green.toDouble() * scale).toInt(),
                blue = (blue.toDouble() * scale).toInt(),
            )
        }
    }
}
