package com.moblin.android.platform.corebluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.nio.ByteBuffer
import java.util.Locale
import java.util.UUID

const val CBAdvertisementDataLocalNameKey = "kCBAdvDataLocalName"
const val CBAdvertisementDataManufacturerDataKey = "kCBAdvDataManufacturerData"
const val CBAdvertisementDataServiceDataKey = "kCBAdvDataServiceData"
const val CBAdvertisementDataServiceUUIDsKey = "kCBAdvDataServiceUUIDs"
const val CBAdvertisementDataSolicitedServiceUUIDsKey = "kCBAdvDataSolicitedServiceUUIDs"
const val CBAdvertisementDataTxPowerLevelKey = "kCBAdvDataTxPowerLevel"
const val CBAdvertisementDataIsConnectable = "kCBAdvDataIsConnectable"
const val CBCentralManagerScanOptionAllowDuplicatesKey = "kCBScanOptionAllowDuplicates"
const val CBCentralManagerOptionShowPowerAlertKey = "kCBInitOptionShowPowerAlert"
const val CBConnectPeripheralOptionEnableAutoReconnect = "kCBConnectOptionEnableAutoReconnect"
const val CBConnectPeripheralOptionNotifyOnDisconnectionKey = "kCBConnectOptionNotifyOnDisconnection"

private val bluetoothBaseUuid: UUID = UUID.fromString("00000000-0000-1000-8000-00805F9B34FB")
private val uuidPattern = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

class CBUUID(nsuuid: UUID) {
    internal val uuid: UUID = nsuuid

    constructor(string: String) : this(parseUuid(string))

    constructor(data: ByteArray) : this(uuidFromData(data))

    val uuidString: String
        get() {
            val value = shortValue() ?: return uuid.toString().uppercase(Locale.ROOT)
            return if (value <= 0xFFFF) {
                String.format(Locale.ROOT, "%04X", value)
            } else {
                String.format(Locale.ROOT, "%08X", value)
            }
        }

    val data: ByteArray
        get() {
            val value = shortValue()
            return when {
                value == null -> ByteBuffer.allocate(16)
                    .putLong(uuid.mostSignificantBits)
                    .putLong(uuid.leastSignificantBits)
                    .array()
                value <= 0xFFFF -> byteArrayOf((value shr 8).toByte(), value.toByte())
                else -> ByteBuffer.allocate(4).putInt(value.toInt()).array()
            }
        }

    private fun shortValue(): Long? {
        if (uuid.leastSignificantBits != bluetoothBaseUuid.leastSignificantBits) {
            return null
        }
        if (uuid.mostSignificantBits and 0xFFFFFFFFL != bluetoothBaseUuid.mostSignificantBits) {
            return null
        }
        return uuid.mostSignificantBits ushr 32
    }

    override fun equals(other: Any?): Boolean {
        return other is CBUUID && other.uuid == uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    override fun toString(): String {
        return uuidString
    }

    private companion object {
        fun parseUuid(string: String): UUID {
            if ((string.length == 4 || string.length == 8) && string.all { isHexDigit(it) }) {
                return uuidFromShort(string.toLong(16))
            }
            if (uuidPattern.matches(string)) {
                return UUID.fromString(string)
            }
            throw IllegalArgumentException("Invalid UUID string: $string")
        }

        fun uuidFromData(data: ByteArray): UUID {
            return when (data.size) {
                2 -> uuidFromShort(ByteBuffer.wrap(byteArrayOf(0, 0) + data).int.toLong())
                4 -> uuidFromShort(ByteBuffer.wrap(data).int.toLong() and 0xFFFFFFFFL)
                16 -> ByteBuffer.wrap(data).let { UUID(it.long, it.long) }
                else -> throw IllegalArgumentException("Data must be 2, 4 or 16 bytes, not ${data.size}")
            }
        }

        fun uuidFromShort(value: Long): UUID {
            return UUID((value shl 32) or bluetoothBaseUuid.mostSignificantBits, bluetoothBaseUuid.leastSignificantBits)
        }

        fun isHexDigit(character: Char): Boolean {
            return character in '0'..'9' || character in 'a'..'f' || character in 'A'..'F'
        }
    }
}

class CBCharacteristicProperties(val rawValue: Int) {
    operator fun plus(other: CBCharacteristicProperties): CBCharacteristicProperties {
        return CBCharacteristicProperties(rawValue or other.rawValue)
    }

    fun union(other: CBCharacteristicProperties): CBCharacteristicProperties {
        return plus(other)
    }

    fun intersection(other: CBCharacteristicProperties): CBCharacteristicProperties {
        return CBCharacteristicProperties(rawValue and other.rawValue)
    }

    fun contains(member: CBCharacteristicProperties): Boolean {
        return rawValue and member.rawValue == member.rawValue
    }

    val isEmpty: Boolean
        get() = rawValue == 0

    override fun equals(other: Any?): Boolean {
        return other is CBCharacteristicProperties && other.rawValue == rawValue
    }

    override fun hashCode(): Int {
        return rawValue
    }

    override fun toString(): String {
        return "CBCharacteristicProperties(rawValue: $rawValue)"
    }

    companion object {
        val broadcast = CBCharacteristicProperties(0x01)
        val read = CBCharacteristicProperties(0x02)
        val writeWithoutResponse = CBCharacteristicProperties(0x04)
        val write = CBCharacteristicProperties(0x08)
        val notify = CBCharacteristicProperties(0x10)
        val indicate = CBCharacteristicProperties(0x20)
        val authenticatedSignedWrites = CBCharacteristicProperties(0x40)
        val extendedProperties = CBCharacteristicProperties(0x80)
        val notifyEncryptionRequired = CBCharacteristicProperties(0x100)
        val indicateEncryptionRequired = CBCharacteristicProperties(0x200)
    }
}

enum class CBCharacteristicWriteType {
    withResponse,
    withoutResponse,
}

open class CBAttribute internal constructor(val uuid: CBUUID)

class CBService internal constructor(
    internal val service: BluetoothGattService,
    val peripheral: CBPeripheral?,
) : CBAttribute(CBUUID(service.uuid)) {
    val isPrimary: Boolean = service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY

    @Volatile
    var characteristics: List<CBCharacteristic>? = null
        internal set

    override fun toString(): String {
        return "CBService(uuid: $uuid, isPrimary: $isPrimary)"
    }
}

class CBCharacteristic internal constructor(
    internal val characteristic: BluetoothGattCharacteristic,
    val service: CBService?,
) : CBAttribute(CBUUID(characteristic.uuid)) {
    val properties = CBCharacteristicProperties(characteristic.properties and 0xFF)

    @Volatile
    var value: ByteArray? = null
        internal set

    @Volatile
    var isNotifying = false
        internal set

    @Volatile
    var descriptors: List<CBDescriptor>? = null
        internal set

    override fun toString(): String {
        return "CBCharacteristic(uuid: $uuid, properties: ${properties.rawValue}, isNotifying: $isNotifying)"
    }
}

class CBDescriptor internal constructor(
    internal val descriptor: BluetoothGattDescriptor,
    val characteristic: CBCharacteristic?,
) : CBAttribute(CBUUID(descriptor.uuid)) {
    @Volatile
    var value: Any? = null
        internal set

    override fun toString(): String {
        return "CBDescriptor(uuid: $uuid)"
    }
}

internal fun descriptorValue(uuid: CBUUID, bytes: ByteArray): Any {
    return when (uuid.uuidString) {
        "2901" -> String(bytes, Charsets.UTF_8)
        "2900", "2902", "2903" -> if (bytes.size >= 2) {
            (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        } else {
            bytes
        }
        else -> bytes
    }
}

class CBError(val code: Code, message: String? = null) : Exception(message ?: "CBError.${code.name}") {
    enum class Code(val rawValue: Int) {
        unknown(0),
        invalidParameters(1),
        invalidHandle(2),
        notConnected(3),
        outOfSpace(4),
        operationCancelled(5),
        connectionTimeout(6),
        peripheralDisconnected(7),
        uuidNotAllowed(8),
        alreadyAdvertising(9),
        connectionFailed(10),
        connectionLimitReached(11),
        unknownDevice(12),
        operationNotSupported(13),
        peerRemovedPairingInformation(14),
        encryptionTimedOut(15),
        tooManyLEPairedDevices(16),
    }
}

class CBATTError(val code: Code, message: String? = null) : Exception(message ?: "CBATTError.${code.name}") {
    enum class Code(val rawValue: Int) {
        success(0x00),
        invalidHandle(0x01),
        readNotPermitted(0x02),
        writeNotPermitted(0x03),
        invalidPdu(0x04),
        insufficientAuthentication(0x05),
        requestNotSupported(0x06),
        invalidOffset(0x07),
        insufficientAuthorization(0x08),
        prepareQueueFull(0x09),
        attributeNotFound(0x0A),
        attributeNotLong(0x0B),
        insufficientEncryptionKeySize(0x0C),
        invalidAttributeValueLength(0x0D),
        unlikelyError(0x0E),
        insufficientEncryption(0x0F),
        unsupportedGroupType(0x10),
        insufficientResources(0x11),
    }
}

internal fun gattError(status: Int): Throwable? {
    if (status == BluetoothGatt.GATT_SUCCESS) {
        return null
    }
    val code = CBATTError.Code.entries.firstOrNull { it.rawValue == status }
    return if (code != null) CBATTError(code) else CBError(CBError.Code.unknown, "GATT status $status")
}

internal fun disconnectionError(status: Int): Throwable {
    return when (status) {
        0x08 -> CBError(CBError.Code.connectionTimeout)
        0x3E -> CBError(CBError.Code.connectionFailed)
        else -> CBError(CBError.Code.peripheralDisconnected, "The specified device has disconnected from us (status $status)")
    }
}
