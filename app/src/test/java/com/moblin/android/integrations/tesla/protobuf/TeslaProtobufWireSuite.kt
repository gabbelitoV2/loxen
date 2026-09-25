package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecodingError
import com.moblin.android.platform.swiftprotobuf.Google_Protobuf_Timestamp
import com.moblin.android.platform.swiftprotobuf.Message
import com.moblin.android.platform.swiftprotobuf.merge
import com.moblin.android.platform.swiftprotobuf.serializedData
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TeslaProtobufWireSuite {
    private fun <M : Message> assertWire(
        expected: String,
        message: M,
        decode: (ByteArray) -> M,
    ) {
        assertEquals(expected, hex(message.serializedData()))
        val decoded = decode(unhex(expected))
        assertEquals(message, decoded)
        assertEquals(message.hashCode(), decoded.hashCode())
        assertEquals(expected, hex(decoded.serializedData()))
    }

    private fun schedule(block: CarServer_ChargeSchedule.() -> Unit) = CarServer_ChargeSchedule.with(block)

    private val decodeSchedule = { bytes: ByteArray -> CarServer_ChargeSchedule(serializedBytes = bytes) }

    @Test
    fun int32EdgeValues() {
        assertWire("", schedule { daysOfWeek = 0 }, decodeSchedule)
        assertWire("18ffffffffffffffffff01", schedule { daysOfWeek = -1 }, decodeSchedule)
        assertWire("18ffffffff07", schedule { daysOfWeek = Int.MAX_VALUE }, decodeSchedule)
        assertWire("1880808080f8ffffffff01", schedule { daysOfWeek = Int.MIN_VALUE }, decodeSchedule)
        assertEquals(5, decodeSchedule(unhex("188580808010")).daysOfWeek)
    }

    @Test
    fun uint64EdgeValues() {
        assertWire("", schedule { id = 0uL }, decodeSchedule)
        assertWire("0801", schedule { id = 1uL }, decodeSchedule)
        assertWire("08ffffffffffffffffff01", schedule { id = ULong.MAX_VALUE }, decodeSchedule)
        assertWire("0880808080808080808001", schedule { id = 1uL shl 63 }, decodeSchedule)
    }

    @Test
    fun uint32EdgeValues() {
        val decode = { bytes: ByteArray -> UniversalMessage_RoutableMessage(serializedBytes = bytes) }
        assertWire("", UniversalMessage_RoutableMessage.with { flags = 0u }, decode)
        assertWire("a00301", UniversalMessage_RoutableMessage.with { flags = 1u }, decode)
        assertWire("a003ffffffff0f", UniversalMessage_RoutableMessage.with { flags = UInt.MAX_VALUE }, decode)
        assertEquals(0xFFFFFFFFu, decode(unhex("a003ffffffffffffffffff01")).flags)
    }

    @Test
    fun int64EdgeValues() {
        val decode = { bytes: ByteArray -> CarServer_NearbyChargingSites(serializedBytes = bytes) }
        fun sites(value: Long) = CarServer_NearbyChargingSites.with { congestionSyncTimeUtcSecs = value }
        assertWire("", sites(0), decode)
        assertWire("20ffffffffffffffffff01", sites(-1), decode)
        assertWire("20ffffffffffffffff7f", sites(Long.MAX_VALUE), decode)
        assertWire("2080808080808080808001", sites(Long.MIN_VALUE), decode)
    }

    @Test
    fun sint32EdgeValues() {
        val decode = { bytes: ByteArray -> CarServer_HvacTemperatureAdjustmentAction(serializedBytes = bytes) }
        fun action(value: Int) = CarServer_HvacTemperatureAdjustmentAction.with { deltaPercent = value }
        assertWire("", action(0), decode)
        assertWire("1001", action(-1), decode)
        assertWire("1002", action(1), decode)
        assertWire("10feffffff0f", action(Int.MAX_VALUE), decode)
        assertWire("10ffffffff0f", action(Int.MIN_VALUE), decode)
        val volume = CarServer_MediaUpdateVolume.with { volumeDelta = -3 }
        assertWire("0805", volume, { CarServer_MediaUpdateVolume(serializedBytes = it) })
    }

    @Test
    fun fixed32EdgeValues() {
        val decode = { bytes: ByteArray -> Signatures_AES_GCM_Personalized_Signature_Data(serializedBytes = bytes) }
        fun data(value: UInt) = Signatures_AES_GCM_Personalized_Signature_Data.with { expiresAt = value }
        assertWire("", data(0u), decode)
        assertWire("2501000000", data(1u), decode)
        assertWire("2578563412", data(0x12345678u), decode)
        assertWire("25ffffffff", data(UInt.MAX_VALUE), decode)
    }

    @Test
    fun floatEdgeValues() {
        assertWire("", schedule { latitude = 0f }, decodeSchedule)
        assertWire("550000803f", schedule { latitude = 1f }, decodeSchedule)
        assertWire("55000080bf", schedule { latitude = -1f }, decodeSchedule)
        assertWire("5500000080", schedule { latitude = -0f }, decodeSchedule)
        assertWire("55ffff7f7f", schedule { latitude = Float.MAX_VALUE }, decodeSchedule)
        assertWire("55ffff7fff", schedule { latitude = -Float.MAX_VALUE }, decodeSchedule)
        assertWire("5501000000", schedule { latitude = Float.MIN_VALUE }, decodeSchedule)
        assertWire("550000807f", schedule { latitude = Float.POSITIVE_INFINITY }, decodeSchedule)
        val nan = schedule { latitude = Float.NaN }
        assertEquals("550000c07f", hex(nan.serializedData()))
        assertTrue(decodeSchedule(nan.serializedData()).latitude.isNaN())
        assertNotEquals(nan, decodeSchedule(nan.serializedData()))
        assertEquals(schedule { latitude = 0f }, schedule { latitude = -0f })
        assertEquals(schedule { latitude = 0f }.hashCode(), schedule { latitude = -0f }.hashCode())
    }

    @Test
    fun doubleEdgeValues() {
        val decode = { bytes: ByteArray -> CarServer_DrivingSetSpeedLimitAction(serializedBytes = bytes) }
        fun limit(value: Double) = CarServer_DrivingSetSpeedLimitAction.with { limitMph = value }
        assertWire("", limit(0.0), decode)
        assertWire("09000000000000f03f", limit(1.0), decode)
        assertWire("09000000000000f0bf", limit(-1.0), decode)
        assertWire("090000000000000080", limit(-0.0), decode)
        assertWire("09ffffffffffffef7f", limit(Double.MAX_VALUE), decode)
        assertWire("090100000000000000", limit(Double.MIN_VALUE), decode)
    }

    @Test
    fun boolStringAndBytesValues() {
        assertWire("2001", schedule { startEnabled = true }, decodeSchedule)
        assertWire("", schedule { startEnabled = false }, decodeSchedule)
        assertTrue(decodeSchedule(unhex("2002")).startEnabled)
        assertWire("120348656d", schedule { name = "Hem" }, decodeSchedule)
        assertWire("1206c3a5c3a4c3b6", schedule { name = "åäö" }, decodeSchedule)
        assertWire("1204f09f9a97", schedule { name = "🚗" }, decodeSchedule)
        assertWire("", schedule { name = "" }, decodeSchedule)
        val request = UniversalMessage_SessionInfoRequest.with { publicKey = byteArrayOf(0, -1) }
        assertWire("0a0200ff", request, { UniversalMessage_SessionInfoRequest(serializedBytes = it) })
    }

    @Test
    fun allScalarKindsTogether() {
        val full = schedule {
            id = 7uL
            name = "Work"
            daysOfWeek = 31
            startEnabled = true
            startTime = 420
            endEnabled = true
            endTime = -1
            oneTime = true
            enabled = true
            latitude = 1f
            longitude = -1f
        }
        val expected = "0807" + "1204576f726b" + "181f" + "2001" + "28a403" + "3001" + "38ffffffffffffffffff01" + "4001" +
            "4801" + "550000803f" + "5d000080bf"
        assertWire(expected, full, decodeSchedule)
    }

    @Test
    fun enumValues() {
        val decode = { bytes: ByteArray -> VCSEC_ClosureMoveRequest(serializedBytes = bytes) }
        assertWire("2803", VCSEC_ClosureMoveRequest.with { rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeOpen }, decode)
        assertWire("", VCSEC_ClosureMoveRequest.with { rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeNone }, decode)
    }

    @Test
    fun unknownEnumValuesRoundTrip() {
        val decoded = VCSEC_ClosureMoveRequest(serializedBytes = unhex("2807"))
        assertEquals(VCSEC_ClosureMoveType_E.UNRECOGNIZED, decoded.rearTrunk)
        assertEquals("2807", hex(decoded.unknownFields))
        assertEquals("2807", hex(decoded.serializedData()))
        val negative = VCSEC_ClosureMoveRequest(serializedBytes = unhex("28ffffffffffffffffff01"))
        assertEquals(VCSEC_ClosureMoveType_E.UNRECOGNIZED, negative.rearTrunk)
        assertEquals("28ffffffffffffffffff01", hex(negative.serializedData()))
        val status = UniversalMessage_MessageStatus(serializedBytes = unhex("0809" + "1001"))
        assertEquals(UniversalMessage_OperationStatus_E.UNRECOGNIZED, status.operationStatus)
        assertEquals(UniversalMessage_MessageFault_E.rrorBusy, status.signedMessageFault)
        assertEquals("1001" + "0809", hex(status.serializedData()))
        assertEquals(status, UniversalMessage_MessageStatus(serializedBytes = status.serializedData()))
        val set = VCSEC_ClosureMoveRequest()
        set.rearTrunk = VCSEC_ClosureMoveType_E.UNRECOGNIZED
        assertEquals("", hex(set.serializedData()))
        val destination = UniversalMessage_Destination(serializedBytes = unhex("0807"))
        assertEquals(UniversalMessage_Domain.UNRECOGNIZED, destination.domain)
        assertTrue(destination.subDestination is UniversalMessage_Destination.OneOf_SubDestination.domain)
        assertEquals("0807", hex(destination.serializedData()))
    }

    @Test
    fun unrecognizedEnumValuesNeverOverrideNewerValues() {
        val decode = { bytes: ByteArray -> VCSEC_ClosureMoveRequest(serializedBytes = bytes) }
        val open = VCSEC_ClosureMoveRequest.with { rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeOpen }
        val set = decode(unhex("2807"))
        set.rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeOpen
        assertEquals("", hex(set.unknownFields))
        assertEquals("2803", hex(set.serializedData()))
        assertEquals(open, set)
        assertEquals(open.hashCode(), set.hashCode())
        val knownLast = decode(unhex("2807" + "2803"))
        assertEquals(VCSEC_ClosureMoveType_E.closureMoveTypeOpen, knownLast.rearTrunk)
        assertEquals("2803", hex(knownLast.serializedData()))
        assertEquals(open, knownLast)
        val unknownLast = decode(unhex("2803" + "2807"))
        assertEquals(VCSEC_ClosureMoveType_E.UNRECOGNIZED, unknownLast.rearTrunk)
        assertEquals("2807", hex(unknownLast.serializedData()))
        assertEquals("2809", hex(decode(unhex("2807" + "2809")).serializedData()))
        val merged = decode(unhex("2807"))
        merged.merge(unhex("2803"))
        assertEquals("2803", hex(merged.serializedData()))
        merged.merge(unhex("2809"))
        assertEquals(VCSEC_ClosureMoveType_E.UNRECOGNIZED, merged.rearTrunk)
        assertEquals("2809", hex(merged.serializedData()))
        val kept = decode(unhex("2807" + "980605"))
        kept.rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeClose
        assertEquals("2804" + "980605", hex(kept.serializedData()))
        val status = UniversalMessage_MessageStatus(serializedBytes = unhex("0809" + "1063"))
        status.operationStatus = UniversalMessage_OperationStatus_E.operationstatusWait
        assertEquals("0801" + "1063", hex(status.serializedData()))
    }

    @Test
    fun unrecognizedEnumOneofCasesNeverOverrideNewerCases() {
        val decode = { bytes: ByteArray -> UniversalMessage_Destination(serializedBytes = bytes) }
        val switched = decode(unhex("0807"))
        switched.routingAddress = byteArrayOf(1)
        assertEquals("120101", hex(switched.serializedData()))
        val reread = decode(switched.serializedData()).subDestination
        assertTrue(reread is UniversalMessage_Destination.OneOf_SubDestination.routingAddress)
        assertEquals(UniversalMessage_Destination.with { routingAddress = byteArrayOf(1) }, switched)
        assertEquals("120101", hex(decode(unhex("0807" + "120101")).serializedData()))
        val unknownLast = decode(unhex("120101" + "0807"))
        assertEquals(UniversalMessage_Domain.UNRECOGNIZED, unknownLast.domain)
        assertEquals("0807", hex(unknownLast.serializedData()))
        val cleared = decode(unhex("0807"))
        cleared.subDestination = null
        assertEquals("", hex(cleared.serializedData()))
        val known = decode(unhex("0807"))
        known.domain = UniversalMessage_Domain.infotainment
        assertEquals("0803", hex(known.serializedData()))
        val nested = UniversalMessage_RoutableMessage(serializedBytes = unhex("3202" + "0807"))
        nested.toDestination.routingAddress = byteArrayOf(2)
        assertEquals("3203" + "120102", hex(nested.serializedData()))
        val media = CarServer_MediaState(serializedBytes = unhex("4063"))
        assertTrue(media.optionalNowPlayingSource is CarServer_MediaState.OneOf_OptionalNowPlayingSource.nowPlayingSource)
        assertEquals("4063", hex(media.serializedData()))
        media.optionalNowPlayingSource = null
        assertEquals("", hex(media.serializedData()))
    }

    @Test
    fun unrecognizedRepeatedEnumValuesFollowTheList() {
        val decode = { bytes: ByteArray -> CarServer_HvacSetPreconditioningMaxAction(serializedBytes = bytes) }
        val cleared = decode(unhex("1a020109"))
        cleared.manualOverrideMode.clear()
        assertEquals("", hex(cleared.serializedData()))
        assertEquals(CarServer_HvacSetPreconditioningMaxAction(), cleared)
        val replaced = decode(unhex("1a020109"))
        replaced.manualOverrideMode = mutableListOf(CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.doors)
        assertEquals("1a0102", hex(replaced.serializedData()))
        val partly = decode(unhex("1a020109"))
        partly.manualOverrideMode.removeAt(0)
        assertEquals("1809", hex(partly.serializedData()))
        val removed = decode(unhex("1a020109"))
        removed.manualOverrideMode.remove(CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.UNRECOGNIZED)
        assertEquals("1a0101", hex(removed.serializedData()))
    }

    @Test
    fun deeplyNestedGroupsThrowInsteadOfOverflowingTheStack() {
        val deep = ByteArray(200_000) { 0x0b } + ByteArray(200_000) { 0x0c }
        assertFailsWith<BinaryDecodingError> { UniversalMessage_RoutableMessage(serializedBytes = deep) }
        val inMessage = unhex("3aa09c01") + ByteArray(10_000) { 0x0b } + ByteArray(10_000) { 0x0c }
        assertFailsWith<BinaryDecodingError> { UniversalMessage_RoutableMessage(serializedBytes = inMessage) }
        val shallow = ByteArray(50) { 0x0b } + ByteArray(50) { 0x0c }
        assertEquals(hex(shallow), hex(UniversalMessage_RoutableMessage(serializedBytes = shallow).serializedData()))
    }

    @Test
    fun packedRepeatedEnums() {
        val decode = { bytes: ByteArray -> CarServer_HvacSetPreconditioningMaxAction(serializedBytes = bytes) }
        val action = CarServer_HvacSetPreconditioningMaxAction()
        action.on = true
        action.manualOverrideMode.add(CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.soc)
        action.manualOverrideMode.add(CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.doors)
        action.manualOverrideMode.add(CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.dogMode)
        assertWire("0801" + "1a03010200", action, decode)
        val unpacked = decode(unhex("0801" + "1801" + "1802" + "1a0100"))
        assertEquals(action, unpacked)
        assertEquals("0801" + "1a03010200", hex(unpacked.serializedData()))
        val unknown = decode(unhex("1a020109"))
        assertEquals(
            listOf(
                CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.soc,
                CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.UNRECOGNIZED,
            ),
            unknown.manualOverrideMode,
        )
        assertEquals("1809", hex(unknown.unknownFields))
        assertEquals("1a0101" + "1809", hex(unknown.serializedData()))
        assertEquals(unknown, decode(unknown.serializedData()))
        assertEquals("", hex(CarServer_HvacSetPreconditioningMaxAction().serializedData()))
    }

    @Test
    fun repeatedMessagesAndTimestamp() {
        val sites = CarServer_NearbyChargingSites()
        sites.timestamp = Google_Protobuf_Timestamp(seconds = 1_700_000_000, nanos = 5)
        sites.superchargers.add(CarServer_Superchargers.with { id = 1; name = "A" })
        sites.superchargers.add(CarServer_Superchargers.with { id = 2; siteClosed = true })
        sites.congestionSyncTimeUtcSecs = 60
        val expected = "0a08" + "0880e2cfaa06" + "1005" + "1a05" + "0801" + "5a0141" + "1a04" + "0802" + "6801" + "203c"
        assertWire(expected, sites, { CarServer_NearbyChargingSites(serializedBytes = it) })
        val decoded = CarServer_NearbyChargingSites(serializedBytes = unhex(expected))
        assertEquals(2, decoded.superchargers.size)
        assertEquals("A", decoded.superchargers[0].name)
        assertEquals(1_700_000_000L, decoded.timestamp.seconds)
        assertEquals(5, decoded.timestamp.nanos)
    }

    @Test
    fun unknownFieldsRoundTrip() {
        val known = "3202" + "0802"
        val unknown = "9806" + "05" + "a206" + "02aabb" + "ad06" + "01020304" + "b106" + "0102030405060708" +
            "bb06" + "0801" + "bc06"
        val message = UniversalMessage_RoutableMessage(serializedBytes = unhex(known + unknown))
        assertEquals(unknown, hex(message.unknownFields))
        assertEquals(known + unknown, hex(message.serializedData()))
        val reordered = UniversalMessage_RoutableMessage(serializedBytes = unhex("980605" + known))
        assertEquals(known + "980605", hex(reordered.serializedData()))
        assertEquals(reordered, UniversalMessage_RoutableMessage(serializedBytes = reordered.serializedData()))
        assertNotEquals(reordered, UniversalMessage_RoutableMessage(serializedBytes = unhex(known)))
        val empty = CarServer_Void(serializedBytes = unhex("0801" + "1203616263"))
        assertEquals("0801" + "1203616263", hex(empty.serializedData()))
        val mismatched = CarServer_ChargeSchedule(serializedBytes = unhex("1a0100"))
        assertEquals(0, mismatched.daysOfWeek)
        assertEquals("1a0100", hex(mismatched.serializedData()))
    }

    @Test
    fun oneofSwitchingAndLastOneWins() {
        val message = UniversalMessage_RoutableMessage()
        message.sessionInfoRequest.publicKey = byteArrayOf(1)
        assertTrue(message.payload is UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest)
        message.sessionInfo = byteArrayOf(2)
        assertTrue(message.payload is UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo)
        assertEquals(0, message.sessionInfoRequest.publicKey.size)
        assertEquals(0, message.protobufMessageAsBytes.size)
        assertEquals("7a0102", hex(message.serializedData()))
        message.payload = null
        assertEquals("", hex(message.serializedData()))
        val lastWins = UniversalMessage_RoutableMessage(serializedBytes = unhex("7a0102" + "520103"))
        assertContentEquals(byteArrayOf(3), lastWins.protobufMessageAsBytes)
        assertEquals("520103", hex(lastWins.serializedData()))
        val destination = UniversalMessage_Destination(serializedBytes = unhex("1201aa" + "0803"))
        assertEquals(UniversalMessage_Domain.infotainment, destination.domain)
        assertEquals(0, destination.routingAddress.size)
    }

    @Test
    fun repeatedMessageOneofCaseMerges() {
        val merged = UniversalMessage_RoutableMessage(serializedBytes = unhex("72030a01aa" + "72031201bb"))
        assertContentEquals(byteArrayOf(0xAA.toByte()), merged.sessionInfoRequest.publicKey)
        assertContentEquals(byteArrayOf(0xBB.toByte()), merged.sessionInfoRequest.challenge)
        assertEquals("72060a01aa1201bb", hex(merged.serializedData()))
        val replaced = UniversalMessage_RoutableMessage(serializedBytes = unhex("72030a01aa" + "7a0102" + "72031201bb"))
        assertEquals(0, replaced.sessionInfoRequest.publicKey.size)
        assertContentEquals(byteArrayOf(0xBB.toByte()), replaced.sessionInfoRequest.challenge)
    }

    @Test
    fun singularMessageFieldsMerge() {
        val message = UniversalMessage_RoutableMessage(serializedBytes = unhex("62020801" + "62021005" + "9a030101" + "9a030102"))
        assertEquals(UniversalMessage_OperationStatus_E.operationstatusWait, message.signedMessageStatus.operationStatus)
        assertEquals(UniversalMessage_MessageFault_E.rrorInvalidSignature, message.signedMessageStatus.signedMessageFault)
        assertContentEquals(byteArrayOf(2), message.uuid)
        assertEquals("620408011005" + "9a030102", hex(message.serializedData()))
        message.merge(unhex("62021011" + "a00301"))
        assertEquals(UniversalMessage_OperationStatus_E.operationstatusWait, message.signedMessageStatus.operationStatus)
        assertEquals(UniversalMessage_MessageFault_E.rrorTimeExpired, message.signedMessageStatus.signedMessageFault)
        assertEquals(1u, message.flags)
    }

    @Test
    fun malformedInputThrows() {
        val inputs = listOf(
            "08",
            "1205616263",
            "0e01",
            "0001",
            "0c",
            "08ffffffffffffffffffff01",
            "3205080261",
            "25010203",
            "0f",
            "ffffffffff1f01",
        )
        for (input in inputs) {
            assertFailsWith<BinaryDecodingError>(input) {
                UniversalMessage_RoutableMessage(serializedBytes = unhex(input))
            }
        }
        assertFailsWith<BinaryDecodingError> { CarServer_ChargeSchedule(serializedBytes = unhex("1202c328")) }
        assertFailsWith<BinaryDecodingError> { CarServer_ChargeSchedule(serializedBytes = unhex("5501")) }
        assertFailsWith<BinaryDecodingError> { CarServer_DrivingSetSpeedLimitAction(serializedBytes = unhex("0901020304")) }
        assertFailsWith<BinaryDecodingError> { CarServer_HvacSetPreconditioningMaxAction(serializedBytes = unhex("1a0301")) }
    }
}
