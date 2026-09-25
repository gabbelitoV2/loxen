package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.serializedData
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

internal fun unhex(text: String): ByteArray {
    val digits = text.replace(" ", "")
    return ByteArray(digits.length / 2) { digits.substring(2 * it, 2 * it + 2).toInt(16).toByte() }
}

internal fun byteRun(first: Int, count: Int): ByteArray = ByteArray(count) { (first + it).toByte() }

@RunWith(RobolectricTestRunner::class)
class TeslaProtobufSuite {
    private val address = byteRun(0xA0, 16)
    private val uuid = byteRun(0x10, 16)
    private val publicKey = byteArrayOf(0x04) + byteRun(0x01, 64)

    @Test
    fun encodesSessionInfoRequestLikeTeslaVehicle() {
        val message = UniversalMessage_RoutableMessage()
        message.toDestination.domain = UniversalMessage_Domain.vehicleSecurity
        message.fromDestination.routingAddress = address
        message.sessionInfoRequest.publicKey = publicKey
        message.uuid = uuid
        val expected = "32020802" + "3a121210" + hex(address) + "72430a41" + hex(publicKey) + "9a0310" + hex(uuid)
        assertEquals(expected, hex(message.serializedData()))
        val decoded = UniversalMessage_RoutableMessage(serializedBytes = unhex(expected))
        assertEquals(message, decoded)
        assertEquals(message.hashCode(), decoded.hashCode())
        assertEquals(UniversalMessage_Domain.vehicleSecurity, decoded.toDestination.domain)
        val subDestination = decoded.fromDestination.subDestination
        val routingAddress = (subDestination as? UniversalMessage_Destination.OneOf_SubDestination.routingAddress)?.value
        assertNotNull(routingAddress)
        assertContentEquals(address, routingAddress)
        assertContentEquals(publicKey, decoded.sessionInfoRequest.publicKey)
        assertTrue(decoded.payload is UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest)
        assertContentEquals(uuid, decoded.uuid)
    }

    @Test
    fun encodesSignedRequestLikeTeslaVehicleSign() {
        val epoch = byteRun(0x30, 16)
        val nonce = byteRun(0x50, 12)
        val tag = byteRun(0x70, 16)
        val ciphertext = byteRun(0x90, 6)
        val message = UniversalMessage_RoutableMessage()
        message.toDestination.domain = UniversalMessage_Domain.infotainment
        message.fromDestination.routingAddress = address
        message.uuid = uuid
        message.flags = 1u shl UniversalMessage_Flags.flagEncryptResponse.rawValue
        message.signatureData.signerIdentity.publicKey = publicKey
        message.signatureData.aesGcmPersonalizedData.epoch = epoch
        message.signatureData.aesGcmPersonalizedData.counter = 7u
        message.signatureData.aesGcmPersonalizedData.expiresAt = 1000u
        message.signatureData.aesGcmPersonalizedData.nonce = nonce
        message.signatureData.aesGcmPersonalizedData.tag = tag
        message.protobufMessageAsBytes = ciphertext
        val personalized = "0a10" + hex(epoch) + "120c" + hex(nonce) + "1807" + "25e8030000" + "2a10" + hex(tag)
        val signatureData = "0a430a41" + hex(publicKey) + "2a39" + personalized
        val expected = "32020803" + "3a121210" + hex(address) + "5206" + hex(ciphertext) + "6a8001" + signatureData +
            "9a0310" + hex(uuid) + "a00302"
        assertEquals(expected, hex(message.serializedData()))
        val decoded = UniversalMessage_RoutableMessage(serializedBytes = unhex(expected))
        assertEquals(message, decoded)
        assertEquals(7u, decoded.signatureData.aesGcmPersonalizedData.counter)
        assertEquals(1000u, decoded.signatureData.aesGcmPersonalizedData.expiresAt)
        assertContentEquals(tag, decoded.signatureData.aesGcmPersonalizedData.tag)
        assertEquals(2u, decoded.flags)
    }

    @Test
    fun encodesSignedWhitelistOperationLikeAddKeyRequestWithRole() {
        val message = VCSEC_UnsignedMessage()
        message.whitelistOperation.addKeyToWhitelistAndAddPermissions.key.publicKeyRaw = publicKey
        message.whitelistOperation.addKeyToWhitelistAndAddPermissions.keyRole = Keys_Role.owner
        message.whitelistOperation.metadataForKey.keyFormFactor = VCSEC_KeyFormFactor.cloudKey
        val encoded = message.serializedData()
        val unsigned = "82014d" + "2a47" + "0a430a41" + hex(publicKey) + "2002" + "32020809"
        assertEquals(unsigned, hex(encoded))
        val envelope = VCSEC_ToVCSECMessage()
        envelope.signedMessage.protobufMessageAsBytes = encoded
        envelope.signedMessage.signatureType = VCSEC_SignatureType.presentKey
        val expected = "0a54" + "1250" + unsigned + "1802"
        assertEquals(expected, hex(envelope.serializedData()))
        val decodedEnvelope = VCSEC_ToVCSECMessage(serializedBytes = unhex(expected))
        assertEquals(envelope, decodedEnvelope)
        assertEquals(VCSEC_SignatureType.presentKey, decodedEnvelope.signedMessage.signatureType)
        val decoded = VCSEC_UnsignedMessage(serializedBytes = decodedEnvelope.signedMessage.protobufMessageAsBytes)
        assertEquals(message, decoded)
        assertEquals(Keys_Role.owner, decoded.whitelistOperation.addKeyToWhitelistAndAddPermissions.keyRole)
        assertContentEquals(publicKey, decoded.whitelistOperation.addKeyToWhitelistAndAddPermissions.key.publicKeyRaw)
        assertEquals(VCSEC_KeyFormFactor.cloudKey, decoded.whitelistOperation.metadataForKey.keyFormFactor)
    }

    @Test
    fun encodesClosureMoveRequestLikeOpenTrunk() {
        val closureMoveRequest = VCSEC_ClosureMoveRequest()
        closureMoveRequest.rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeOpen
        val unsignedMessage = VCSEC_UnsignedMessage()
        unsignedMessage.closureMoveRequest = closureMoveRequest
        assertEquals("22022803", hex(unsignedMessage.serializedData()))
        closureMoveRequest.rearTrunk = VCSEC_ClosureMoveType_E.closureMoveTypeClose
        assertEquals("22022803", hex(unsignedMessage.serializedData()))
        assertEquals(VCSEC_UnsignedMessage(serializedBytes = unhex("22022803")), unsignedMessage)
    }

    @Test
    fun encodesCarServerActions() {
        val getChargeState = CarServer_Action()
        getChargeState.vehicleAction.getVehicleData.getChargeState = CarServer_GetChargeState()
        assertEquals("12040a021200", hex(getChargeState.serializedData()))
        val honk = CarServer_Action()
        honk.vehicleAction.vehicleControlHonkHornAction = CarServer_VehicleControlHonkHornAction()
        assertEquals("1203da0100", hex(honk.serializedData()))
        val flashLights = CarServer_Action()
        flashLights.vehicleAction.vehicleControlFlashLightsAction = CarServer_VehicleControlFlashLightsAction()
        assertEquals("1203d20100", hex(flashLights.serializedData()))
        val play = CarServer_Action()
        play.vehicleAction.mediaPlayAction = CarServer_MediaPlayAction()
        assertEquals("12027a00", hex(play.serializedData()))
        val nextTrack = CarServer_Action()
        nextTrack.vehicleAction.mediaNextTrack = CarServer_MediaNextTrack()
        assertEquals("12039a0100", hex(nextTrack.serializedData()))
        for (action in listOf(getChargeState, honk, flashLights, play, nextTrack)) {
            assertEquals(action, CarServer_Action(serializedBytes = action.serializedData()))
        }
        val decoded = CarServer_Action(serializedBytes = unhex("12040a021200"))
        assertTrue(decoded.vehicleAction.getVehicleData.hasGetChargeState)
        assertFalse(decoded.vehicleAction.getVehicleData.hasGetDriveState)
    }

    @Test
    fun decodesChargeStateResponse() {
        val bytes = unhex("0a00" + "120b" + "1a09" + "900750" + "d0070b" + "f0082d")
        val response = CarServer_Response(serializedBytes = bytes)
        assertTrue(response.hasActionStatus)
        assertEquals(CarServer_OperationStatus_E.operationstatusOk, response.actionStatus.result)
        val chargeState = response.vehicleData.chargeState
        assertNotNull(chargeState.optionalBatteryLevel)
        assertEquals(80, chargeState.batteryLevel)
        assertEquals(11, chargeState.chargerPower)
        assertNotNull(chargeState.optionalMinutesToChargeLimit)
        assertEquals(45, chargeState.minutesToChargeLimit)
        assertEquals(hex(bytes), hex(response.serializedData()))
        val built = CarServer_Response()
        built.actionStatus = CarServer_ActionStatus()
        built.vehicleData.chargeState.batteryLevel = 80
        built.vehicleData.chargeState.chargerPower = 11
        built.vehicleData.chargeState.minutesToChargeLimit = 45
        assertEquals(response, built)
    }

    @Test
    fun decodesDriveAndMediaStateLikeModelTesla() {
        val drive = CarServer_DriveState(
            serializedBytes = unhex("0a022a00" + "b00641" + "b806fbffffffffffffffff01")
        )
        val shift = drive.shiftState.type
        assertTrue(shift is CarServer_ShiftState.OneOf_Type.d)
        val speed = (drive.optionalSpeed as? CarServer_DriveState.OneOf_OptionalSpeed.speed)?.value
        assertEquals(65u, speed)
        val power = (drive.optionalPower as? CarServer_DriveState.OneOf_OptionalPower.power)?.value
        assertEquals(-5, power)
        val text = when (shift) {
            is CarServer_ShiftState.OneOf_Type.invalid -> "-"
            is CarServer_ShiftState.OneOf_Type.p -> "P"
            is CarServer_ShiftState.OneOf_Type.r -> "R"
            is CarServer_ShiftState.OneOf_Type.n -> "N"
            is CarServer_ShiftState.OneOf_Type.d -> "D"
            is CarServer_ShiftState.OneOf_Type.sna -> "SNA"
            null -> "?"
        }
        assertEquals("D", text)
        val media = CarServer_MediaState()
        media.nowPlayingArtist = "Artist"
        media.nowPlayingTitle = "Title"
        val decodedMedia = CarServer_MediaState(serializedBytes = media.serializedData())
        val artist = (decodedMedia.optionalNowPlayingArtist as? CarServer_MediaState.OneOf_OptionalNowPlayingArtist.nowPlayingArtist)
        val title = (decodedMedia.optionalNowPlayingTitle as? CarServer_MediaState.OneOf_OptionalNowPlayingTitle.nowPlayingTitle)
        assertEquals("Artist", artist?.value)
        assertEquals("Title", title?.value)
        assertEquals("1a06" + hex("Artist".encodeToByteArray()) + "2205" + hex("Title".encodeToByteArray()), hex(media.serializedData()))
    }

    @Test
    fun decodesSessionInfo() {
        val sessionInfo = Signatures_SessionInfo()
        sessionInfo.counter = 3u
        sessionInfo.publicKey = publicKey
        sessionInfo.epoch = byteRun(0x30, 16)
        sessionInfo.clockTime = 0x01020304u
        sessionInfo.status = Signatures_Session_Info_Status.keyNotOnWhitelist
        sessionInfo.handle = 9u
        val expected = "0803" + "1241" + hex(publicKey) + "1a10" + hex(byteRun(0x30, 16)) + "2504030201" + "2801" + "3009"
        assertEquals(expected, hex(sessionInfo.serializedData()))
        val decoded = Signatures_SessionInfo(serializedBytes = unhex(expected))
        assertEquals(sessionInfo, decoded)
        decoded.counter += 1u
        assertEquals(4u, decoded.counter)
    }

    @Test
    fun enumsKeepRawValuesAndCases() {
        assertEquals(255, Signatures_Tag.end.rawValue)
        assertEquals(Signatures_Tag.end, Signatures_Tag.fromRawValue(255))
        assertEquals(Signatures_Tag.UNRECOGNIZED, Signatures_Tag.fromRawValue(254))
        assertEquals(11, Signatures_Tag.allCases.size)
        assertFalse(Signatures_Tag.UNRECOGNIZED in Signatures_Tag.allCases)
        assertEquals(UniversalMessage_Domain.broadcast, UniversalMessage_Domain.allCases.first())
        assertEquals(5, Signatures_SignatureType.aesGcmPersonalized.rawValue)
        assertEquals(9, Signatures_SignatureType.aesGcmResponse.rawValue)
        assertEquals(UniversalMessage_MessageFault_E.rrorNone, UniversalMessage_MessageStatus().signedMessageFault)
        assertEquals("UniversalMessage.RoutableMessage", UniversalMessage_RoutableMessage.protoMessageName)
        assertEquals("CarServer.ClosuresState.SunRoofState", CarServer_ClosuresState.SunRoofState.protoMessageName)
    }
}
