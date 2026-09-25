package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecoder
import com.moblin.android.platform.swiftprotobuf.BinaryEncodingVisitor
import com.moblin.android.platform.swiftprotobuf.GeneratedMessage
import com.moblin.android.platform.swiftprotobuf.ProtobufList
import com.moblin.android.platform.swiftprotobuf.ProtobufOneofCase
import com.moblin.android.platform.swiftprotobuf.merge

enum class VCSEC_SignatureType(val rawValue: Int) {
    none(0),
    presentKey(2),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_SignatureType> = listOf(
            none,
            presentKey,
        )

        fun fromRawValue(rawValue: Int): VCSEC_SignatureType =
            when (rawValue) {
                0 -> none
                2 -> presentKey
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_KeyFormFactor(val rawValue: Int) {
    unknown(0),
    nfcCard(1),
    iosDevice(6),
    androidDevice(7),
    cloudKey(9),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_KeyFormFactor> = listOf(
            unknown,
            nfcCard,
            iosDevice,
            androidDevice,
            cloudKey,
        )

        fun fromRawValue(rawValue: Int): VCSEC_KeyFormFactor =
            when (rawValue) {
                0 -> unknown
                1 -> nfcCard
                6 -> iosDevice
                7 -> androidDevice
                9 -> cloudKey
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_InformationRequestType(val rawValue: Int) {
    getStatus(0),
    getWhitelistInfo(5),
    getWhitelistEntryInfo(6),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_InformationRequestType> = listOf(
            getStatus,
            getWhitelistInfo,
            getWhitelistEntryInfo,
        )

        fun fromRawValue(rawValue: Int): VCSEC_InformationRequestType =
            when (rawValue) {
                0 -> getStatus
                5 -> getWhitelistInfo
                6 -> getWhitelistEntryInfo
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_RKEAction_E(val rawValue: Int) {
    rkeActionUnlock(0),
    rkeActionLock(1),
    rkeActionRemoteDrive(20),
    rkeActionAutoSecureVehicle(29),
    rkeActionWakeVehicle(30),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_RKEAction_E> = listOf(
            rkeActionUnlock,
            rkeActionLock,
            rkeActionRemoteDrive,
            rkeActionAutoSecureVehicle,
            rkeActionWakeVehicle,
        )

        fun fromRawValue(rawValue: Int): VCSEC_RKEAction_E =
            when (rawValue) {
                0 -> rkeActionUnlock
                1 -> rkeActionLock
                20 -> rkeActionRemoteDrive
                29 -> rkeActionAutoSecureVehicle
                30 -> rkeActionWakeVehicle
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_ClosureMoveType_E(val rawValue: Int) {
    closureMoveTypeNone(0),
    closureMoveTypeMove(1),
    closureMoveTypeStop(2),
    closureMoveTypeOpen(3),
    closureMoveTypeClose(4),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_ClosureMoveType_E> = listOf(
            closureMoveTypeNone,
            closureMoveTypeMove,
            closureMoveTypeStop,
            closureMoveTypeOpen,
            closureMoveTypeClose,
        )

        fun fromRawValue(rawValue: Int): VCSEC_ClosureMoveType_E =
            when (rawValue) {
                0 -> closureMoveTypeNone
                1 -> closureMoveTypeMove
                2 -> closureMoveTypeStop
                3 -> closureMoveTypeOpen
                4 -> closureMoveTypeClose
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_OperationStatus_E(val rawValue: Int) {
    operationstatusOk(0),
    operationstatusWait(1),
    rror(2),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_OperationStatus_E> = listOf(
            operationstatusOk,
            operationstatusWait,
            rror,
        )

        fun fromRawValue(rawValue: Int): VCSEC_OperationStatus_E =
            when (rawValue) {
                0 -> operationstatusOk
                1 -> operationstatusWait
                2 -> rror
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_SignedMessage_information_E(val rawValue: Int) {
    signedmessageInformationNone(0),
    signedmessageInformationFaultUnknown(1),
    signedmessageInformationFaultNotOnWhitelist(2),
    signedmessageInformationFaultIvSmallerThanExpected(3),
    signedmessageInformationFaultInvalidToken(4),
    signedmessageInformationFaultTokenAndCounterInvalid(5),
    signedmessageInformationFaultAesDecryptAuth(6),
    signedmessageInformationFaultEcdsaInput(7),
    signedmessageInformationFaultEcdsaSignature(8),
    signedmessageInformationFaultLocalEntityStart(9),
    signedmessageInformationFaultLocalEntityResult(10),
    signedmessageInformationFaultCouldNotRetrieveKey(11),
    signedmessageInformationFaultCouldNotRetrieveToken(12),
    signedmessageInformationFaultSignatureTooShort(13),
    signedmessageInformationFaultTokenIsIncorrectLength(14),
    signedmessageInformationFaultIncorrectEpoch(15),
    signedmessageInformationFaultIvIncorrectLength(16),
    signedmessageInformationFaultTimeExpired(17),
    signedmessageInformationFaultNotProvisionedWithIdentity(18),
    signedmessageInformationFaultCouldNotHashMetadata(19),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_SignedMessage_information_E> = listOf(
            signedmessageInformationNone,
            signedmessageInformationFaultUnknown,
            signedmessageInformationFaultNotOnWhitelist,
            signedmessageInformationFaultIvSmallerThanExpected,
            signedmessageInformationFaultInvalidToken,
            signedmessageInformationFaultTokenAndCounterInvalid,
            signedmessageInformationFaultAesDecryptAuth,
            signedmessageInformationFaultEcdsaInput,
            signedmessageInformationFaultEcdsaSignature,
            signedmessageInformationFaultLocalEntityStart,
            signedmessageInformationFaultLocalEntityResult,
            signedmessageInformationFaultCouldNotRetrieveKey,
            signedmessageInformationFaultCouldNotRetrieveToken,
            signedmessageInformationFaultSignatureTooShort,
            signedmessageInformationFaultTokenIsIncorrectLength,
            signedmessageInformationFaultIncorrectEpoch,
            signedmessageInformationFaultIvIncorrectLength,
            signedmessageInformationFaultTimeExpired,
            signedmessageInformationFaultNotProvisionedWithIdentity,
            signedmessageInformationFaultCouldNotHashMetadata,
        )

        fun fromRawValue(rawValue: Int): VCSEC_SignedMessage_information_E =
            when (rawValue) {
                0 -> signedmessageInformationNone
                1 -> signedmessageInformationFaultUnknown
                2 -> signedmessageInformationFaultNotOnWhitelist
                3 -> signedmessageInformationFaultIvSmallerThanExpected
                4 -> signedmessageInformationFaultInvalidToken
                5 -> signedmessageInformationFaultTokenAndCounterInvalid
                6 -> signedmessageInformationFaultAesDecryptAuth
                7 -> signedmessageInformationFaultEcdsaInput
                8 -> signedmessageInformationFaultEcdsaSignature
                9 -> signedmessageInformationFaultLocalEntityStart
                10 -> signedmessageInformationFaultLocalEntityResult
                11 -> signedmessageInformationFaultCouldNotRetrieveKey
                12 -> signedmessageInformationFaultCouldNotRetrieveToken
                13 -> signedmessageInformationFaultSignatureTooShort
                14 -> signedmessageInformationFaultTokenIsIncorrectLength
                15 -> signedmessageInformationFaultIncorrectEpoch
                16 -> signedmessageInformationFaultIvIncorrectLength
                17 -> signedmessageInformationFaultTimeExpired
                18 -> signedmessageInformationFaultNotProvisionedWithIdentity
                19 -> signedmessageInformationFaultCouldNotHashMetadata
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_WhitelistOperation_information_E(val rawValue: Int) {
    whitelistoperationInformationNone(0),
    whitelistoperationInformationUndocumentedError(1),
    whitelistoperationInformationNoPermissionToRemoveOneself(2),
    whitelistoperationInformationKeyfobSlotsFull(3),
    whitelistoperationInformationWhitelistFull(4),
    whitelistoperationInformationNoPermissionToAdd(5),
    whitelistoperationInformationInvalidPublicKey(6),
    whitelistoperationInformationNoPermissionToRemove(7),
    whitelistoperationInformationNoPermissionToChangePermissions(8),
    whitelistoperationInformationAttemptingToElevateOtherAboveOneself(9),
    whitelistoperationInformationAttemptingToDemoteSuperiorToOneself(10),
    whitelistoperationInformationAttemptingToRemoveOwnPermissions(11),
    whitelistoperationInformationPublicKeyNotOnWhitelist(12),
    whitelistoperationInformationAttemptingToAddKeyThatIsAlreadyOnTheWhitelist(13),
    whitelistoperationInformationNotAllowedToAddUnlessOnReader(14),
    whitelistoperationInformationFmModifyingOutsideOfFMode(15),
    whitelistoperationInformationFmAttemptingToAddPermanentKey(16),
    whitelistoperationInformationFmAttemptingToRemovePermanentKey(17),
    whitelistoperationInformationKeychainWhileFsFull(18),
    whitelistoperationInformationAttemptingToAddKeyWithoutRole(19),
    whitelistoperationInformationAttemptingToAddKeyWithServiceRole(20),
    whitelistoperationInformationNonServiceKeyAttemptingToAddServiceTech(21),
    whitelistoperationInformationServiceKeyAttemptingToAddServiceTechOutsideServiceMode(22),
    whitelistoperationInformationCouldNotStartLocalEntityAuth(23),
    whitelistoperationInformationLocalEntityAuthFailedUiDenied(24),
    whitelistoperationInformationLocalEntityAuthFailedTimedOutWaitingForTap(25),
    whitelistoperationInformationLocalEntityAuthFailedTimedOutWaitingForUiAck(26),
    whitelistoperationInformationLocalEntityAuthFailedValetMode(27),
    whitelistoperationInformationLocalEntityAuthFailedCancelled(28),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_WhitelistOperation_information_E> = listOf(
            whitelistoperationInformationNone,
            whitelistoperationInformationUndocumentedError,
            whitelistoperationInformationNoPermissionToRemoveOneself,
            whitelistoperationInformationKeyfobSlotsFull,
            whitelistoperationInformationWhitelistFull,
            whitelistoperationInformationNoPermissionToAdd,
            whitelistoperationInformationInvalidPublicKey,
            whitelistoperationInformationNoPermissionToRemove,
            whitelistoperationInformationNoPermissionToChangePermissions,
            whitelistoperationInformationAttemptingToElevateOtherAboveOneself,
            whitelistoperationInformationAttemptingToDemoteSuperiorToOneself,
            whitelistoperationInformationAttemptingToRemoveOwnPermissions,
            whitelistoperationInformationPublicKeyNotOnWhitelist,
            whitelistoperationInformationAttemptingToAddKeyThatIsAlreadyOnTheWhitelist,
            whitelistoperationInformationNotAllowedToAddUnlessOnReader,
            whitelistoperationInformationFmModifyingOutsideOfFMode,
            whitelistoperationInformationFmAttemptingToAddPermanentKey,
            whitelistoperationInformationFmAttemptingToRemovePermanentKey,
            whitelistoperationInformationKeychainWhileFsFull,
            whitelistoperationInformationAttemptingToAddKeyWithoutRole,
            whitelistoperationInformationAttemptingToAddKeyWithServiceRole,
            whitelistoperationInformationNonServiceKeyAttemptingToAddServiceTech,
            whitelistoperationInformationServiceKeyAttemptingToAddServiceTechOutsideServiceMode,
            whitelistoperationInformationCouldNotStartLocalEntityAuth,
            whitelistoperationInformationLocalEntityAuthFailedUiDenied,
            whitelistoperationInformationLocalEntityAuthFailedTimedOutWaitingForTap,
            whitelistoperationInformationLocalEntityAuthFailedTimedOutWaitingForUiAck,
            whitelistoperationInformationLocalEntityAuthFailedValetMode,
            whitelistoperationInformationLocalEntityAuthFailedCancelled,
        )

        fun fromRawValue(rawValue: Int): VCSEC_WhitelistOperation_information_E =
            when (rawValue) {
                0 -> whitelistoperationInformationNone
                1 -> whitelistoperationInformationUndocumentedError
                2 -> whitelistoperationInformationNoPermissionToRemoveOneself
                3 -> whitelistoperationInformationKeyfobSlotsFull
                4 -> whitelistoperationInformationWhitelistFull
                5 -> whitelistoperationInformationNoPermissionToAdd
                6 -> whitelistoperationInformationInvalidPublicKey
                7 -> whitelistoperationInformationNoPermissionToRemove
                8 -> whitelistoperationInformationNoPermissionToChangePermissions
                9 -> whitelistoperationInformationAttemptingToElevateOtherAboveOneself
                10 -> whitelistoperationInformationAttemptingToDemoteSuperiorToOneself
                11 -> whitelistoperationInformationAttemptingToRemoveOwnPermissions
                12 -> whitelistoperationInformationPublicKeyNotOnWhitelist
                13 -> whitelistoperationInformationAttemptingToAddKeyThatIsAlreadyOnTheWhitelist
                14 -> whitelistoperationInformationNotAllowedToAddUnlessOnReader
                15 -> whitelistoperationInformationFmModifyingOutsideOfFMode
                16 -> whitelistoperationInformationFmAttemptingToAddPermanentKey
                17 -> whitelistoperationInformationFmAttemptingToRemovePermanentKey
                18 -> whitelistoperationInformationKeychainWhileFsFull
                19 -> whitelistoperationInformationAttemptingToAddKeyWithoutRole
                20 -> whitelistoperationInformationAttemptingToAddKeyWithServiceRole
                21 -> whitelistoperationInformationNonServiceKeyAttemptingToAddServiceTech
                22 -> whitelistoperationInformationServiceKeyAttemptingToAddServiceTechOutsideServiceMode
                23 -> whitelistoperationInformationCouldNotStartLocalEntityAuth
                24 -> whitelistoperationInformationLocalEntityAuthFailedUiDenied
                25 -> whitelistoperationInformationLocalEntityAuthFailedTimedOutWaitingForTap
                26 -> whitelistoperationInformationLocalEntityAuthFailedTimedOutWaitingForUiAck
                27 -> whitelistoperationInformationLocalEntityAuthFailedValetMode
                28 -> whitelistoperationInformationLocalEntityAuthFailedCancelled
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_ClosureState_E(val rawValue: Int) {
    closurestateClosed(0),
    closurestateOpen(1),
    closurestateAjar(2),
    closurestateUnknown(3),
    closurestateFailedUnlatch(4),
    closurestateOpening(5),
    closurestateClosing(6),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_ClosureState_E> = listOf(
            closurestateClosed,
            closurestateOpen,
            closurestateAjar,
            closurestateUnknown,
            closurestateFailedUnlatch,
            closurestateOpening,
            closurestateClosing,
        )

        fun fromRawValue(rawValue: Int): VCSEC_ClosureState_E =
            when (rawValue) {
                0 -> closurestateClosed
                1 -> closurestateOpen
                2 -> closurestateAjar
                3 -> closurestateUnknown
                4 -> closurestateFailedUnlatch
                5 -> closurestateOpening
                6 -> closurestateClosing
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_VehicleLockState_E(val rawValue: Int) {
    vehiclelockstateUnlocked(0),
    vehiclelockstateLocked(1),
    vehiclelockstateInternalLocked(2),
    vehiclelockstateSelectiveUnlocked(3),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_VehicleLockState_E> = listOf(
            vehiclelockstateUnlocked,
            vehiclelockstateLocked,
            vehiclelockstateInternalLocked,
            vehiclelockstateSelectiveUnlocked,
        )

        fun fromRawValue(rawValue: Int): VCSEC_VehicleLockState_E =
            when (rawValue) {
                0 -> vehiclelockstateUnlocked
                1 -> vehiclelockstateLocked
                2 -> vehiclelockstateInternalLocked
                3 -> vehiclelockstateSelectiveUnlocked
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_VehicleSleepStatus_E(val rawValue: Int) {
    vehicleSleepStatusUnknown(0),
    vehicleSleepStatusAwake(1),
    vehicleSleepStatusAsleep(2),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_VehicleSleepStatus_E> = listOf(
            vehicleSleepStatusUnknown,
            vehicleSleepStatusAwake,
            vehicleSleepStatusAsleep,
        )

        fun fromRawValue(rawValue: Int): VCSEC_VehicleSleepStatus_E =
            when (rawValue) {
                0 -> vehicleSleepStatusUnknown
                1 -> vehicleSleepStatusAwake
                2 -> vehicleSleepStatusAsleep
                else -> UNRECOGNIZED
            }
    }
}

enum class VCSEC_UserPresence_E(val rawValue: Int) {
    vehicleUserPresenceUnknown(0),
    vehicleUserPresenceNotPresent(1),
    vehicleUserPresencePresent(2),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<VCSEC_UserPresence_E> = listOf(
            vehicleUserPresenceUnknown,
            vehicleUserPresenceNotPresent,
            vehicleUserPresencePresent,
        )

        fun fromRawValue(rawValue: Int): VCSEC_UserPresence_E =
            when (rawValue) {
                0 -> vehicleUserPresenceUnknown
                1 -> vehicleUserPresenceNotPresent
                2 -> vehicleUserPresencePresent
                else -> UNRECOGNIZED
            }
    }
}

class VCSEC_SignedMessage() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var protobufMessageAsBytes: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var signatureType: VCSEC_SignatureType = VCSEC_SignatureType.none
        set(value) {
            field = value
            _protobufForgetUnrecognized(3)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                2 -> decoder.decodeSingularBytesField()?.let { this.protobufMessageAsBytes = it }
                3 -> decoder.decodeSingularOpenEnumField(VCSEC_SignatureType.UNRECOGNIZED) { VCSEC_SignatureType.fromRawValue(it) }
                    ?.let { this.signatureType = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.protobufMessageAsBytes.isNotEmpty()) {
            visitor.visitSingularBytesField(this.protobufMessageAsBytes, 2)
        }
        if (this.signatureType != VCSEC_SignatureType.none && this.signatureType != VCSEC_SignatureType.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.signatureType.rawValue, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_SignedMessage) return false
        if (!this.protobufMessageAsBytes.contentEquals(other.protobufMessageAsBytes)) return false
        if (this.signatureType != other.signatureType) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.protobufMessageAsBytes.contentHashCode()
        hash = 31 * hash + this.signatureType.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_SignedMessage {
        val result = VCSEC_SignedMessage()
        result.protobufMessageAsBytes = this.protobufMessageAsBytes
        result.signatureType = this.signatureType
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.SignedMessage"

        fun with(block: VCSEC_SignedMessage.() -> Unit): VCSEC_SignedMessage =
            VCSEC_SignedMessage().apply(block)
    }
}

class VCSEC_ToVCSECMessage() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var signedMessage: VCSEC_SignedMessage
        get() = this._signedMessage ?: _protobufPending(1, { VCSEC_SignedMessage() }) { this._signedMessage = it }
        set(value) {
            _protobufDropPending(1)
            this._signedMessage = value.copy()
            _protobufMutated()
        }

    val hasSignedMessage: Boolean
        get() = this._signedMessage != null

    fun clearSignedMessage() {
        _protobufDropPending(1)
        this._signedMessage = null
        _protobufMutated()
    }

    private var _signedMessage: VCSEC_SignedMessage? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._signedMessage) { VCSEC_SignedMessage() }?.let {
                    _protobufDropPending(1)
                    this._signedMessage = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._signedMessage?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_ToVCSECMessage) return false
        if (this._signedMessage != other._signedMessage) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._signedMessage?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_ToVCSECMessage {
        val result = VCSEC_ToVCSECMessage()
        result._signedMessage = this._signedMessage?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.ToVCSECMessage"

        fun with(block: VCSEC_ToVCSECMessage.() -> Unit): VCSEC_ToVCSECMessage =
            VCSEC_ToVCSECMessage().apply(block)
    }
}

class VCSEC_KeyIdentifier() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var publicKeySha1: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBytesField()?.let { this.publicKeySha1 = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.publicKeySha1.isNotEmpty()) {
            visitor.visitSingularBytesField(this.publicKeySha1, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_KeyIdentifier) return false
        if (!this.publicKeySha1.contentEquals(other.publicKeySha1)) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.publicKeySha1.contentHashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_KeyIdentifier {
        val result = VCSEC_KeyIdentifier()
        result.publicKeySha1 = this.publicKeySha1
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.KeyIdentifier"

        fun with(block: VCSEC_KeyIdentifier.() -> Unit): VCSEC_KeyIdentifier =
            VCSEC_KeyIdentifier().apply(block)
    }
}

class VCSEC_KeyMetadata() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var keyFormFactor: VCSEC_KeyFormFactor = VCSEC_KeyFormFactor.unknown
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(VCSEC_KeyFormFactor.UNRECOGNIZED) { VCSEC_KeyFormFactor.fromRawValue(it) }
                    ?.let { this.keyFormFactor = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.keyFormFactor != VCSEC_KeyFormFactor.unknown && this.keyFormFactor != VCSEC_KeyFormFactor.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.keyFormFactor.rawValue, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_KeyMetadata) return false
        if (this.keyFormFactor != other.keyFormFactor) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.keyFormFactor.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_KeyMetadata {
        val result = VCSEC_KeyMetadata()
        result.keyFormFactor = this.keyFormFactor
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.KeyMetadata"

        fun with(block: VCSEC_KeyMetadata.() -> Unit): VCSEC_KeyMetadata =
            VCSEC_KeyMetadata().apply(block)
    }
}

class VCSEC_PublicKey() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var publicKeyRaw: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBytesField()?.let { this.publicKeyRaw = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.publicKeyRaw.isNotEmpty()) {
            visitor.visitSingularBytesField(this.publicKeyRaw, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_PublicKey) return false
        if (!this.publicKeyRaw.contentEquals(other.publicKeyRaw)) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.publicKeyRaw.contentHashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_PublicKey {
        val result = VCSEC_PublicKey()
        result.publicKeyRaw = this.publicKeyRaw
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.PublicKey"

        fun with(block: VCSEC_PublicKey.() -> Unit): VCSEC_PublicKey =
            VCSEC_PublicKey().apply(block)
    }
}

class VCSEC_WhitelistInfo() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var numberOfEntries: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var whitelistEntries: MutableList<VCSEC_KeyIdentifier>
        get() = this._whitelistEntries
        set(value) {
            this._whitelistEntries = ProtobufList<VCSEC_KeyIdentifier>(this) { it.copy() }.apply { addAll(value) }
            _protobufMutated()
        }

    var slotMask: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    private var _whitelistEntries: ProtobufList<VCSEC_KeyIdentifier> = ProtobufList<VCSEC_KeyIdentifier>(this) { it.copy() }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularUInt32Field()?.let { this.numberOfEntries = it }
                2 -> decoder.decodeRepeatedMessageField(this._whitelistEntries) { VCSEC_KeyIdentifier() }
                3 -> decoder.decodeSingularUInt32Field()?.let { this.slotMask = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.numberOfEntries != 0u) {
            visitor.visitSingularUInt32Field(this.numberOfEntries, 1)
        }
        if (this._whitelistEntries.isNotEmpty()) {
            visitor.visitRepeatedMessageField(this._whitelistEntries, 2)
        }
        if (this.slotMask != 0u) {
            visitor.visitSingularUInt32Field(this.slotMask, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_WhitelistInfo) return false
        if (this.numberOfEntries != other.numberOfEntries) return false
        if (this._whitelistEntries != other._whitelistEntries) return false
        if (this.slotMask != other.slotMask) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.numberOfEntries.hashCode()
        hash = 31 * hash + this._whitelistEntries.hashCode()
        hash = 31 * hash + this.slotMask.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_WhitelistInfo {
        val result = VCSEC_WhitelistInfo()
        result.numberOfEntries = this.numberOfEntries
        result._whitelistEntries.addAll(this._whitelistEntries)
        result.slotMask = this.slotMask
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.WhitelistInfo"

        fun with(block: VCSEC_WhitelistInfo.() -> Unit): VCSEC_WhitelistInfo =
            VCSEC_WhitelistInfo().apply(block)
    }
}

class VCSEC_WhitelistEntryInfo() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var keyID: VCSEC_KeyIdentifier
        get() = this._keyID ?: _protobufPending(1, { VCSEC_KeyIdentifier() }) { this._keyID = it }
        set(value) {
            _protobufDropPending(1)
            this._keyID = value.copy()
            _protobufMutated()
        }

    val hasKeyID: Boolean
        get() = this._keyID != null

    fun clearKeyID() {
        _protobufDropPending(1)
        this._keyID = null
        _protobufMutated()
    }

    var publicKey: VCSEC_PublicKey
        get() = this._publicKey ?: _protobufPending(2, { VCSEC_PublicKey() }) { this._publicKey = it }
        set(value) {
            _protobufDropPending(2)
            this._publicKey = value.copy()
            _protobufMutated()
        }

    val hasPublicKey: Boolean
        get() = this._publicKey != null

    fun clearPublicKey() {
        _protobufDropPending(2)
        this._publicKey = null
        _protobufMutated()
    }

    var metadataForKey: VCSEC_KeyMetadata
        get() = this._metadataForKey ?: _protobufPending(4, { VCSEC_KeyMetadata() }) { this._metadataForKey = it }
        set(value) {
            _protobufDropPending(4)
            this._metadataForKey = value.copy()
            _protobufMutated()
        }

    val hasMetadataForKey: Boolean
        get() = this._metadataForKey != null

    fun clearMetadataForKey() {
        _protobufDropPending(4)
        this._metadataForKey = null
        _protobufMutated()
    }

    var slot: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var keyRole: Keys_Role = Keys_Role.none
        set(value) {
            field = value
            _protobufForgetUnrecognized(7)
            _protobufMutated()
        }

    private var _keyID: VCSEC_KeyIdentifier? = null
    private var _publicKey: VCSEC_PublicKey? = null
    private var _metadataForKey: VCSEC_KeyMetadata? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._keyID) { VCSEC_KeyIdentifier() }?.let {
                    _protobufDropPending(1)
                    this._keyID = it
                }
                2 -> decoder.decodeSingularMessageField(this._publicKey) { VCSEC_PublicKey() }?.let {
                    _protobufDropPending(2)
                    this._publicKey = it
                }
                4 -> decoder.decodeSingularMessageField(this._metadataForKey) { VCSEC_KeyMetadata() }?.let {
                    _protobufDropPending(4)
                    this._metadataForKey = it
                }
                6 -> decoder.decodeSingularUInt32Field()?.let { this.slot = it }
                7 -> decoder.decodeSingularOpenEnumField(Keys_Role.UNRECOGNIZED) { Keys_Role.fromRawValue(it) }
                    ?.let { this.keyRole = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._keyID?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        this._publicKey?.let {
            visitor.visitSingularMessageField(it, 2)
        }
        this._metadataForKey?.let {
            visitor.visitSingularMessageField(it, 4)
        }
        if (this.slot != 0u) {
            visitor.visitSingularUInt32Field(this.slot, 6)
        }
        if (this.keyRole != Keys_Role.none && this.keyRole != Keys_Role.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.keyRole.rawValue, 7)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_WhitelistEntryInfo) return false
        if (this._keyID != other._keyID) return false
        if (this._publicKey != other._publicKey) return false
        if (this._metadataForKey != other._metadataForKey) return false
        if (this.slot != other.slot) return false
        if (this.keyRole != other.keyRole) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._keyID?.hashCode() ?: 0)
        hash = 31 * hash + (this._publicKey?.hashCode() ?: 0)
        hash = 31 * hash + (this._metadataForKey?.hashCode() ?: 0)
        hash = 31 * hash + this.slot.hashCode()
        hash = 31 * hash + this.keyRole.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_WhitelistEntryInfo {
        val result = VCSEC_WhitelistEntryInfo()
        result._keyID = this._keyID?.copy()
        result._publicKey = this._publicKey?.copy()
        result._metadataForKey = this._metadataForKey?.copy()
        result.slot = this.slot
        result.keyRole = this.keyRole
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.WhitelistEntryInfo"

        fun with(block: VCSEC_WhitelistEntryInfo.() -> Unit): VCSEC_WhitelistEntryInfo =
            VCSEC_WhitelistEntryInfo().apply(block)
    }
}

class VCSEC_InformationRequest() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var informationRequestType: VCSEC_InformationRequestType = VCSEC_InformationRequestType.getStatus
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var key: VCSEC_InformationRequest.OneOf_Key?
        get() = this._key
        set(value) {
            this._protobufStore_key(this._protobufCopy_key(value))
            _protobufMutated()
        }

    var keyID: VCSEC_KeyIdentifier
        get() = (this._key as? VCSEC_InformationRequest.OneOf_Key.keyID)?.value
            ?: _protobufPending(2, { VCSEC_KeyIdentifier() }) { this._protobufStore_key(VCSEC_InformationRequest.OneOf_Key.keyID(it)) }
        set(value) {
            this.key = VCSEC_InformationRequest.OneOf_Key.keyID(value)
        }

    var publicKey: ByteArray
        get() = (this._key as? VCSEC_InformationRequest.OneOf_Key.publicKey)?.value ?: ByteArray(0)
        set(value) {
            this.key = VCSEC_InformationRequest.OneOf_Key.publicKey(value)
        }

    var slot: UInt
        get() = (this._key as? VCSEC_InformationRequest.OneOf_Key.slot)?.value ?: 0u
        set(value) {
            this.key = VCSEC_InformationRequest.OneOf_Key.slot(value)
        }

    sealed class OneOf_Key(value: Any) : ProtobufOneofCase(value) {
        class keyID(val value: VCSEC_KeyIdentifier) : OneOf_Key(value)
        class publicKey(val value: ByteArray) : OneOf_Key(value)
        class slot(val value: UInt) : OneOf_Key(value)
    }

    private var _key: VCSEC_InformationRequest.OneOf_Key? = null

    private fun _protobufStore_key(value: VCSEC_InformationRequest.OneOf_Key?) {
        _protobufDropPending(2)
        this._key = value
    }

    private fun _protobufCopy_key(value: VCSEC_InformationRequest.OneOf_Key?): VCSEC_InformationRequest.OneOf_Key? =
        when (value) {
            is VCSEC_InformationRequest.OneOf_Key.keyID -> VCSEC_InformationRequest.OneOf_Key.keyID(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(VCSEC_InformationRequestType.UNRECOGNIZED) { VCSEC_InformationRequestType.fromRawValue(it) }
                    ?.let { this.informationRequestType = it }
                2 -> decoder.decodeSingularMessageField((this._key as? VCSEC_InformationRequest.OneOf_Key.keyID)?.value) { VCSEC_KeyIdentifier() }
                    ?.let { this._protobufStore_key(VCSEC_InformationRequest.OneOf_Key.keyID(it)) }
                3 -> decoder.decodeSingularBytesField()?.let { this._protobufStore_key(VCSEC_InformationRequest.OneOf_Key.publicKey(it)) }
                4 -> decoder.decodeSingularUInt32Field()?.let { this._protobufStore_key(VCSEC_InformationRequest.OneOf_Key.slot(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.informationRequestType != VCSEC_InformationRequestType.getStatus && this.informationRequestType != VCSEC_InformationRequestType.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.informationRequestType.rawValue, 1)
        }
        (this._key as? VCSEC_InformationRequest.OneOf_Key.keyID)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._key as? VCSEC_InformationRequest.OneOf_Key.publicKey)?.let {
            visitor.visitSingularBytesField(it.value, 3)
        }
        (this._key as? VCSEC_InformationRequest.OneOf_Key.slot)?.let {
            visitor.visitSingularUInt32Field(it.value, 4)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_InformationRequest) return false
        if (this.informationRequestType != other.informationRequestType) return false
        if (this._key != other._key) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.informationRequestType.hashCode()
        hash = 31 * hash + (this._key?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_InformationRequest {
        val result = VCSEC_InformationRequest()
        result.informationRequestType = this.informationRequestType
        result._key = this._protobufCopy_key(this._key)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.InformationRequest"

        fun with(block: VCSEC_InformationRequest.() -> Unit): VCSEC_InformationRequest =
            VCSEC_InformationRequest().apply(block)
    }
}

class VCSEC_ClosureMoveRequest() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var frontDriverDoor: VCSEC_ClosureMoveType_E = VCSEC_ClosureMoveType_E.closureMoveTypeNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var frontPassengerDoor: VCSEC_ClosureMoveType_E = VCSEC_ClosureMoveType_E.closureMoveTypeNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(2)
            _protobufMutated()
        }

    var rearDriverDoor: VCSEC_ClosureMoveType_E = VCSEC_ClosureMoveType_E.closureMoveTypeNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(3)
            _protobufMutated()
        }

    var rearPassengerDoor: VCSEC_ClosureMoveType_E = VCSEC_ClosureMoveType_E.closureMoveTypeNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(4)
            _protobufMutated()
        }

    var rearTrunk: VCSEC_ClosureMoveType_E = VCSEC_ClosureMoveType_E.closureMoveTypeNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(5)
            _protobufMutated()
        }

    var frontTrunk: VCSEC_ClosureMoveType_E = VCSEC_ClosureMoveType_E.closureMoveTypeNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(6)
            _protobufMutated()
        }

    var chargePort: VCSEC_ClosureMoveType_E = VCSEC_ClosureMoveType_E.closureMoveTypeNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(7)
            _protobufMutated()
        }

    var tonneau: VCSEC_ClosureMoveType_E = VCSEC_ClosureMoveType_E.closureMoveTypeNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(8)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureMoveType_E.UNRECOGNIZED) { VCSEC_ClosureMoveType_E.fromRawValue(it) }
                    ?.let { this.frontDriverDoor = it }
                2 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureMoveType_E.UNRECOGNIZED) { VCSEC_ClosureMoveType_E.fromRawValue(it) }
                    ?.let { this.frontPassengerDoor = it }
                3 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureMoveType_E.UNRECOGNIZED) { VCSEC_ClosureMoveType_E.fromRawValue(it) }
                    ?.let { this.rearDriverDoor = it }
                4 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureMoveType_E.UNRECOGNIZED) { VCSEC_ClosureMoveType_E.fromRawValue(it) }
                    ?.let { this.rearPassengerDoor = it }
                5 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureMoveType_E.UNRECOGNIZED) { VCSEC_ClosureMoveType_E.fromRawValue(it) }
                    ?.let { this.rearTrunk = it }
                6 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureMoveType_E.UNRECOGNIZED) { VCSEC_ClosureMoveType_E.fromRawValue(it) }
                    ?.let { this.frontTrunk = it }
                7 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureMoveType_E.UNRECOGNIZED) { VCSEC_ClosureMoveType_E.fromRawValue(it) }
                    ?.let { this.chargePort = it }
                8 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureMoveType_E.UNRECOGNIZED) { VCSEC_ClosureMoveType_E.fromRawValue(it) }
                    ?.let { this.tonneau = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.frontDriverDoor != VCSEC_ClosureMoveType_E.closureMoveTypeNone && this.frontDriverDoor != VCSEC_ClosureMoveType_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.frontDriverDoor.rawValue, 1)
        }
        if (this.frontPassengerDoor != VCSEC_ClosureMoveType_E.closureMoveTypeNone && this.frontPassengerDoor != VCSEC_ClosureMoveType_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.frontPassengerDoor.rawValue, 2)
        }
        if (this.rearDriverDoor != VCSEC_ClosureMoveType_E.closureMoveTypeNone && this.rearDriverDoor != VCSEC_ClosureMoveType_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.rearDriverDoor.rawValue, 3)
        }
        if (this.rearPassengerDoor != VCSEC_ClosureMoveType_E.closureMoveTypeNone && this.rearPassengerDoor != VCSEC_ClosureMoveType_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.rearPassengerDoor.rawValue, 4)
        }
        if (this.rearTrunk != VCSEC_ClosureMoveType_E.closureMoveTypeNone && this.rearTrunk != VCSEC_ClosureMoveType_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.rearTrunk.rawValue, 5)
        }
        if (this.frontTrunk != VCSEC_ClosureMoveType_E.closureMoveTypeNone && this.frontTrunk != VCSEC_ClosureMoveType_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.frontTrunk.rawValue, 6)
        }
        if (this.chargePort != VCSEC_ClosureMoveType_E.closureMoveTypeNone && this.chargePort != VCSEC_ClosureMoveType_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.chargePort.rawValue, 7)
        }
        if (this.tonneau != VCSEC_ClosureMoveType_E.closureMoveTypeNone && this.tonneau != VCSEC_ClosureMoveType_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.tonneau.rawValue, 8)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_ClosureMoveRequest) return false
        if (this.frontDriverDoor != other.frontDriverDoor) return false
        if (this.frontPassengerDoor != other.frontPassengerDoor) return false
        if (this.rearDriverDoor != other.rearDriverDoor) return false
        if (this.rearPassengerDoor != other.rearPassengerDoor) return false
        if (this.rearTrunk != other.rearTrunk) return false
        if (this.frontTrunk != other.frontTrunk) return false
        if (this.chargePort != other.chargePort) return false
        if (this.tonneau != other.tonneau) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.frontDriverDoor.hashCode()
        hash = 31 * hash + this.frontPassengerDoor.hashCode()
        hash = 31 * hash + this.rearDriverDoor.hashCode()
        hash = 31 * hash + this.rearPassengerDoor.hashCode()
        hash = 31 * hash + this.rearTrunk.hashCode()
        hash = 31 * hash + this.frontTrunk.hashCode()
        hash = 31 * hash + this.chargePort.hashCode()
        hash = 31 * hash + this.tonneau.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_ClosureMoveRequest {
        val result = VCSEC_ClosureMoveRequest()
        result.frontDriverDoor = this.frontDriverDoor
        result.frontPassengerDoor = this.frontPassengerDoor
        result.rearDriverDoor = this.rearDriverDoor
        result.rearPassengerDoor = this.rearPassengerDoor
        result.rearTrunk = this.rearTrunk
        result.frontTrunk = this.frontTrunk
        result.chargePort = this.chargePort
        result.tonneau = this.tonneau
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.ClosureMoveRequest"

        fun with(block: VCSEC_ClosureMoveRequest.() -> Unit): VCSEC_ClosureMoveRequest =
            VCSEC_ClosureMoveRequest().apply(block)
    }
}

class VCSEC_PermissionChange() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var key: VCSEC_PublicKey
        get() = this._key ?: _protobufPending(1, { VCSEC_PublicKey() }) { this._key = it }
        set(value) {
            _protobufDropPending(1)
            this._key = value.copy()
            _protobufMutated()
        }

    val hasKey: Boolean
        get() = this._key != null

    fun clearKey() {
        _protobufDropPending(1)
        this._key = null
        _protobufMutated()
    }

    var secondsToBeActive: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var keyRole: Keys_Role = Keys_Role.none
        set(value) {
            field = value
            _protobufForgetUnrecognized(4)
            _protobufMutated()
        }

    private var _key: VCSEC_PublicKey? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._key) { VCSEC_PublicKey() }?.let {
                    _protobufDropPending(1)
                    this._key = it
                }
                3 -> decoder.decodeSingularUInt32Field()?.let { this.secondsToBeActive = it }
                4 -> decoder.decodeSingularOpenEnumField(Keys_Role.UNRECOGNIZED) { Keys_Role.fromRawValue(it) }
                    ?.let { this.keyRole = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._key?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        if (this.secondsToBeActive != 0u) {
            visitor.visitSingularUInt32Field(this.secondsToBeActive, 3)
        }
        if (this.keyRole != Keys_Role.none && this.keyRole != Keys_Role.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.keyRole.rawValue, 4)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_PermissionChange) return false
        if (this._key != other._key) return false
        if (this.secondsToBeActive != other.secondsToBeActive) return false
        if (this.keyRole != other.keyRole) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._key?.hashCode() ?: 0)
        hash = 31 * hash + this.secondsToBeActive.hashCode()
        hash = 31 * hash + this.keyRole.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_PermissionChange {
        val result = VCSEC_PermissionChange()
        result._key = this._key?.copy()
        result.secondsToBeActive = this.secondsToBeActive
        result.keyRole = this.keyRole
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.PermissionChange"

        fun with(block: VCSEC_PermissionChange.() -> Unit): VCSEC_PermissionChange =
            VCSEC_PermissionChange().apply(block)
    }
}

class VCSEC_ReplaceKey() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var keyToReplace: VCSEC_ReplaceKey.OneOf_KeyToReplace?
        get() = this._keyToReplace
        set(value) {
            this._protobufStore_keyToReplace(this._protobufCopy_keyToReplace(value))
            _protobufMutated()
        }

    var publicKeyToReplace: VCSEC_PublicKey
        get() = (this._keyToReplace as? VCSEC_ReplaceKey.OneOf_KeyToReplace.publicKeyToReplace)?.value
            ?: _protobufPending(1, { VCSEC_PublicKey() }) { this._protobufStore_keyToReplace(VCSEC_ReplaceKey.OneOf_KeyToReplace.publicKeyToReplace(it)) }
        set(value) {
            this.keyToReplace = VCSEC_ReplaceKey.OneOf_KeyToReplace.publicKeyToReplace(value)
        }

    var slotToReplace: UInt
        get() = (this._keyToReplace as? VCSEC_ReplaceKey.OneOf_KeyToReplace.slotToReplace)?.value ?: 0u
        set(value) {
            this.keyToReplace = VCSEC_ReplaceKey.OneOf_KeyToReplace.slotToReplace(value)
        }

    var keyToAdd: VCSEC_PublicKey
        get() = this._keyToAdd ?: _protobufPending(3, { VCSEC_PublicKey() }) { this._keyToAdd = it }
        set(value) {
            _protobufDropPending(3)
            this._keyToAdd = value.copy()
            _protobufMutated()
        }

    val hasKeyToAdd: Boolean
        get() = this._keyToAdd != null

    fun clearKeyToAdd() {
        _protobufDropPending(3)
        this._keyToAdd = null
        _protobufMutated()
    }

    var keyRole: Keys_Role = Keys_Role.none
        set(value) {
            field = value
            _protobufForgetUnrecognized(4)
            _protobufMutated()
        }

    var impermanent: Boolean = false
        set(value) {
            field = value
            _protobufMutated()
        }

    sealed class OneOf_KeyToReplace(value: Any) : ProtobufOneofCase(value) {
        class publicKeyToReplace(val value: VCSEC_PublicKey) : OneOf_KeyToReplace(value)
        class slotToReplace(val value: UInt) : OneOf_KeyToReplace(value)
    }

    private var _keyToReplace: VCSEC_ReplaceKey.OneOf_KeyToReplace? = null
    private var _keyToAdd: VCSEC_PublicKey? = null

    private fun _protobufStore_keyToReplace(value: VCSEC_ReplaceKey.OneOf_KeyToReplace?) {
        _protobufDropPending(1)
        this._keyToReplace = value
    }

    private fun _protobufCopy_keyToReplace(value: VCSEC_ReplaceKey.OneOf_KeyToReplace?): VCSEC_ReplaceKey.OneOf_KeyToReplace? =
        when (value) {
            is VCSEC_ReplaceKey.OneOf_KeyToReplace.publicKeyToReplace -> VCSEC_ReplaceKey.OneOf_KeyToReplace.publicKeyToReplace(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._keyToReplace as? VCSEC_ReplaceKey.OneOf_KeyToReplace.publicKeyToReplace)?.value) { VCSEC_PublicKey() }
                    ?.let { this._protobufStore_keyToReplace(VCSEC_ReplaceKey.OneOf_KeyToReplace.publicKeyToReplace(it)) }
                2 -> decoder.decodeSingularUInt32Field()?.let { this._protobufStore_keyToReplace(VCSEC_ReplaceKey.OneOf_KeyToReplace.slotToReplace(it)) }
                3 -> decoder.decodeSingularMessageField(this._keyToAdd) { VCSEC_PublicKey() }?.let {
                    _protobufDropPending(3)
                    this._keyToAdd = it
                }
                4 -> decoder.decodeSingularOpenEnumField(Keys_Role.UNRECOGNIZED) { Keys_Role.fromRawValue(it) }
                    ?.let { this.keyRole = it }
                5 -> decoder.decodeSingularBoolField()?.let { this.impermanent = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._keyToReplace as? VCSEC_ReplaceKey.OneOf_KeyToReplace.publicKeyToReplace)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._keyToReplace as? VCSEC_ReplaceKey.OneOf_KeyToReplace.slotToReplace)?.let {
            visitor.visitSingularUInt32Field(it.value, 2)
        }
        this._keyToAdd?.let {
            visitor.visitSingularMessageField(it, 3)
        }
        if (this.keyRole != Keys_Role.none && this.keyRole != Keys_Role.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.keyRole.rawValue, 4)
        }
        if (this.impermanent) {
            visitor.visitSingularBoolField(this.impermanent, 5)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_ReplaceKey) return false
        if (this._keyToReplace != other._keyToReplace) return false
        if (this._keyToAdd != other._keyToAdd) return false
        if (this.keyRole != other.keyRole) return false
        if (this.impermanent != other.impermanent) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._keyToReplace?.hashCode() ?: 0)
        hash = 31 * hash + (this._keyToAdd?.hashCode() ?: 0)
        hash = 31 * hash + this.keyRole.hashCode()
        hash = 31 * hash + this.impermanent.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_ReplaceKey {
        val result = VCSEC_ReplaceKey()
        result._keyToReplace = this._protobufCopy_keyToReplace(this._keyToReplace)
        result._keyToAdd = this._keyToAdd?.copy()
        result.keyRole = this.keyRole
        result.impermanent = this.impermanent
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.ReplaceKey"

        fun with(block: VCSEC_ReplaceKey.() -> Unit): VCSEC_ReplaceKey =
            VCSEC_ReplaceKey().apply(block)
    }
}

class VCSEC_WhitelistOperation() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var subMessage: VCSEC_WhitelistOperation.OneOf_SubMessage?
        get() = this._subMessage
        set(value) {
            this._protobufStore_subMessage(this._protobufCopy_subMessage(value))
            _protobufMutated()
        }

    var addPublicKeyToWhitelist: VCSEC_PublicKey
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addPublicKeyToWhitelist)?.value
            ?: _protobufPending(1, { VCSEC_PublicKey() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addPublicKeyToWhitelist(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.addPublicKeyToWhitelist(value)
        }

    var removePublicKeyFromWhitelist: VCSEC_PublicKey
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.removePublicKeyFromWhitelist)?.value
            ?: _protobufPending(2, { VCSEC_PublicKey() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.removePublicKeyFromWhitelist(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.removePublicKeyFromWhitelist(value)
        }

    var addPermissionsToPublicKey: VCSEC_PermissionChange
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addPermissionsToPublicKey)?.value
            ?: _protobufPending(3, { VCSEC_PermissionChange() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addPermissionsToPublicKey(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.addPermissionsToPublicKey(value)
        }

    var removePermissionsFromPublicKey: VCSEC_PermissionChange
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.removePermissionsFromPublicKey)?.value
            ?: _protobufPending(4, { VCSEC_PermissionChange() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.removePermissionsFromPublicKey(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.removePermissionsFromPublicKey(value)
        }

    var addKeyToWhitelistAndAddPermissions: VCSEC_PermissionChange
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addKeyToWhitelistAndAddPermissions)?.value
            ?: _protobufPending(5, { VCSEC_PermissionChange() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addKeyToWhitelistAndAddPermissions(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.addKeyToWhitelistAndAddPermissions(value)
        }

    var updateKeyAndPermissions: VCSEC_PermissionChange
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.updateKeyAndPermissions)?.value
            ?: _protobufPending(7, { VCSEC_PermissionChange() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.updateKeyAndPermissions(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.updateKeyAndPermissions(value)
        }

    var addImpermanentKey: VCSEC_PermissionChange
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKey)?.value
            ?: _protobufPending(8, { VCSEC_PermissionChange() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKey(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKey(value)
        }

    var addImpermanentKeyAndRemoveExisting: VCSEC_PermissionChange
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKeyAndRemoveExisting)?.value
            ?: _protobufPending(9, { VCSEC_PermissionChange() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKeyAndRemoveExisting(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKeyAndRemoveExisting(value)
        }

    var removeAllImpermanentKeys: Boolean
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.removeAllImpermanentKeys)?.value ?: false
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.removeAllImpermanentKeys(value)
        }

    var replaceKey: VCSEC_ReplaceKey
        get() = (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.replaceKey)?.value
            ?: _protobufPending(17, { VCSEC_ReplaceKey() }) { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.replaceKey(it)) }
        set(value) {
            this.subMessage = VCSEC_WhitelistOperation.OneOf_SubMessage.replaceKey(value)
        }

    var metadataForKey: VCSEC_KeyMetadata
        get() = this._metadataForKey ?: _protobufPending(6, { VCSEC_KeyMetadata() }) { this._metadataForKey = it }
        set(value) {
            _protobufDropPending(6)
            this._metadataForKey = value.copy()
            _protobufMutated()
        }

    val hasMetadataForKey: Boolean
        get() = this._metadataForKey != null

    fun clearMetadataForKey() {
        _protobufDropPending(6)
        this._metadataForKey = null
        _protobufMutated()
    }

    sealed class OneOf_SubMessage(value: Any) : ProtobufOneofCase(value) {
        class addPublicKeyToWhitelist(val value: VCSEC_PublicKey) : OneOf_SubMessage(value)
        class removePublicKeyFromWhitelist(val value: VCSEC_PublicKey) : OneOf_SubMessage(value)
        class addPermissionsToPublicKey(val value: VCSEC_PermissionChange) : OneOf_SubMessage(value)
        class removePermissionsFromPublicKey(val value: VCSEC_PermissionChange) : OneOf_SubMessage(value)
        class addKeyToWhitelistAndAddPermissions(val value: VCSEC_PermissionChange) : OneOf_SubMessage(value)
        class updateKeyAndPermissions(val value: VCSEC_PermissionChange) : OneOf_SubMessage(value)
        class addImpermanentKey(val value: VCSEC_PermissionChange) : OneOf_SubMessage(value)
        class addImpermanentKeyAndRemoveExisting(val value: VCSEC_PermissionChange) : OneOf_SubMessage(value)
        class removeAllImpermanentKeys(val value: Boolean) : OneOf_SubMessage(value)
        class replaceKey(val value: VCSEC_ReplaceKey) : OneOf_SubMessage(value)
    }

    private var _subMessage: VCSEC_WhitelistOperation.OneOf_SubMessage? = null
    private var _metadataForKey: VCSEC_KeyMetadata? = null

    private fun _protobufStore_subMessage(value: VCSEC_WhitelistOperation.OneOf_SubMessage?) {
        _protobufDropPending(1)
        _protobufDropPending(2)
        _protobufDropPending(3)
        _protobufDropPending(4)
        _protobufDropPending(5)
        _protobufDropPending(7)
        _protobufDropPending(8)
        _protobufDropPending(9)
        _protobufDropPending(17)
        this._subMessage = value
    }

    private fun _protobufCopy_subMessage(value: VCSEC_WhitelistOperation.OneOf_SubMessage?): VCSEC_WhitelistOperation.OneOf_SubMessage? =
        when (value) {
            is VCSEC_WhitelistOperation.OneOf_SubMessage.addPublicKeyToWhitelist -> VCSEC_WhitelistOperation.OneOf_SubMessage.addPublicKeyToWhitelist(value.value.copy())
            is VCSEC_WhitelistOperation.OneOf_SubMessage.removePublicKeyFromWhitelist -> VCSEC_WhitelistOperation.OneOf_SubMessage.removePublicKeyFromWhitelist(value.value.copy())
            is VCSEC_WhitelistOperation.OneOf_SubMessage.addPermissionsToPublicKey -> VCSEC_WhitelistOperation.OneOf_SubMessage.addPermissionsToPublicKey(value.value.copy())
            is VCSEC_WhitelistOperation.OneOf_SubMessage.removePermissionsFromPublicKey -> VCSEC_WhitelistOperation.OneOf_SubMessage.removePermissionsFromPublicKey(value.value.copy())
            is VCSEC_WhitelistOperation.OneOf_SubMessage.addKeyToWhitelistAndAddPermissions -> VCSEC_WhitelistOperation.OneOf_SubMessage.addKeyToWhitelistAndAddPermissions(value.value.copy())
            is VCSEC_WhitelistOperation.OneOf_SubMessage.updateKeyAndPermissions -> VCSEC_WhitelistOperation.OneOf_SubMessage.updateKeyAndPermissions(value.value.copy())
            is VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKey -> VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKey(value.value.copy())
            is VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKeyAndRemoveExisting -> VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKeyAndRemoveExisting(value.value.copy())
            is VCSEC_WhitelistOperation.OneOf_SubMessage.replaceKey -> VCSEC_WhitelistOperation.OneOf_SubMessage.replaceKey(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addPublicKeyToWhitelist)?.value) { VCSEC_PublicKey() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addPublicKeyToWhitelist(it)) }
                2 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.removePublicKeyFromWhitelist)?.value) { VCSEC_PublicKey() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.removePublicKeyFromWhitelist(it)) }
                3 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addPermissionsToPublicKey)?.value) { VCSEC_PermissionChange() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addPermissionsToPublicKey(it)) }
                4 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.removePermissionsFromPublicKey)?.value) { VCSEC_PermissionChange() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.removePermissionsFromPublicKey(it)) }
                5 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addKeyToWhitelistAndAddPermissions)?.value) { VCSEC_PermissionChange() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addKeyToWhitelistAndAddPermissions(it)) }
                6 -> decoder.decodeSingularMessageField(this._metadataForKey) { VCSEC_KeyMetadata() }?.let {
                    _protobufDropPending(6)
                    this._metadataForKey = it
                }
                7 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.updateKeyAndPermissions)?.value) { VCSEC_PermissionChange() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.updateKeyAndPermissions(it)) }
                8 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKey)?.value) { VCSEC_PermissionChange() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKey(it)) }
                9 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKeyAndRemoveExisting)?.value) { VCSEC_PermissionChange() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKeyAndRemoveExisting(it)) }
                16 -> decoder.decodeSingularBoolField()?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.removeAllImpermanentKeys(it)) }
                17 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.replaceKey)?.value) { VCSEC_ReplaceKey() }
                    ?.let { this._protobufStore_subMessage(VCSEC_WhitelistOperation.OneOf_SubMessage.replaceKey(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addPublicKeyToWhitelist)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.removePublicKeyFromWhitelist)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addPermissionsToPublicKey)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.removePermissionsFromPublicKey)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addKeyToWhitelistAndAddPermissions)?.let {
            visitor.visitSingularMessageField(it.value, 5)
        }
        this._metadataForKey?.let {
            visitor.visitSingularMessageField(it, 6)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.updateKeyAndPermissions)?.let {
            visitor.visitSingularMessageField(it.value, 7)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKey)?.let {
            visitor.visitSingularMessageField(it.value, 8)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.addImpermanentKeyAndRemoveExisting)?.let {
            visitor.visitSingularMessageField(it.value, 9)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.removeAllImpermanentKeys)?.let {
            visitor.visitSingularBoolField(it.value, 16)
        }
        (this._subMessage as? VCSEC_WhitelistOperation.OneOf_SubMessage.replaceKey)?.let {
            visitor.visitSingularMessageField(it.value, 17)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_WhitelistOperation) return false
        if (this._subMessage != other._subMessage) return false
        if (this._metadataForKey != other._metadataForKey) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._subMessage?.hashCode() ?: 0)
        hash = 31 * hash + (this._metadataForKey?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_WhitelistOperation {
        val result = VCSEC_WhitelistOperation()
        result._subMessage = this._protobufCopy_subMessage(this._subMessage)
        result._metadataForKey = this._metadataForKey?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.WhitelistOperation"

        fun with(block: VCSEC_WhitelistOperation.() -> Unit): VCSEC_WhitelistOperation =
            VCSEC_WhitelistOperation().apply(block)
    }
}

class VCSEC_WhitelistOperation_status() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var whitelistOperationInformation: VCSEC_WhitelistOperation_information_E = VCSEC_WhitelistOperation_information_E.whitelistoperationInformationNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var signerOfOperation: VCSEC_KeyIdentifier
        get() = this._signerOfOperation ?: _protobufPending(2, { VCSEC_KeyIdentifier() }) { this._signerOfOperation = it }
        set(value) {
            _protobufDropPending(2)
            this._signerOfOperation = value.copy()
            _protobufMutated()
        }

    val hasSignerOfOperation: Boolean
        get() = this._signerOfOperation != null

    fun clearSignerOfOperation() {
        _protobufDropPending(2)
        this._signerOfOperation = null
        _protobufMutated()
    }

    var operationStatus: VCSEC_OperationStatus_E = VCSEC_OperationStatus_E.operationstatusOk
        set(value) {
            field = value
            _protobufForgetUnrecognized(3)
            _protobufMutated()
        }

    private var _signerOfOperation: VCSEC_KeyIdentifier? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(VCSEC_WhitelistOperation_information_E.UNRECOGNIZED) { VCSEC_WhitelistOperation_information_E.fromRawValue(it) }
                    ?.let { this.whitelistOperationInformation = it }
                2 -> decoder.decodeSingularMessageField(this._signerOfOperation) { VCSEC_KeyIdentifier() }?.let {
                    _protobufDropPending(2)
                    this._signerOfOperation = it
                }
                3 -> decoder.decodeSingularOpenEnumField(VCSEC_OperationStatus_E.UNRECOGNIZED) { VCSEC_OperationStatus_E.fromRawValue(it) }
                    ?.let { this.operationStatus = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.whitelistOperationInformation != VCSEC_WhitelistOperation_information_E.whitelistoperationInformationNone && this.whitelistOperationInformation != VCSEC_WhitelistOperation_information_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.whitelistOperationInformation.rawValue, 1)
        }
        this._signerOfOperation?.let {
            visitor.visitSingularMessageField(it, 2)
        }
        if (this.operationStatus != VCSEC_OperationStatus_E.operationstatusOk && this.operationStatus != VCSEC_OperationStatus_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.operationStatus.rawValue, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_WhitelistOperation_status) return false
        if (this.whitelistOperationInformation != other.whitelistOperationInformation) return false
        if (this._signerOfOperation != other._signerOfOperation) return false
        if (this.operationStatus != other.operationStatus) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.whitelistOperationInformation.hashCode()
        hash = 31 * hash + (this._signerOfOperation?.hashCode() ?: 0)
        hash = 31 * hash + this.operationStatus.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_WhitelistOperation_status {
        val result = VCSEC_WhitelistOperation_status()
        result.whitelistOperationInformation = this.whitelistOperationInformation
        result._signerOfOperation = this._signerOfOperation?.copy()
        result.operationStatus = this.operationStatus
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.WhitelistOperation_status"

        fun with(block: VCSEC_WhitelistOperation_status.() -> Unit): VCSEC_WhitelistOperation_status =
            VCSEC_WhitelistOperation_status().apply(block)
    }
}

class VCSEC_SignedMessage_status() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var counter: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var signedMessageInformation: VCSEC_SignedMessage_information_E = VCSEC_SignedMessage_information_E.signedmessageInformationNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(2)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularUInt32Field()?.let { this.counter = it }
                2 -> decoder.decodeSingularOpenEnumField(VCSEC_SignedMessage_information_E.UNRECOGNIZED) { VCSEC_SignedMessage_information_E.fromRawValue(it) }
                    ?.let { this.signedMessageInformation = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.counter != 0u) {
            visitor.visitSingularUInt32Field(this.counter, 1)
        }
        if (this.signedMessageInformation != VCSEC_SignedMessage_information_E.signedmessageInformationNone && this.signedMessageInformation != VCSEC_SignedMessage_information_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.signedMessageInformation.rawValue, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_SignedMessage_status) return false
        if (this.counter != other.counter) return false
        if (this.signedMessageInformation != other.signedMessageInformation) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.counter.hashCode()
        hash = 31 * hash + this.signedMessageInformation.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_SignedMessage_status {
        val result = VCSEC_SignedMessage_status()
        result.counter = this.counter
        result.signedMessageInformation = this.signedMessageInformation
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.SignedMessage_status"

        fun with(block: VCSEC_SignedMessage_status.() -> Unit): VCSEC_SignedMessage_status =
            VCSEC_SignedMessage_status().apply(block)
    }
}

class VCSEC_CommandStatus() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var operationStatus: VCSEC_OperationStatus_E = VCSEC_OperationStatus_E.operationstatusOk
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var subMessage: VCSEC_CommandStatus.OneOf_SubMessage?
        get() = this._subMessage
        set(value) {
            this._protobufStore_subMessage(this._protobufCopy_subMessage(value))
            _protobufMutated()
        }

    var signedMessageStatus: VCSEC_SignedMessage_status
        get() = (this._subMessage as? VCSEC_CommandStatus.OneOf_SubMessage.signedMessageStatus)?.value
            ?: _protobufPending(2, { VCSEC_SignedMessage_status() }) { this._protobufStore_subMessage(VCSEC_CommandStatus.OneOf_SubMessage.signedMessageStatus(it)) }
        set(value) {
            this.subMessage = VCSEC_CommandStatus.OneOf_SubMessage.signedMessageStatus(value)
        }

    var whitelistOperationStatus: VCSEC_WhitelistOperation_status
        get() = (this._subMessage as? VCSEC_CommandStatus.OneOf_SubMessage.whitelistOperationStatus)?.value
            ?: _protobufPending(3, { VCSEC_WhitelistOperation_status() }) { this._protobufStore_subMessage(VCSEC_CommandStatus.OneOf_SubMessage.whitelistOperationStatus(it)) }
        set(value) {
            this.subMessage = VCSEC_CommandStatus.OneOf_SubMessage.whitelistOperationStatus(value)
        }

    sealed class OneOf_SubMessage(value: Any) : ProtobufOneofCase(value) {
        class signedMessageStatus(val value: VCSEC_SignedMessage_status) : OneOf_SubMessage(value)
        class whitelistOperationStatus(val value: VCSEC_WhitelistOperation_status) : OneOf_SubMessage(value)
    }

    private var _subMessage: VCSEC_CommandStatus.OneOf_SubMessage? = null

    private fun _protobufStore_subMessage(value: VCSEC_CommandStatus.OneOf_SubMessage?) {
        _protobufDropPending(2)
        _protobufDropPending(3)
        this._subMessage = value
    }

    private fun _protobufCopy_subMessage(value: VCSEC_CommandStatus.OneOf_SubMessage?): VCSEC_CommandStatus.OneOf_SubMessage? =
        when (value) {
            is VCSEC_CommandStatus.OneOf_SubMessage.signedMessageStatus -> VCSEC_CommandStatus.OneOf_SubMessage.signedMessageStatus(value.value.copy())
            is VCSEC_CommandStatus.OneOf_SubMessage.whitelistOperationStatus -> VCSEC_CommandStatus.OneOf_SubMessage.whitelistOperationStatus(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(VCSEC_OperationStatus_E.UNRECOGNIZED) { VCSEC_OperationStatus_E.fromRawValue(it) }
                    ?.let { this.operationStatus = it }
                2 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_CommandStatus.OneOf_SubMessage.signedMessageStatus)?.value) { VCSEC_SignedMessage_status() }
                    ?.let { this._protobufStore_subMessage(VCSEC_CommandStatus.OneOf_SubMessage.signedMessageStatus(it)) }
                3 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_CommandStatus.OneOf_SubMessage.whitelistOperationStatus)?.value) { VCSEC_WhitelistOperation_status() }
                    ?.let { this._protobufStore_subMessage(VCSEC_CommandStatus.OneOf_SubMessage.whitelistOperationStatus(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.operationStatus != VCSEC_OperationStatus_E.operationstatusOk && this.operationStatus != VCSEC_OperationStatus_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.operationStatus.rawValue, 1)
        }
        (this._subMessage as? VCSEC_CommandStatus.OneOf_SubMessage.signedMessageStatus)?.let {
            visitor.visitSingularMessageField(it.value, 2)
        }
        (this._subMessage as? VCSEC_CommandStatus.OneOf_SubMessage.whitelistOperationStatus)?.let {
            visitor.visitSingularMessageField(it.value, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_CommandStatus) return false
        if (this.operationStatus != other.operationStatus) return false
        if (this._subMessage != other._subMessage) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.operationStatus.hashCode()
        hash = 31 * hash + (this._subMessage?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_CommandStatus {
        val result = VCSEC_CommandStatus()
        result.operationStatus = this.operationStatus
        result._subMessage = this._protobufCopy_subMessage(this._subMessage)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.CommandStatus"

        fun with(block: VCSEC_CommandStatus.() -> Unit): VCSEC_CommandStatus =
            VCSEC_CommandStatus().apply(block)
    }
}

class VCSEC_UnsignedMessage() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var subMessage: VCSEC_UnsignedMessage.OneOf_SubMessage?
        get() = this._subMessage
        set(value) {
            this._protobufStore_subMessage(this._protobufCopy_subMessage(value))
            _protobufMutated()
        }

    var informationRequest: VCSEC_InformationRequest
        get() = (this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.informationRequest)?.value
            ?: _protobufPending(1, { VCSEC_InformationRequest() }) { this._protobufStore_subMessage(VCSEC_UnsignedMessage.OneOf_SubMessage.informationRequest(it)) }
        set(value) {
            this.subMessage = VCSEC_UnsignedMessage.OneOf_SubMessage.informationRequest(value)
        }

    var rkeaction: VCSEC_RKEAction_E
        get() = (this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.rkeaction)?.value ?: VCSEC_RKEAction_E.rkeActionUnlock
        set(value) {
            this.subMessage = VCSEC_UnsignedMessage.OneOf_SubMessage.rkeaction(value)
        }

    var closureMoveRequest: VCSEC_ClosureMoveRequest
        get() = (this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.closureMoveRequest)?.value
            ?: _protobufPending(4, { VCSEC_ClosureMoveRequest() }) { this._protobufStore_subMessage(VCSEC_UnsignedMessage.OneOf_SubMessage.closureMoveRequest(it)) }
        set(value) {
            this.subMessage = VCSEC_UnsignedMessage.OneOf_SubMessage.closureMoveRequest(value)
        }

    var whitelistOperation: VCSEC_WhitelistOperation
        get() = (this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.whitelistOperation)?.value
            ?: _protobufPending(16, { VCSEC_WhitelistOperation() }) { this._protobufStore_subMessage(VCSEC_UnsignedMessage.OneOf_SubMessage.whitelistOperation(it)) }
        set(value) {
            this.subMessage = VCSEC_UnsignedMessage.OneOf_SubMessage.whitelistOperation(value)
        }

    sealed class OneOf_SubMessage(value: Any) : ProtobufOneofCase(value) {
        class informationRequest(val value: VCSEC_InformationRequest) : OneOf_SubMessage(value)
        class rkeaction(val value: VCSEC_RKEAction_E) : OneOf_SubMessage(value)
        class closureMoveRequest(val value: VCSEC_ClosureMoveRequest) : OneOf_SubMessage(value)
        class whitelistOperation(val value: VCSEC_WhitelistOperation) : OneOf_SubMessage(value)
    }

    private var _subMessage: VCSEC_UnsignedMessage.OneOf_SubMessage? = null

    private fun _protobufStore_subMessage(value: VCSEC_UnsignedMessage.OneOf_SubMessage?) {
        _protobufDropPending(1)
        _protobufDropPending(4)
        _protobufDropPending(16)
        _protobufForgetUnrecognized(2)
        this._subMessage = value
    }

    private fun _protobufCopy_subMessage(value: VCSEC_UnsignedMessage.OneOf_SubMessage?): VCSEC_UnsignedMessage.OneOf_SubMessage? =
        when (value) {
            is VCSEC_UnsignedMessage.OneOf_SubMessage.informationRequest -> VCSEC_UnsignedMessage.OneOf_SubMessage.informationRequest(value.value.copy())
            is VCSEC_UnsignedMessage.OneOf_SubMessage.closureMoveRequest -> VCSEC_UnsignedMessage.OneOf_SubMessage.closureMoveRequest(value.value.copy())
            is VCSEC_UnsignedMessage.OneOf_SubMessage.whitelistOperation -> VCSEC_UnsignedMessage.OneOf_SubMessage.whitelistOperation(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.informationRequest)?.value) { VCSEC_InformationRequest() }
                    ?.let { decoder.forgetUnrecognized(2); this._protobufStore_subMessage(VCSEC_UnsignedMessage.OneOf_SubMessage.informationRequest(it)) }
                2 -> decoder.decodeSingularOpenEnumField(VCSEC_RKEAction_E.UNRECOGNIZED) { VCSEC_RKEAction_E.fromRawValue(it) }
                    ?.let { this._protobufStore_subMessage(VCSEC_UnsignedMessage.OneOf_SubMessage.rkeaction(it)) }
                4 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.closureMoveRequest)?.value) { VCSEC_ClosureMoveRequest() }
                    ?.let { decoder.forgetUnrecognized(2); this._protobufStore_subMessage(VCSEC_UnsignedMessage.OneOf_SubMessage.closureMoveRequest(it)) }
                16 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.whitelistOperation)?.value) { VCSEC_WhitelistOperation() }
                    ?.let { decoder.forgetUnrecognized(2); this._protobufStore_subMessage(VCSEC_UnsignedMessage.OneOf_SubMessage.whitelistOperation(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.informationRequest)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.rkeaction)?.let {
            if (it.value != VCSEC_RKEAction_E.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 2)
            }
        }
        (this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.closureMoveRequest)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        (this._subMessage as? VCSEC_UnsignedMessage.OneOf_SubMessage.whitelistOperation)?.let {
            visitor.visitSingularMessageField(it.value, 16)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_UnsignedMessage) return false
        if (this._subMessage != other._subMessage) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._subMessage?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_UnsignedMessage {
        val result = VCSEC_UnsignedMessage()
        result._subMessage = this._protobufCopy_subMessage(this._subMessage)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.UnsignedMessage"

        fun with(block: VCSEC_UnsignedMessage.() -> Unit): VCSEC_UnsignedMessage =
            VCSEC_UnsignedMessage().apply(block)
    }
}

class VCSEC_ClosureStatuses() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var frontDriverDoor: VCSEC_ClosureState_E = VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var frontPassengerDoor: VCSEC_ClosureState_E = VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            field = value
            _protobufForgetUnrecognized(2)
            _protobufMutated()
        }

    var rearDriverDoor: VCSEC_ClosureState_E = VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            field = value
            _protobufForgetUnrecognized(3)
            _protobufMutated()
        }

    var rearPassengerDoor: VCSEC_ClosureState_E = VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            field = value
            _protobufForgetUnrecognized(4)
            _protobufMutated()
        }

    var rearTrunk: VCSEC_ClosureState_E = VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            field = value
            _protobufForgetUnrecognized(5)
            _protobufMutated()
        }

    var frontTrunk: VCSEC_ClosureState_E = VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            field = value
            _protobufForgetUnrecognized(6)
            _protobufMutated()
        }

    var chargePort: VCSEC_ClosureState_E = VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            field = value
            _protobufForgetUnrecognized(7)
            _protobufMutated()
        }

    var tonneau: VCSEC_ClosureState_E = VCSEC_ClosureState_E.closurestateClosed
        set(value) {
            field = value
            _protobufForgetUnrecognized(8)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this.frontDriverDoor = it }
                2 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this.frontPassengerDoor = it }
                3 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this.rearDriverDoor = it }
                4 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this.rearPassengerDoor = it }
                5 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this.rearTrunk = it }
                6 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this.frontTrunk = it }
                7 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this.chargePort = it }
                8 -> decoder.decodeSingularOpenEnumField(VCSEC_ClosureState_E.UNRECOGNIZED) { VCSEC_ClosureState_E.fromRawValue(it) }
                    ?.let { this.tonneau = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.frontDriverDoor != VCSEC_ClosureState_E.closurestateClosed && this.frontDriverDoor != VCSEC_ClosureState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.frontDriverDoor.rawValue, 1)
        }
        if (this.frontPassengerDoor != VCSEC_ClosureState_E.closurestateClosed && this.frontPassengerDoor != VCSEC_ClosureState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.frontPassengerDoor.rawValue, 2)
        }
        if (this.rearDriverDoor != VCSEC_ClosureState_E.closurestateClosed && this.rearDriverDoor != VCSEC_ClosureState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.rearDriverDoor.rawValue, 3)
        }
        if (this.rearPassengerDoor != VCSEC_ClosureState_E.closurestateClosed && this.rearPassengerDoor != VCSEC_ClosureState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.rearPassengerDoor.rawValue, 4)
        }
        if (this.rearTrunk != VCSEC_ClosureState_E.closurestateClosed && this.rearTrunk != VCSEC_ClosureState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.rearTrunk.rawValue, 5)
        }
        if (this.frontTrunk != VCSEC_ClosureState_E.closurestateClosed && this.frontTrunk != VCSEC_ClosureState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.frontTrunk.rawValue, 6)
        }
        if (this.chargePort != VCSEC_ClosureState_E.closurestateClosed && this.chargePort != VCSEC_ClosureState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.chargePort.rawValue, 7)
        }
        if (this.tonneau != VCSEC_ClosureState_E.closurestateClosed && this.tonneau != VCSEC_ClosureState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.tonneau.rawValue, 8)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_ClosureStatuses) return false
        if (this.frontDriverDoor != other.frontDriverDoor) return false
        if (this.frontPassengerDoor != other.frontPassengerDoor) return false
        if (this.rearDriverDoor != other.rearDriverDoor) return false
        if (this.rearPassengerDoor != other.rearPassengerDoor) return false
        if (this.rearTrunk != other.rearTrunk) return false
        if (this.frontTrunk != other.frontTrunk) return false
        if (this.chargePort != other.chargePort) return false
        if (this.tonneau != other.tonneau) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.frontDriverDoor.hashCode()
        hash = 31 * hash + this.frontPassengerDoor.hashCode()
        hash = 31 * hash + this.rearDriverDoor.hashCode()
        hash = 31 * hash + this.rearPassengerDoor.hashCode()
        hash = 31 * hash + this.rearTrunk.hashCode()
        hash = 31 * hash + this.frontTrunk.hashCode()
        hash = 31 * hash + this.chargePort.hashCode()
        hash = 31 * hash + this.tonneau.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_ClosureStatuses {
        val result = VCSEC_ClosureStatuses()
        result.frontDriverDoor = this.frontDriverDoor
        result.frontPassengerDoor = this.frontPassengerDoor
        result.rearDriverDoor = this.rearDriverDoor
        result.rearPassengerDoor = this.rearPassengerDoor
        result.rearTrunk = this.rearTrunk
        result.frontTrunk = this.frontTrunk
        result.chargePort = this.chargePort
        result.tonneau = this.tonneau
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.ClosureStatuses"

        fun with(block: VCSEC_ClosureStatuses.() -> Unit): VCSEC_ClosureStatuses =
            VCSEC_ClosureStatuses().apply(block)
    }
}

class VCSEC_DetailedClosureStatus() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var tonneauPercentOpen: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularUInt32Field()?.let { this.tonneauPercentOpen = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.tonneauPercentOpen != 0u) {
            visitor.visitSingularUInt32Field(this.tonneauPercentOpen, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_DetailedClosureStatus) return false
        if (this.tonneauPercentOpen != other.tonneauPercentOpen) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.tonneauPercentOpen.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_DetailedClosureStatus {
        val result = VCSEC_DetailedClosureStatus()
        result.tonneauPercentOpen = this.tonneauPercentOpen
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.DetailedClosureStatus"

        fun with(block: VCSEC_DetailedClosureStatus.() -> Unit): VCSEC_DetailedClosureStatus =
            VCSEC_DetailedClosureStatus().apply(block)
    }
}

class VCSEC_VehicleStatus() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var closureStatuses: VCSEC_ClosureStatuses
        get() = this._closureStatuses ?: _protobufPending(1, { VCSEC_ClosureStatuses() }) { this._closureStatuses = it }
        set(value) {
            _protobufDropPending(1)
            this._closureStatuses = value.copy()
            _protobufMutated()
        }

    val hasClosureStatuses: Boolean
        get() = this._closureStatuses != null

    fun clearClosureStatuses() {
        _protobufDropPending(1)
        this._closureStatuses = null
        _protobufMutated()
    }

    var vehicleLockState: VCSEC_VehicleLockState_E = VCSEC_VehicleLockState_E.vehiclelockstateUnlocked
        set(value) {
            field = value
            _protobufForgetUnrecognized(2)
            _protobufMutated()
        }

    var vehicleSleepStatus: VCSEC_VehicleSleepStatus_E = VCSEC_VehicleSleepStatus_E.vehicleSleepStatusUnknown
        set(value) {
            field = value
            _protobufForgetUnrecognized(3)
            _protobufMutated()
        }

    var userPresence: VCSEC_UserPresence_E = VCSEC_UserPresence_E.vehicleUserPresenceUnknown
        set(value) {
            field = value
            _protobufForgetUnrecognized(4)
            _protobufMutated()
        }

    var detailedClosureStatus: VCSEC_DetailedClosureStatus
        get() = this._detailedClosureStatus ?: _protobufPending(5, { VCSEC_DetailedClosureStatus() }) { this._detailedClosureStatus = it }
        set(value) {
            _protobufDropPending(5)
            this._detailedClosureStatus = value.copy()
            _protobufMutated()
        }

    val hasDetailedClosureStatus: Boolean
        get() = this._detailedClosureStatus != null

    fun clearDetailedClosureStatus() {
        _protobufDropPending(5)
        this._detailedClosureStatus = null
        _protobufMutated()
    }

    private var _closureStatuses: VCSEC_ClosureStatuses? = null
    private var _detailedClosureStatus: VCSEC_DetailedClosureStatus? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._closureStatuses) { VCSEC_ClosureStatuses() }?.let {
                    _protobufDropPending(1)
                    this._closureStatuses = it
                }
                2 -> decoder.decodeSingularOpenEnumField(VCSEC_VehicleLockState_E.UNRECOGNIZED) { VCSEC_VehicleLockState_E.fromRawValue(it) }
                    ?.let { this.vehicleLockState = it }
                3 -> decoder.decodeSingularOpenEnumField(VCSEC_VehicleSleepStatus_E.UNRECOGNIZED) { VCSEC_VehicleSleepStatus_E.fromRawValue(it) }
                    ?.let { this.vehicleSleepStatus = it }
                4 -> decoder.decodeSingularOpenEnumField(VCSEC_UserPresence_E.UNRECOGNIZED) { VCSEC_UserPresence_E.fromRawValue(it) }
                    ?.let { this.userPresence = it }
                5 -> decoder.decodeSingularMessageField(this._detailedClosureStatus) { VCSEC_DetailedClosureStatus() }?.let {
                    _protobufDropPending(5)
                    this._detailedClosureStatus = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._closureStatuses?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        if (this.vehicleLockState != VCSEC_VehicleLockState_E.vehiclelockstateUnlocked && this.vehicleLockState != VCSEC_VehicleLockState_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.vehicleLockState.rawValue, 2)
        }
        if (this.vehicleSleepStatus != VCSEC_VehicleSleepStatus_E.vehicleSleepStatusUnknown && this.vehicleSleepStatus != VCSEC_VehicleSleepStatus_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.vehicleSleepStatus.rawValue, 3)
        }
        if (this.userPresence != VCSEC_UserPresence_E.vehicleUserPresenceUnknown && this.userPresence != VCSEC_UserPresence_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.userPresence.rawValue, 4)
        }
        this._detailedClosureStatus?.let {
            visitor.visitSingularMessageField(it, 5)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_VehicleStatus) return false
        if (this._closureStatuses != other._closureStatuses) return false
        if (this.vehicleLockState != other.vehicleLockState) return false
        if (this.vehicleSleepStatus != other.vehicleSleepStatus) return false
        if (this.userPresence != other.userPresence) return false
        if (this._detailedClosureStatus != other._detailedClosureStatus) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._closureStatuses?.hashCode() ?: 0)
        hash = 31 * hash + this.vehicleLockState.hashCode()
        hash = 31 * hash + this.vehicleSleepStatus.hashCode()
        hash = 31 * hash + this.userPresence.hashCode()
        hash = 31 * hash + (this._detailedClosureStatus?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_VehicleStatus {
        val result = VCSEC_VehicleStatus()
        result._closureStatuses = this._closureStatuses?.copy()
        result.vehicleLockState = this.vehicleLockState
        result.vehicleSleepStatus = this.vehicleSleepStatus
        result.userPresence = this.userPresence
        result._detailedClosureStatus = this._detailedClosureStatus?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.VehicleStatus"

        fun with(block: VCSEC_VehicleStatus.() -> Unit): VCSEC_VehicleStatus =
            VCSEC_VehicleStatus().apply(block)
    }
}

class VCSEC_FromVCSECMessage() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var subMessage: VCSEC_FromVCSECMessage.OneOf_SubMessage?
        get() = this._subMessage
        set(value) {
            this._protobufStore_subMessage(this._protobufCopy_subMessage(value))
            _protobufMutated()
        }

    var vehicleStatus: VCSEC_VehicleStatus
        get() = (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.vehicleStatus)?.value
            ?: _protobufPending(1, { VCSEC_VehicleStatus() }) { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.vehicleStatus(it)) }
        set(value) {
            this.subMessage = VCSEC_FromVCSECMessage.OneOf_SubMessage.vehicleStatus(value)
        }

    var commandStatus: VCSEC_CommandStatus
        get() = (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.commandStatus)?.value
            ?: _protobufPending(4, { VCSEC_CommandStatus() }) { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.commandStatus(it)) }
        set(value) {
            this.subMessage = VCSEC_FromVCSECMessage.OneOf_SubMessage.commandStatus(value)
        }

    var whitelistInfo: VCSEC_WhitelistInfo
        get() = (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistInfo)?.value
            ?: _protobufPending(16, { VCSEC_WhitelistInfo() }) { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistInfo(it)) }
        set(value) {
            this.subMessage = VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistInfo(value)
        }

    var whitelistEntryInfo: VCSEC_WhitelistEntryInfo
        get() = (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistEntryInfo)?.value
            ?: _protobufPending(17, { VCSEC_WhitelistEntryInfo() }) { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistEntryInfo(it)) }
        set(value) {
            this.subMessage = VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistEntryInfo(value)
        }

    var nominalError: Errors_NominalError
        get() = (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.nominalError)?.value
            ?: _protobufPending(46, { Errors_NominalError() }) { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.nominalError(it)) }
        set(value) {
            this.subMessage = VCSEC_FromVCSECMessage.OneOf_SubMessage.nominalError(value)
        }

    sealed class OneOf_SubMessage(value: Any) : ProtobufOneofCase(value) {
        class vehicleStatus(val value: VCSEC_VehicleStatus) : OneOf_SubMessage(value)
        class commandStatus(val value: VCSEC_CommandStatus) : OneOf_SubMessage(value)
        class whitelistInfo(val value: VCSEC_WhitelistInfo) : OneOf_SubMessage(value)
        class whitelistEntryInfo(val value: VCSEC_WhitelistEntryInfo) : OneOf_SubMessage(value)
        class nominalError(val value: Errors_NominalError) : OneOf_SubMessage(value)
    }

    private var _subMessage: VCSEC_FromVCSECMessage.OneOf_SubMessage? = null

    private fun _protobufStore_subMessage(value: VCSEC_FromVCSECMessage.OneOf_SubMessage?) {
        _protobufDropPending(1)
        _protobufDropPending(4)
        _protobufDropPending(16)
        _protobufDropPending(17)
        _protobufDropPending(46)
        this._subMessage = value
    }

    private fun _protobufCopy_subMessage(value: VCSEC_FromVCSECMessage.OneOf_SubMessage?): VCSEC_FromVCSECMessage.OneOf_SubMessage? =
        when (value) {
            is VCSEC_FromVCSECMessage.OneOf_SubMessage.vehicleStatus -> VCSEC_FromVCSECMessage.OneOf_SubMessage.vehicleStatus(value.value.copy())
            is VCSEC_FromVCSECMessage.OneOf_SubMessage.commandStatus -> VCSEC_FromVCSECMessage.OneOf_SubMessage.commandStatus(value.value.copy())
            is VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistInfo -> VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistInfo(value.value.copy())
            is VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistEntryInfo -> VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistEntryInfo(value.value.copy())
            is VCSEC_FromVCSECMessage.OneOf_SubMessage.nominalError -> VCSEC_FromVCSECMessage.OneOf_SubMessage.nominalError(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.vehicleStatus)?.value) { VCSEC_VehicleStatus() }
                    ?.let { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.vehicleStatus(it)) }
                4 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.commandStatus)?.value) { VCSEC_CommandStatus() }
                    ?.let { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.commandStatus(it)) }
                16 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistInfo)?.value) { VCSEC_WhitelistInfo() }
                    ?.let { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistInfo(it)) }
                17 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistEntryInfo)?.value) { VCSEC_WhitelistEntryInfo() }
                    ?.let { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistEntryInfo(it)) }
                46 -> decoder.decodeSingularMessageField((this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.nominalError)?.value) { Errors_NominalError() }
                    ?.let { this._protobufStore_subMessage(VCSEC_FromVCSECMessage.OneOf_SubMessage.nominalError(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.vehicleStatus)?.let {
            visitor.visitSingularMessageField(it.value, 1)
        }
        (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.commandStatus)?.let {
            visitor.visitSingularMessageField(it.value, 4)
        }
        (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistInfo)?.let {
            visitor.visitSingularMessageField(it.value, 16)
        }
        (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.whitelistEntryInfo)?.let {
            visitor.visitSingularMessageField(it.value, 17)
        }
        (this._subMessage as? VCSEC_FromVCSECMessage.OneOf_SubMessage.nominalError)?.let {
            visitor.visitSingularMessageField(it.value, 46)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCSEC_FromVCSECMessage) return false
        if (this._subMessage != other._subMessage) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._subMessage?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): VCSEC_FromVCSECMessage {
        val result = VCSEC_FromVCSECMessage()
        result._subMessage = this._protobufCopy_subMessage(this._subMessage)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "VCSEC.FromVCSECMessage"

        fun with(block: VCSEC_FromVCSECMessage.() -> Unit): VCSEC_FromVCSECMessage =
            VCSEC_FromVCSECMessage().apply(block)
    }
}
