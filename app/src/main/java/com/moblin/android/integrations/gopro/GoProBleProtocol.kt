package com.moblin.android.integrations.gopro

import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumLens
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumPairingFinishState
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumRegisterLiveStreamStatus
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumScanEntryFlags
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_EnumWindowSize
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_RequestConnect
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_RequestConnectNew
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_RequestGetApEntries
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_RequestGetLiveStreamStatus
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_RequestPairingFinish
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_RequestSetLiveStreamMode
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_RequestStartScan
import com.moblin.android.integrations.gopro.protobuf.OpenGopro_ResponseGetApEntries
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProLens
import java.util.UUID

val goProControlServiceId: UUID = UUID.fromString("0000fea6-0000-1000-8000-00805f9b34fb")
val goProCommandId: UUID = UUID.fromString("b5f90072-aa8d-11e3-9046-0002a5d5c51b")
val goProCommandResponseId: UUID = UUID.fromString("b5f90073-aa8d-11e3-9046-0002a5d5c51b")
val goProSettingsId: UUID = UUID.fromString("b5f90074-aa8d-11e3-9046-0002a5d5c51b")
val goProSettingsResponseId: UUID = UUID.fromString("b5f90075-aa8d-11e3-9046-0002a5d5c51b")
val goProQueryId: UUID = UUID.fromString("b5f90076-aa8d-11e3-9046-0002a5d5c51b")
val goProQueryResponseId: UUID = UUID.fromString("b5f90077-aa8d-11e3-9046-0002a5d5c51b")
val goProCameraManagementServiceId: UUID = UUID.fromString("b5f90090-aa8d-11e3-9046-0002a5d5c51b")
val goProNetworkManagementId: UUID = UUID.fromString("b5f90091-aa8d-11e3-9046-0002a5d5c51b")
val goProNetworkManagementResponseId: UUID = UUID.fromString("b5f90092-aa8d-11e3-9046-0002a5d5c51b")

val goProNetworkFeatureId: UByte = 0x02.toUByte()
val goProPairingFeatureId: UByte = 0x03.toUByte()
val goProLiveStreamCommandFeatureId: UByte = 0xF1.toUByte()
val goProLiveStreamQueryFeatureId: UByte = 0xF5.toUByte()
val goProPairingFinishActionId: UByte = 0x01.toUByte()
val goProStartScanActionId: UByte = 0x02.toUByte()
val goProGetApEntriesActionId: UByte = 0x03.toUByte()
val goProConnectActionId: UByte = 0x04.toUByte()
val goProConnectNewActionId: UByte = 0x05.toUByte()
val goProScanningNotificationId: UByte = 0x0B.toUByte()
val goProProvisioningNotificationId: UByte = 0x0C.toUByte()
val goProGetLiveStreamStatusActionId: UByte = 0x74.toUByte()
val goProSetLiveStreamModeActionId: UByte = 0x79.toUByte()
val goProPairingFinishResponseId: UByte = 0x81.toUByte()
val goProStartScanResponseId: UByte = 0x82.toUByte()
val goProGetApEntriesResponseId: UByte = 0x83.toUByte()
val goProConnectResponseId: UByte = 0x84.toUByte()
val goProConnectNewResponseId: UByte = 0x85.toUByte()
val goProGetLiveStreamStatusResponseId: UByte = 0xF4.toUByte()
val goProLiveStreamStatusNotificationId: UByte = 0xF5.toUByte()
val goProSetLiveStreamModeResponseId: UByte = 0xF9.toUByte()
val goProShutterCommandId: UByte = 0x01.toUByte()
val goProGetStatusQueryId: UByte = 0x13.toUByte()
val goProKeepAliveSettingId: UByte = 0x5B.toUByte()
val goProBatteryPercentageStatusId: UByte = 0x46.toUByte()
val goProResponseSuccessStatus: UByte = 0x00.toUByte()
val goProMaximumApEntriesPerRequest: Int = 100

private val goProKeepAliveValue: UByte = 0x42.toUByte()
private val goProMaximumPacketSize = 20
private val goProMaximumPayloadSize = 8191
private val goProContinuationPacketHeader: UByte = 0x80.toUByte()
private val goProGeneralPacketHeaderType: UByte = 0.toUByte()
private val goProExtended13PacketHeaderType: UByte = 1.toUByte()
private val goProExtended16PacketHeaderType: UByte = 2.toUByte()
private val goProPacketHeaderTypeMask: UByte = 0x60.toUByte()
private val goProPacketHeaderTypeShift: UByte = 5.toUByte()
private val goProPacketHeaderLengthMask: UByte = 0x1F.toUByte()

fun goProBlePackets(payload: ByteArray, maximumPacketSize: Int = goProMaximumPacketSize): List<ByteArray> {
    if (payload.isEmpty() || payload.size >= goProMaximumPayloadSize || maximumPacketSize < 3) {
        return emptyList()
    }
    val length = payload.size
    val packets = mutableListOf<ByteArray>()
    var offset = 0
    val first = byteArrayOf(
        ((goProExtended13PacketHeaderType.toInt() shl goProPacketHeaderTypeShift.toInt()) or
            ((length shr 8) and goProPacketHeaderLengthMask.toInt())).toByte(),
        (length and 0xFF).toByte(),
    )
    val firstCount = minOf(maximumPacketSize - first.size, length)
    packets.add(first + payload.copyOf(firstCount))
    offset += firstCount
    while (offset < length) {
        val count = minOf(maximumPacketSize - 1, length - offset)
        packets.add(
            byteArrayOf(goProContinuationPacketHeader.toByte()) +
                payload.copyOfRange(offset, offset + count)
        )
        offset += count
    }
    return packets
}

fun goProPairingCompleteMessage(): ByteArray {
    val request = OpenGopro_RequestPairingFinish()
    request.result = OpenGopro_EnumPairingFinishState.success
    request.phoneName = "Moblin"
    return byteArrayOf(goProPairingFeatureId.toByte(), goProPairingFinishActionId.toByte()) + request.encoded()
}

fun goProStartScanMessage(): ByteArray =
    byteArrayOf(goProNetworkFeatureId.toByte(), goProStartScanActionId.toByte()) +
        OpenGopro_RequestStartScan().encoded()

fun goProGetApEntriesMessage(scanId: Int, startIndex: Int, maximumEntries: Int): ByteArray {
    val request = OpenGopro_RequestGetApEntries()
    request.startIndex = startIndex
    request.maxEntries = maximumEntries
    request.scanID = scanId
    return byteArrayOf(goProNetworkFeatureId.toByte(), goProGetApEntriesActionId.toByte()) + request.encoded()
}

fun goProConnectToProvisionedWifiMessage(ssid: String): ByteArray {
    val request = OpenGopro_RequestConnect()
    request.ssid = ssid
    return byteArrayOf(goProNetworkFeatureId.toByte(), goProConnectActionId.toByte()) + request.encoded()
}

fun goProConnectToWifiMessage(ssid: String, password: String): ByteArray {
    val request = OpenGopro_RequestConnectNew()
    request.ssid = ssid
    request.password = password
    request.bypassEulaCheck = true
    return byteArrayOf(goProNetworkFeatureId.toByte(), goProConnectNewActionId.toByte()) + request.encoded()
}

fun goProRegisterLiveStreamStatusMessage(): ByteArray {
    val request = OpenGopro_RequestGetLiveStreamStatus()
    request.registerLiveStreamStatus = mutableListOf(
        OpenGopro_EnumRegisterLiveStreamStatus.registerLiveStreamStatusStatus,
        OpenGopro_EnumRegisterLiveStreamStatus.registerLiveStreamStatusError,
        OpenGopro_EnumRegisterLiveStreamStatus.registerLiveStreamStatusBitrate,
    )
    return byteArrayOf(goProLiveStreamQueryFeatureId.toByte(), goProGetLiveStreamStatusActionId.toByte()) +
        request.encoded()
}

fun goProGetLiveStreamStatusMessage(): ByteArray =
    byteArrayOf(goProLiveStreamQueryFeatureId.toByte(), goProGetLiveStreamStatusActionId.toByte()) +
        OpenGopro_RequestGetLiveStreamStatus().encoded()

fun goProSetLiveStreamModeMessage(
    url: String,
    resolution: SettingsGoProLaunchLiveStreamResolution,
    bitrate: UInt,
    lens: SettingsGoProLens,
): ByteArray {
    val request = OpenGopro_RequestSetLiveStreamMode()
    request.url = url
    request.encode = false
    request.windowSize = resolution.toProtobuf()
    request.minimumBitrate = 800
    request.maximumBitrate = (bitrate / 1000u).toInt()
    request.startingBitrate = (bitrate / 1000u).toInt()
    lens.toProtobuf()?.let { request.lens = it }
    return byteArrayOf(goProLiveStreamCommandFeatureId.toByte(), goProSetLiveStreamModeActionId.toByte()) +
        request.encoded()
}

fun goProSetShutterMessage(on: Boolean): ByteArray =
    byteArrayOf(goProShutterCommandId.toByte(), 1, (if (on) 1 else 0).toByte())

fun goProKeepAliveMessage(): ByteArray =
    byteArrayOf(goProKeepAliveSettingId.toByte(), 1, goProKeepAliveValue.toByte())

fun goProGetBatteryPercentageMessage(): ByteArray =
    byteArrayOf(goProGetStatusQueryId.toByte(), goProBatteryPercentageStatusId.toByte())

class GoProBleMessageAccumulator {
    private var expectedLength: Int? = null
    private var payload: ByteArray = ByteArray(0)

    fun append(packet: ByteArray): ByteArray? {
        val firstByte = packet.firstOrNull() ?: return null
        if ((firstByte.toInt() and goProContinuationPacketHeader.toInt()) != 0) {
            if (expectedLength == null) {
                reset()
                return null
            }
            payload += packet.copyOfRange(1, packet.size)
        } else {
            reset()
            val headerType = (firstByte.toInt() and goProPacketHeaderTypeMask.toInt()) shr
                goProPacketHeaderTypeShift.toInt()
            val headerLength: Int
            when (headerType) {
                goProGeneralPacketHeaderType.toInt() -> {
                    expectedLength = firstByte.toInt() and goProPacketHeaderLengthMask.toInt()
                    headerLength = 1
                }
                goProExtended13PacketHeaderType.toInt() -> {
                    if (packet.size < 2) {
                        return null
                    }
                    expectedLength = ((firstByte.toInt() and goProPacketHeaderLengthMask.toInt()) shl 8) or
                        (packet[1].toInt() and 0xFF)
                    headerLength = 2
                }
                goProExtended16PacketHeaderType.toInt() -> {
                    if (packet.size < 3) {
                        return null
                    }
                    expectedLength = ((packet[1].toInt() and 0xFF) shl 8) or (packet[2].toInt() and 0xFF)
                    headerLength = 3
                }
                else -> return null
            }
            payload += packet.copyOfRange(headerLength, packet.size)
        }
        val expected = expectedLength
        if (expected == null || payload.size < expected) {
            return null
        }
        val message = payload.copyOf(expected)
        reset()
        return message
    }

    private fun reset() {
        expectedLength = null
        payload = ByteArray(0)
    }
}

fun OpenGopro_ResponseGetApEntries.ScanEntry.isConfigured(): Boolean =
    (scanEntryFlags and OpenGopro_EnumScanEntryFlags.scanFlagConfigured.rawValue) != 0

fun OpenGopro_ResponseGetApEntries.ScanEntry.isUnsupportedType(): Boolean =
    (scanEntryFlags and OpenGopro_EnumScanEntryFlags.scanFlagUnsupportedType.rawValue) != 0

private fun Any.encoded(): ByteArray = TODO("protobuf serialization is not available")

private fun SettingsGoProLaunchLiveStreamResolution.toProtobuf(): OpenGopro_EnumWindowSize = when (this) {
    SettingsGoProLaunchLiveStreamResolution.r480p -> OpenGopro_EnumWindowSize.windowSize480
    SettingsGoProLaunchLiveStreamResolution.r720p -> OpenGopro_EnumWindowSize.windowSize720
    SettingsGoProLaunchLiveStreamResolution.r1080p -> OpenGopro_EnumWindowSize.windowSize1080
}

fun SettingsGoProLens.toProtobuf(): OpenGopro_EnumLens? = when (this) {
    SettingsGoProLens.auto -> null
    SettingsGoProLens.wide -> OpenGopro_EnumLens.lensWide
    SettingsGoProLens.linear -> OpenGopro_EnumLens.lensLinear
    SettingsGoProLens.superView -> OpenGopro_EnumLens.lensSuperview
}
