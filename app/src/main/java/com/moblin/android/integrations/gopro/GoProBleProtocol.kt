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
import com.moblin.android.platform.corebluetooth.CBUUID
import com.moblin.android.platform.swiftprotobuf.Message
import com.moblin.android.platform.swiftprotobuf.serializedData
import com.moblin.android.various.settings.SettingsGoProLaunchLiveStreamResolution
import com.moblin.android.various.settings.SettingsGoProLens

val goProControlServiceId = CBUUID(string = "FEA6")
val goProCommandId = CBUUID(string = "B5F90072-AA8D-11E3-9046-0002A5D5C51B")
val goProCommandResponseId = CBUUID(string = "B5F90073-AA8D-11E3-9046-0002A5D5C51B")
val goProSettingsId = CBUUID(string = "B5F90074-AA8D-11E3-9046-0002A5D5C51B")
val goProSettingsResponseId = CBUUID(string = "B5F90075-AA8D-11E3-9046-0002A5D5C51B")
val goProQueryId = CBUUID(string = "B5F90076-AA8D-11E3-9046-0002A5D5C51B")
val goProQueryResponseId = CBUUID(string = "B5F90077-AA8D-11E3-9046-0002A5D5C51B")
val goProCameraManagementServiceId =
    CBUUID(string = "B5F90090-AA8D-11E3-9046-0002A5D5C51B")
val goProNetworkManagementId = CBUUID(string = "B5F90091-AA8D-11E3-9046-0002A5D5C51B")
val goProNetworkManagementResponseId =
    CBUUID(string = "B5F90092-AA8D-11E3-9046-0002A5D5C51B")

val goProNetworkFeatureId: UByte = 0x02u
val goProPairingFeatureId: UByte = 0x03u
val goProLiveStreamCommandFeatureId: UByte = 0xF1u
val goProLiveStreamQueryFeatureId: UByte = 0xF5u
val goProPairingFinishActionId: UByte = 0x01u
val goProStartScanActionId: UByte = 0x02u
val goProGetApEntriesActionId: UByte = 0x03u
val goProConnectActionId: UByte = 0x04u
val goProConnectNewActionId: UByte = 0x05u
val goProScanningNotificationId: UByte = 0x0Bu
val goProProvisioningNotificationId: UByte = 0x0Cu
val goProGetLiveStreamStatusActionId: UByte = 0x74u
val goProSetLiveStreamModeActionId: UByte = 0x79u
val goProPairingFinishResponseId: UByte = 0x81u
val goProStartScanResponseId: UByte = 0x82u
val goProGetApEntriesResponseId: UByte = 0x83u
val goProConnectResponseId: UByte = 0x84u
val goProConnectNewResponseId: UByte = 0x85u
val goProGetLiveStreamStatusResponseId: UByte = 0xF4u
val goProLiveStreamStatusNotificationId: UByte = 0xF5u
val goProSetLiveStreamModeResponseId: UByte = 0xF9u
val goProShutterCommandId: UByte = 0x01u
val goProGetStatusQueryId: UByte = 0x13u
val goProKeepAliveSettingId: UByte = 0x5Bu
val goProBatteryPercentageStatusId: UByte = 0x46u
val goProResponseSuccessStatus: UByte = 0x00u
val goProMaximumApEntriesPerRequest: Int = 100

private val goProKeepAliveValue: UByte = 0x42u
private val goProMaximumPacketSize = 20
private val goProMaximumPayloadSize = 8191
private val goProContinuationPacketHeader: UByte = 0x80u
private val goProGeneralPacketHeaderType: UByte = 0u
private val goProExtended13PacketHeaderType: UByte = 1u
private val goProExtended16PacketHeaderType: UByte = 2u
private val goProPacketHeaderTypeMask: UByte = 0x60u
private val goProPacketHeaderTypeShift: UByte = 5u
private val goProPacketHeaderLengthMask: UByte = 0x1Fu

fun goProBlePackets(payload: ByteArray, maximumPacketSize: Int = goProMaximumPacketSize): List<ByteArray> {
    if (payload.isEmpty() || payload.size >= goProMaximumPayloadSize || maximumPacketSize < 3) {
        return emptyList()
    }
    val length = payload.size
    val packets = mutableListOf<ByteArray>()
    var offset = 0
    var first = byteArrayOf(
        (
            (goProExtended13PacketHeaderType.toInt() shl goProPacketHeaderTypeShift.toInt()) or
                ((length shr 8) and goProPacketHeaderLengthMask.toInt())
            ).toByte(),
        (length and 0xFF).toByte(),
    )
    val firstCount = minOf(maximumPacketSize - first.size, length)
    first += payload.copyOfRange(0, firstCount)
    packets.add(first)
    offset += firstCount
    while (offset < length) {
        var continuation = byteArrayOf(goProContinuationPacketHeader.toByte())
        val count = minOf(maximumPacketSize - 1, length - offset)
        continuation += payload.copyOfRange(offset, offset + count)
        packets.add(continuation)
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
    lens.toProtobuf()?.let {
        request.lens = it
    }
    return byteArrayOf(goProLiveStreamCommandFeatureId.toByte(), goProSetLiveStreamModeActionId.toByte()) +
        request.encoded()
}

fun goProSetShutterMessage(on: Boolean): ByteArray =
    byteArrayOf(goProShutterCommandId.toByte(), 1, if (on) 1 else 0)

fun goProKeepAliveMessage(): ByteArray =
    byteArrayOf(goProKeepAliveSettingId.toByte(), 1, goProKeepAliveValue.toByte())

fun goProGetBatteryPercentageMessage(): ByteArray =
    byteArrayOf(goProGetStatusQueryId.toByte(), goProBatteryPercentageStatusId.toByte())

class GoProBleMessageAccumulator {
    private var expectedLength: Int? = null
    private var payload = ByteArray(0)

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
            val headerLength = when (headerType) {
                goProGeneralPacketHeaderType.toInt() -> {
                    expectedLength = firstByte.toInt() and goProPacketHeaderLengthMask.toInt()
                    1
                }
                goProExtended13PacketHeaderType.toInt() -> {
                    if (packet.size < 2) {
                        return null
                    }
                    expectedLength = ((firstByte.toInt() and goProPacketHeaderLengthMask.toInt()) shl 8) or
                        (packet[1].toInt() and 0xFF)
                    2
                }
                goProExtended16PacketHeaderType.toInt() -> {
                    if (packet.size < 3) {
                        return null
                    }
                    expectedLength = ((packet[1].toInt() and 0xFF) shl 8) or (packet[2].toInt() and 0xFF)
                    3
                }
                else -> return null
            }
            payload += packet.copyOfRange(headerLength, packet.size)
        }
        val expectedLength = expectedLength
        if (expectedLength == null || payload.size < expectedLength) {
            return null
        }
        val message = payload.copyOfRange(0, expectedLength)
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

private fun Message.encoded(): ByteArray =
    runCatching { serializedData() }.getOrNull() ?: ByteArray(0)

private fun SettingsGoProLaunchLiveStreamResolution.toProtobuf(): OpenGopro_EnumWindowSize =
    when (this) {
        SettingsGoProLaunchLiveStreamResolution.r480p -> OpenGopro_EnumWindowSize.windowSize480
        SettingsGoProLaunchLiveStreamResolution.r720p -> OpenGopro_EnumWindowSize.windowSize720
        SettingsGoProLaunchLiveStreamResolution.r1080p -> OpenGopro_EnumWindowSize.windowSize1080
    }

fun SettingsGoProLens.toProtobuf(): OpenGopro_EnumLens? =
    when (this) {
        SettingsGoProLens.auto -> null
        SettingsGoProLens.wide -> OpenGopro_EnumLens.lensWide
        SettingsGoProLens.linear -> OpenGopro_EnumLens.lensLinear
        SettingsGoProLens.superView -> OpenGopro_EnumLens.lensSuperview
    }
