package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.merge
import com.moblin.android.platform.swiftprotobuf.serializedData
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TeslaProtobufMutationSuite {
    @Test
    fun multiLevelChainAttachesEveryLevel() {
        val message = UniversalMessage_RoutableMessage()
        message.signatureData.aesGcmPersonalizedData.epoch = byteArrayOf(5)
        val subSigData = message.subSigData
        assertTrue(subSigData is UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData)
        val sigType = subSigData.value.sigType
        assertTrue(sigType is Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData)
        assertContentEquals(byteArrayOf(5), sigType.value.epoch)
        assertEquals("6a052a030a0105", hex(message.serializedData()))
        message.signatureData.aesGcmPersonalizedData.counter = 5u
        assertEquals("6a072a050a01051805", hex(message.serializedData()))
        val counter = UniversalMessage_RoutableMessage()
        counter.signatureData.aesGcmPersonalizedData.counter = 5u
        assertEquals("6a042a021805", hex(counter.serializedData()))
    }

    @Test
    fun readsOfAbsentFieldsNeverSetPresence() {
        val message = UniversalMessage_RoutableMessage()
        assertEquals(UniversalMessage_Domain.broadcast, message.toDestination.domain)
        assertEquals(0, message.fromDestination.routingAddress.size)
        assertEquals(0u, message.signatureData.aesGcmPersonalizedData.counter)
        assertEquals(0, message.signatureData.signerIdentity.publicKey.size)
        assertEquals(0, message.sessionInfoRequest.publicKey.size)
        assertEquals(UniversalMessage_OperationStatus_E.operationstatusOk, message.signedMessageStatus.operationStatus)
        assertEquals(0, message.sessionInfo.size)
        assertFalse(message.hasToDestination)
        assertFalse(message.hasFromDestination)
        assertFalse(message.hasSignedMessageStatus)
        assertNull(message.payload)
        assertNull(message.subSigData)
        assertNull(message.toDestination.subDestination)
        assertEquals("", hex(message.serializedData()))
        assertEquals(UniversalMessage_RoutableMessage(), message)
        val action = CarServer_Action()
        assertFalse(action.vehicleAction.getVehicleData.hasGetChargeState)
        assertEquals(0, action.vehicleAction.hvacSeatHeaterActions.hvacSeatHeaterAction.size)
        assertNull(action.actionMsg)
        assertEquals("", hex(action.serializedData()))
        val sites = CarServer_NearbyChargingSites()
        assertEquals(0L, sites.timestamp.seconds)
        assertFalse(sites.hasTimestamp)
        assertEquals("", hex(sites.serializedData()))
    }

    @Test
    fun pendingDefaultsAreCachedAndBecomeTheStoredInstance() {
        val message = UniversalMessage_RoutableMessage()
        val pending = message.toDestination
        assertSame(pending, message.toDestination)
        assertFalse(message.hasToDestination)
        pending.domain = UniversalMessage_Domain.infotainment
        assertTrue(message.hasToDestination)
        assertSame(pending, message.toDestination)
        pending.domain = UniversalMessage_Domain.vehicleSecurity
        assertEquals(UniversalMessage_Domain.vehicleSecurity, message.toDestination.domain)
        assertEquals("32020802", hex(message.serializedData()))
        val request = message.sessionInfoRequest
        assertSame(request, message.sessionInfoRequest)
        request.challenge = byteArrayOf(1)
        assertSame(request, message.sessionInfoRequest)
        assertTrue(message.payload is UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest)
    }

    @Test
    fun readingAnotherOneofCaseDoesNotSwitchTheOneof() {
        val message = UniversalMessage_RoutableMessage()
        message.sessionInfo = byteArrayOf(9)
        val request = message.sessionInfoRequest
        assertEquals(0, request.publicKey.size)
        assertTrue(message.payload is UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo)
        assertEquals("7a0109", hex(message.serializedData()))
        request.publicKey = byteArrayOf(1)
        assertTrue(message.payload is UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest)
        assertEquals(0, message.sessionInfo.size)
        assertEquals("72030a0101", hex(message.serializedData()))
    }

    @Test
    fun pendingDefaultsDetachWhenTheFieldIsWritten() {
        val message = UniversalMessage_RoutableMessage()
        val stale = message.toDestination
        message.toDestination = UniversalMessage_Destination.with { domain = UniversalMessage_Domain.infotainment }
        stale.domain = UniversalMessage_Domain.vehicleSecurity
        assertEquals(UniversalMessage_Domain.infotainment, message.toDestination.domain)
        val staleRequest = message.sessionInfoRequest
        message.sessionInfoRequest = UniversalMessage_SessionInfoRequest.with { challenge = byteArrayOf(2) }
        staleRequest.publicKey = byteArrayOf(3)
        assertEquals(0, message.sessionInfoRequest.publicKey.size)
        val cleared = message.fromDestination
        message.merge(unhex("3a03" + "1201aa"))
        cleared.domain = UniversalMessage_Domain.infotainment
        assertContentEquals(byteArrayOf(0xAA.toByte()), message.fromDestination.routingAddress)
    }

    @Test
    fun threeLevelChainPropagatesToTheGrandparent() {
        val action = CarServer_Action()
        action.vehicleAction.getVehicleData.getChargeState = CarServer_GetChargeState()
        assertEquals("12040a021200", hex(action.serializedData()))
        val other = CarServer_Action()
        val vehicleAction = other.vehicleAction
        val getVehicleData = vehicleAction.getVehicleData
        assertNull(other.actionMsg)
        assertNull(vehicleAction.vehicleActionMsg)
        getVehicleData.getDriveState = CarServer_GetDriveState()
        assertNotNull(other.actionMsg)
        assertSame(vehicleAction, other.vehicleAction)
        assertSame(getVehicleData, other.vehicleAction.getVehicleData)
        assertEquals("12040a022200", hex(other.serializedData()))
    }

    @Test
    fun clearAndMergeOnAPendingDefaultAttachLikeSwift() {
        val message = UniversalMessage_RoutableMessage()
        message.signatureData.clearSignerIdentity()
        assertTrue(message.subSigData is UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData)
        assertEquals("6a00", hex(message.serializedData()))
        val merged = UniversalMessage_RoutableMessage()
        merged.sessionInfoRequest.merge(unhex("0a01aa"))
        assertContentEquals(byteArrayOf(0xAA.toByte()), merged.sessionInfoRequest.publicKey)
        assertEquals("72030a01aa", hex(merged.serializedData()))
        val same = UniversalMessage_RoutableMessage()
        same.toDestination = same.toDestination
        assertTrue(same.hasToDestination)
        assertEquals("3200", hex(same.serializedData()))
    }

    @Test
    fun assignmentCopiesAndReadsAreLive() {
        val destination = UniversalMessage_Destination()
        destination.domain = UniversalMessage_Domain.infotainment
        val message = UniversalMessage_RoutableMessage()
        message.toDestination = destination
        destination.domain = UniversalMessage_Domain.vehicleSecurity
        assertEquals(UniversalMessage_Domain.infotainment, message.toDestination.domain)
        val live = message.toDestination
        live.domain = UniversalMessage_Domain.broadcast
        assertEquals(UniversalMessage_Domain.broadcast, message.toDestination.domain)
        val request = UniversalMessage_SessionInfoRequest.with { publicKey = byteArrayOf(1) }
        message.payload = UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest(request)
        request.publicKey = byteArrayOf(2)
        assertContentEquals(byteArrayOf(1), message.sessionInfoRequest.publicKey)
        message.sessionInfoRequest = request
        request.publicKey = byteArrayOf(3)
        assertContentEquals(byteArrayOf(2), message.sessionInfoRequest.publicKey)
    }

    @Test
    fun copyIsDeep() {
        val message = UniversalMessage_RoutableMessage()
        message.toDestination.domain = UniversalMessage_Domain.infotainment
        message.signatureData.aesGcmPersonalizedData.counter = 1u
        message.sessionInfoRequest.publicKey = byteArrayOf(1)
        message.uuid = byteArrayOf(4)
        message.unknownFields = unhex("980605")
        val copy = message.copy()
        assertEquals(message, copy)
        assertEquals(message.hashCode(), copy.hashCode())
        assertNotSame(message.toDestination, copy.toDestination)
        copy.toDestination.domain = UniversalMessage_Domain.vehicleSecurity
        copy.signatureData.aesGcmPersonalizedData.counter = 2u
        copy.sessionInfoRequest.publicKey = byteArrayOf(2)
        assertEquals(UniversalMessage_Domain.infotainment, message.toDestination.domain)
        assertEquals(1u, message.signatureData.aesGcmPersonalizedData.counter)
        assertContentEquals(byteArrayOf(1), message.sessionInfoRequest.publicKey)
        assertNotEquals(message, copy)
        val sites = CarServer_NearbyChargingSites()
        sites.superchargers.add(CarServer_Superchargers.with { name = "A" })
        val sitesCopy = sites.copy()
        sitesCopy.superchargers[0].name = "B"
        sitesCopy.superchargers.add(CarServer_Superchargers())
        assertEquals("A", sites.superchargers[0].name)
        assertEquals(1, sites.superchargers.size)
    }

    @Test
    fun repeatedFieldsCopyOnInsertAndAttachPendingParents() {
        val item = CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction()
        item.seatHeaterHigh = CarServer_Void()
        item.carSeatFrontLeft = CarServer_Void()
        val action = CarServer_Action()
        action.vehicleAction.hvacSeatHeaterActions.hvacSeatHeaterAction.add(item)
        item.seatHeaterLow = CarServer_Void()
        assertEquals("1209a202060a042a003a00", hex(action.serializedData()))
        val decoded = CarServer_Action(serializedBytes = action.serializedData())
        assertEquals(action, decoded)
        val level = decoded.vehicleAction.hvacSeatHeaterActions.hvacSeatHeaterAction[0].seatHeaterLevel
        assertTrue(level is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterHigh)
        val emptied = CarServer_Action()
        emptied.vehicleAction.hvacSeatHeaterActions.hvacSeatHeaterAction.clear()
        assertEquals("1203a20200", hex(emptied.serializedData()))
        val replaced = CarServer_HvacSeatHeaterActions()
        val list = mutableListOf(item)
        replaced.hvacSeatHeaterAction = list
        list.add(item)
        item.seatHeaterOff = CarServer_Void()
        assertEquals(1, replaced.hvacSeatHeaterAction.size)
        val stored = replaced.hvacSeatHeaterAction[0].seatHeaterLevel
        assertTrue(stored is CarServer_HvacSeatHeaterActions.HvacSeatHeaterAction.OneOf_SeatHeaterLevel.seatHeaterLow)
    }

    @Test
    fun listsCanBeAppendedToThemselvesLikeSwiftArrays() {
        val sites = CarServer_NearbyChargingSites()
        sites.superchargers.add(CarServer_Superchargers.with { id = 1 })
        sites.superchargers.addAll(sites.superchargers)
        assertEquals(2, sites.superchargers.size)
        assertNotSame(sites.superchargers[0], sites.superchargers[1])
        sites.superchargers.addAll(1, sites.superchargers)
        assertEquals(4, sites.superchargers.size)
        sites.superchargers = sites.superchargers
        assertEquals(4, sites.superchargers.size)
        assertEquals("1a020801".repeat(4), hex(sites.serializedData()))
        val action = CarServer_HvacSetPreconditioningMaxAction()
        action.manualOverrideMode.add(CarServer_HvacSetPreconditioningMaxAction.ManualOverrideMode_E.soc)
        action.manualOverrideMode.addAll(action.manualOverrideMode)
        assertEquals("1a020101", hex(action.serializedData()))
    }

    @Test
    fun nestedTimestampMutationSetsPresence() {
        val sites = CarServer_NearbyChargingSites()
        sites.timestamp.seconds = 5
        assertTrue(sites.hasTimestamp)
        assertEquals("0a020805", hex(sites.serializedData()))
        sites.clearTimestamp()
        assertFalse(sites.hasTimestamp)
        assertEquals("", hex(sites.serializedData()))
    }

    @Test
    fun equalityCoversUnknownFieldsAndBuildersMatchSetters() {
        val built = UniversalMessage_RoutableMessage.with {
            toDestination.domain = UniversalMessage_Domain.vehicleSecurity
            uuid = byteArrayOf(1, 2)
        }
        val set = UniversalMessage_RoutableMessage()
        set.toDestination.domain = UniversalMessage_Domain.vehicleSecurity
        set.uuid = byteArrayOf(1, 2)
        assertEquals(built, set)
        assertEquals(built.hashCode(), set.hashCode())
        set.unknownFields = unhex("980605")
        assertNotEquals(built, set)
        assertEquals(setOf(built), setOf(UniversalMessage_RoutableMessage(serializedBytes = built.serializedData())))
        assertEquals(
            UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo(byteArrayOf(1)),
            UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo(byteArrayOf(1)),
        )
        assertNotEquals<Any>(
            UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo(byteArrayOf(1)),
            UniversalMessage_RoutableMessage.OneOf_Payload.protobufMessageAsBytes(byteArrayOf(1)),
        )
    }
}
