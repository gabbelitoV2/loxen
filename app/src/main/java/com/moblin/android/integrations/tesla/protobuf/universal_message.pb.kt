package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecoder
import com.moblin.android.platform.swiftprotobuf.BinaryEncodingVisitor
import com.moblin.android.platform.swiftprotobuf.GeneratedMessage
import com.moblin.android.platform.swiftprotobuf.ProtobufOneofCase
import com.moblin.android.platform.swiftprotobuf.merge

enum class UniversalMessage_Domain(val rawValue: Int) {
    broadcast(0),
    vehicleSecurity(2),
    infotainment(3),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<UniversalMessage_Domain> = listOf(
            broadcast,
            vehicleSecurity,
            infotainment,
        )

        fun fromRawValue(rawValue: Int): UniversalMessage_Domain =
            when (rawValue) {
                0 -> broadcast
                2 -> vehicleSecurity
                3 -> infotainment
                else -> UNRECOGNIZED
            }
    }
}

enum class UniversalMessage_OperationStatus_E(val rawValue: Int) {
    operationstatusOk(0),
    operationstatusWait(1),
    rror(2),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<UniversalMessage_OperationStatus_E> = listOf(
            operationstatusOk,
            operationstatusWait,
            rror,
        )

        fun fromRawValue(rawValue: Int): UniversalMessage_OperationStatus_E =
            when (rawValue) {
                0 -> operationstatusOk
                1 -> operationstatusWait
                2 -> rror
                else -> UNRECOGNIZED
            }
    }
}

enum class UniversalMessage_MessageFault_E(val rawValue: Int) {
    rrorNone(0),
    rrorBusy(1),
    rrorTimeout(2),
    rrorUnknownKeyID(3),
    rrorInactiveKey(4),
    rrorInvalidSignature(5),
    rrorInvalidTokenOrCounter(6),
    rrorInsufficientPrivileges(7),
    rrorInvalidDomains(8),
    rrorInvalidCommand(9),
    rrorDecoding(10),
    rrorInternal(11),
    rrorWrongPersonalization(12),
    rrorBadParameter(13),
    rrorKeychainIsFull(14),
    rrorIncorrectEpoch(15),
    rrorIvIncorrectLength(16),
    rrorTimeExpired(17),
    rrorNotProvisionedWithIdentity(18),
    rrorCouldNotHashMetadata(19),
    rrorTimeToLiveTooLong(20),
    rrorRemoteAccessDisabled(21),
    rrorRemoteServiceAccessDisabled(22),
    rrorCommandRequiresAccountCredentials(23),
    rrorRequestMtuExceeded(24),
    rrorResponseMtuExceeded(25),
    rrorRepeatedCounter(26),
    rrorInvalidKeyHandle(27),
    rrorRequiresResponseEncryption(28),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<UniversalMessage_MessageFault_E> = listOf(
            rrorNone,
            rrorBusy,
            rrorTimeout,
            rrorUnknownKeyID,
            rrorInactiveKey,
            rrorInvalidSignature,
            rrorInvalidTokenOrCounter,
            rrorInsufficientPrivileges,
            rrorInvalidDomains,
            rrorInvalidCommand,
            rrorDecoding,
            rrorInternal,
            rrorWrongPersonalization,
            rrorBadParameter,
            rrorKeychainIsFull,
            rrorIncorrectEpoch,
            rrorIvIncorrectLength,
            rrorTimeExpired,
            rrorNotProvisionedWithIdentity,
            rrorCouldNotHashMetadata,
            rrorTimeToLiveTooLong,
            rrorRemoteAccessDisabled,
            rrorRemoteServiceAccessDisabled,
            rrorCommandRequiresAccountCredentials,
            rrorRequestMtuExceeded,
            rrorResponseMtuExceeded,
            rrorRepeatedCounter,
            rrorInvalidKeyHandle,
            rrorRequiresResponseEncryption,
        )

        fun fromRawValue(rawValue: Int): UniversalMessage_MessageFault_E =
            when (rawValue) {
                0 -> rrorNone
                1 -> rrorBusy
                2 -> rrorTimeout
                3 -> rrorUnknownKeyID
                4 -> rrorInactiveKey
                5 -> rrorInvalidSignature
                6 -> rrorInvalidTokenOrCounter
                7 -> rrorInsufficientPrivileges
                8 -> rrorInvalidDomains
                9 -> rrorInvalidCommand
                10 -> rrorDecoding
                11 -> rrorInternal
                12 -> rrorWrongPersonalization
                13 -> rrorBadParameter
                14 -> rrorKeychainIsFull
                15 -> rrorIncorrectEpoch
                16 -> rrorIvIncorrectLength
                17 -> rrorTimeExpired
                18 -> rrorNotProvisionedWithIdentity
                19 -> rrorCouldNotHashMetadata
                20 -> rrorTimeToLiveTooLong
                21 -> rrorRemoteAccessDisabled
                22 -> rrorRemoteServiceAccessDisabled
                23 -> rrorCommandRequiresAccountCredentials
                24 -> rrorRequestMtuExceeded
                25 -> rrorResponseMtuExceeded
                26 -> rrorRepeatedCounter
                27 -> rrorInvalidKeyHandle
                28 -> rrorRequiresResponseEncryption
                else -> UNRECOGNIZED
            }
    }
}

enum class UniversalMessage_Flags(val rawValue: Int) {
    flagUserCommand(0),
    flagEncryptResponse(1),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<UniversalMessage_Flags> = listOf(
            flagUserCommand,
            flagEncryptResponse,
        )

        fun fromRawValue(rawValue: Int): UniversalMessage_Flags =
            when (rawValue) {
                0 -> flagUserCommand
                1 -> flagEncryptResponse
                else -> UNRECOGNIZED
            }
    }
}

class UniversalMessage_Destination() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var subDestination: UniversalMessage_Destination.OneOf_SubDestination?
        get() = this._subDestination
        set(value) {
            this._protobufStore_subDestination(value)
            _protobufMutated()
        }

    var domain: UniversalMessage_Domain
        get() = (this._subDestination as? UniversalMessage_Destination.OneOf_SubDestination.domain)?.value ?: UniversalMessage_Domain.broadcast
        set(value) {
            this.subDestination = UniversalMessage_Destination.OneOf_SubDestination.domain(value)
        }

    var routingAddress: ByteArray
        get() = (this._subDestination as? UniversalMessage_Destination.OneOf_SubDestination.routingAddress)?.value ?: ByteArray(0)
        set(value) {
            this.subDestination = UniversalMessage_Destination.OneOf_SubDestination.routingAddress(value)
        }

    sealed class OneOf_SubDestination(value: Any) : ProtobufOneofCase(value) {
        class domain(val value: UniversalMessage_Domain) : OneOf_SubDestination(value)
        class routingAddress(val value: ByteArray) : OneOf_SubDestination(value)
    }

    private var _subDestination: UniversalMessage_Destination.OneOf_SubDestination? = null

    private fun _protobufStore_subDestination(value: UniversalMessage_Destination.OneOf_SubDestination?) {
        _protobufForgetUnrecognized(1)
        this._subDestination = value
    }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(UniversalMessage_Domain.UNRECOGNIZED) { UniversalMessage_Domain.fromRawValue(it) }
                    ?.let { this._protobufStore_subDestination(UniversalMessage_Destination.OneOf_SubDestination.domain(it)) }
                2 -> decoder.decodeSingularBytesField()?.let { decoder.forgetUnrecognized(1); this._protobufStore_subDestination(UniversalMessage_Destination.OneOf_SubDestination.routingAddress(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._subDestination as? UniversalMessage_Destination.OneOf_SubDestination.domain)?.let {
            if (it.value != UniversalMessage_Domain.UNRECOGNIZED) {
                visitor.visitSingularEnumField(it.value.rawValue, 1)
            }
        }
        (this._subDestination as? UniversalMessage_Destination.OneOf_SubDestination.routingAddress)?.let {
            visitor.visitSingularBytesField(it.value, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UniversalMessage_Destination) return false
        if (this._subDestination != other._subDestination) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._subDestination?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): UniversalMessage_Destination {
        val result = UniversalMessage_Destination()
        result._subDestination = this._subDestination
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "UniversalMessage.Destination"

        fun with(block: UniversalMessage_Destination.() -> Unit): UniversalMessage_Destination =
            UniversalMessage_Destination().apply(block)
    }
}

class UniversalMessage_MessageStatus() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var operationStatus: UniversalMessage_OperationStatus_E = UniversalMessage_OperationStatus_E.operationstatusOk
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    var signedMessageFault: UniversalMessage_MessageFault_E = UniversalMessage_MessageFault_E.rrorNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(2)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(UniversalMessage_OperationStatus_E.UNRECOGNIZED) { UniversalMessage_OperationStatus_E.fromRawValue(it) }
                    ?.let { this.operationStatus = it }
                2 -> decoder.decodeSingularOpenEnumField(UniversalMessage_MessageFault_E.UNRECOGNIZED) { UniversalMessage_MessageFault_E.fromRawValue(it) }
                    ?.let { this.signedMessageFault = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.operationStatus != UniversalMessage_OperationStatus_E.operationstatusOk && this.operationStatus != UniversalMessage_OperationStatus_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.operationStatus.rawValue, 1)
        }
        if (this.signedMessageFault != UniversalMessage_MessageFault_E.rrorNone && this.signedMessageFault != UniversalMessage_MessageFault_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.signedMessageFault.rawValue, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UniversalMessage_MessageStatus) return false
        if (this.operationStatus != other.operationStatus) return false
        if (this.signedMessageFault != other.signedMessageFault) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.operationStatus.hashCode()
        hash = 31 * hash + this.signedMessageFault.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): UniversalMessage_MessageStatus {
        val result = UniversalMessage_MessageStatus()
        result.operationStatus = this.operationStatus
        result.signedMessageFault = this.signedMessageFault
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "UniversalMessage.MessageStatus"

        fun with(block: UniversalMessage_MessageStatus.() -> Unit): UniversalMessage_MessageStatus =
            UniversalMessage_MessageStatus().apply(block)
    }
}

class UniversalMessage_SessionInfoRequest() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var publicKey: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var challenge: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBytesField()?.let { this.publicKey = it }
                2 -> decoder.decodeSingularBytesField()?.let { this.challenge = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.publicKey.isNotEmpty()) {
            visitor.visitSingularBytesField(this.publicKey, 1)
        }
        if (this.challenge.isNotEmpty()) {
            visitor.visitSingularBytesField(this.challenge, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UniversalMessage_SessionInfoRequest) return false
        if (!this.publicKey.contentEquals(other.publicKey)) return false
        if (!this.challenge.contentEquals(other.challenge)) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.publicKey.contentHashCode()
        hash = 31 * hash + this.challenge.contentHashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): UniversalMessage_SessionInfoRequest {
        val result = UniversalMessage_SessionInfoRequest()
        result.publicKey = this.publicKey
        result.challenge = this.challenge
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "UniversalMessage.SessionInfoRequest"

        fun with(block: UniversalMessage_SessionInfoRequest.() -> Unit): UniversalMessage_SessionInfoRequest =
            UniversalMessage_SessionInfoRequest().apply(block)
    }
}

class UniversalMessage_RoutableMessage() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var toDestination: UniversalMessage_Destination
        get() = this._toDestination ?: _protobufPending(6, { UniversalMessage_Destination() }) { this._toDestination = it }
        set(value) {
            _protobufDropPending(6)
            this._toDestination = value.copy()
            _protobufMutated()
        }

    val hasToDestination: Boolean
        get() = this._toDestination != null

    fun clearToDestination() {
        _protobufDropPending(6)
        this._toDestination = null
        _protobufMutated()
    }

    var fromDestination: UniversalMessage_Destination
        get() = this._fromDestination ?: _protobufPending(7, { UniversalMessage_Destination() }) { this._fromDestination = it }
        set(value) {
            _protobufDropPending(7)
            this._fromDestination = value.copy()
            _protobufMutated()
        }

    val hasFromDestination: Boolean
        get() = this._fromDestination != null

    fun clearFromDestination() {
        _protobufDropPending(7)
        this._fromDestination = null
        _protobufMutated()
    }

    var payload: UniversalMessage_RoutableMessage.OneOf_Payload?
        get() = this._payload
        set(value) {
            this._protobufStore_payload(this._protobufCopy_payload(value))
            _protobufMutated()
        }

    var protobufMessageAsBytes: ByteArray
        get() = (this._payload as? UniversalMessage_RoutableMessage.OneOf_Payload.protobufMessageAsBytes)?.value ?: ByteArray(0)
        set(value) {
            this.payload = UniversalMessage_RoutableMessage.OneOf_Payload.protobufMessageAsBytes(value)
        }

    var sessionInfoRequest: UniversalMessage_SessionInfoRequest
        get() = (this._payload as? UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest)?.value
            ?: _protobufPending(14, { UniversalMessage_SessionInfoRequest() }) { this._protobufStore_payload(UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest(it)) }
        set(value) {
            this.payload = UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest(value)
        }

    var sessionInfo: ByteArray
        get() = (this._payload as? UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo)?.value ?: ByteArray(0)
        set(value) {
            this.payload = UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo(value)
        }

    var subSigData: UniversalMessage_RoutableMessage.OneOf_SubSigData?
        get() = this._subSigData
        set(value) {
            this._protobufStore_subSigData(this._protobufCopy_subSigData(value))
            _protobufMutated()
        }

    var signatureData: Signatures_SignatureData
        get() = (this._subSigData as? UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData)?.value
            ?: _protobufPending(13, { Signatures_SignatureData() }) { this._protobufStore_subSigData(UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData(it)) }
        set(value) {
            this.subSigData = UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData(value)
        }

    var signedMessageStatus: UniversalMessage_MessageStatus
        get() = this._signedMessageStatus ?: _protobufPending(12, { UniversalMessage_MessageStatus() }) { this._signedMessageStatus = it }
        set(value) {
            _protobufDropPending(12)
            this._signedMessageStatus = value.copy()
            _protobufMutated()
        }

    val hasSignedMessageStatus: Boolean
        get() = this._signedMessageStatus != null

    fun clearSignedMessageStatus() {
        _protobufDropPending(12)
        this._signedMessageStatus = null
        _protobufMutated()
    }

    var requestUuid: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var uuid: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var flags: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    sealed class OneOf_Payload(value: Any) : ProtobufOneofCase(value) {
        class protobufMessageAsBytes(val value: ByteArray) : OneOf_Payload(value)
        class sessionInfoRequest(val value: UniversalMessage_SessionInfoRequest) : OneOf_Payload(value)
        class sessionInfo(val value: ByteArray) : OneOf_Payload(value)
    }

    sealed class OneOf_SubSigData(value: Any) : ProtobufOneofCase(value) {
        class signatureData(val value: Signatures_SignatureData) : OneOf_SubSigData(value)
    }

    private var _toDestination: UniversalMessage_Destination? = null
    private var _fromDestination: UniversalMessage_Destination? = null
    private var _payload: UniversalMessage_RoutableMessage.OneOf_Payload? = null
    private var _subSigData: UniversalMessage_RoutableMessage.OneOf_SubSigData? = null
    private var _signedMessageStatus: UniversalMessage_MessageStatus? = null

    private fun _protobufStore_payload(value: UniversalMessage_RoutableMessage.OneOf_Payload?) {
        _protobufDropPending(14)
        this._payload = value
    }

    private fun _protobufCopy_payload(value: UniversalMessage_RoutableMessage.OneOf_Payload?): UniversalMessage_RoutableMessage.OneOf_Payload? =
        when (value) {
            is UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest -> UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest(value.value.copy())
            else -> value
        }

    private fun _protobufStore_subSigData(value: UniversalMessage_RoutableMessage.OneOf_SubSigData?) {
        _protobufDropPending(13)
        this._subSigData = value
    }

    private fun _protobufCopy_subSigData(value: UniversalMessage_RoutableMessage.OneOf_SubSigData?): UniversalMessage_RoutableMessage.OneOf_SubSigData? =
        when (value) {
            is UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData -> UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                6 -> decoder.decodeSingularMessageField(this._toDestination) { UniversalMessage_Destination() }?.let {
                    _protobufDropPending(6)
                    this._toDestination = it
                }
                7 -> decoder.decodeSingularMessageField(this._fromDestination) { UniversalMessage_Destination() }?.let {
                    _protobufDropPending(7)
                    this._fromDestination = it
                }
                10 -> decoder.decodeSingularBytesField()?.let { this._protobufStore_payload(UniversalMessage_RoutableMessage.OneOf_Payload.protobufMessageAsBytes(it)) }
                12 -> decoder.decodeSingularMessageField(this._signedMessageStatus) { UniversalMessage_MessageStatus() }?.let {
                    _protobufDropPending(12)
                    this._signedMessageStatus = it
                }
                13 -> decoder.decodeSingularMessageField((this._subSigData as? UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData)?.value) { Signatures_SignatureData() }
                    ?.let { this._protobufStore_subSigData(UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData(it)) }
                14 -> decoder.decodeSingularMessageField((this._payload as? UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest)?.value) { UniversalMessage_SessionInfoRequest() }
                    ?.let { this._protobufStore_payload(UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest(it)) }
                15 -> decoder.decodeSingularBytesField()?.let { this._protobufStore_payload(UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo(it)) }
                50 -> decoder.decodeSingularBytesField()?.let { this.requestUuid = it }
                51 -> decoder.decodeSingularBytesField()?.let { this.uuid = it }
                52 -> decoder.decodeSingularUInt32Field()?.let { this.flags = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._toDestination?.let {
            visitor.visitSingularMessageField(it, 6)
        }
        this._fromDestination?.let {
            visitor.visitSingularMessageField(it, 7)
        }
        (this._payload as? UniversalMessage_RoutableMessage.OneOf_Payload.protobufMessageAsBytes)?.let {
            visitor.visitSingularBytesField(it.value, 10)
        }
        this._signedMessageStatus?.let {
            visitor.visitSingularMessageField(it, 12)
        }
        (this._subSigData as? UniversalMessage_RoutableMessage.OneOf_SubSigData.signatureData)?.let {
            visitor.visitSingularMessageField(it.value, 13)
        }
        (this._payload as? UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfoRequest)?.let {
            visitor.visitSingularMessageField(it.value, 14)
        }
        (this._payload as? UniversalMessage_RoutableMessage.OneOf_Payload.sessionInfo)?.let {
            visitor.visitSingularBytesField(it.value, 15)
        }
        if (this.requestUuid.isNotEmpty()) {
            visitor.visitSingularBytesField(this.requestUuid, 50)
        }
        if (this.uuid.isNotEmpty()) {
            visitor.visitSingularBytesField(this.uuid, 51)
        }
        if (this.flags != 0u) {
            visitor.visitSingularUInt32Field(this.flags, 52)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UniversalMessage_RoutableMessage) return false
        if (this._toDestination != other._toDestination) return false
        if (this._fromDestination != other._fromDestination) return false
        if (this._payload != other._payload) return false
        if (this._subSigData != other._subSigData) return false
        if (this._signedMessageStatus != other._signedMessageStatus) return false
        if (!this.requestUuid.contentEquals(other.requestUuid)) return false
        if (!this.uuid.contentEquals(other.uuid)) return false
        if (this.flags != other.flags) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._toDestination?.hashCode() ?: 0)
        hash = 31 * hash + (this._fromDestination?.hashCode() ?: 0)
        hash = 31 * hash + (this._payload?.hashCode() ?: 0)
        hash = 31 * hash + (this._subSigData?.hashCode() ?: 0)
        hash = 31 * hash + (this._signedMessageStatus?.hashCode() ?: 0)
        hash = 31 * hash + this.requestUuid.contentHashCode()
        hash = 31 * hash + this.uuid.contentHashCode()
        hash = 31 * hash + this.flags.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): UniversalMessage_RoutableMessage {
        val result = UniversalMessage_RoutableMessage()
        result._toDestination = this._toDestination?.copy()
        result._fromDestination = this._fromDestination?.copy()
        result._payload = this._protobufCopy_payload(this._payload)
        result._subSigData = this._protobufCopy_subSigData(this._subSigData)
        result._signedMessageStatus = this._signedMessageStatus?.copy()
        result.requestUuid = this.requestUuid
        result.uuid = this.uuid
        result.flags = this.flags
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "UniversalMessage.RoutableMessage"

        fun with(block: UniversalMessage_RoutableMessage.() -> Unit): UniversalMessage_RoutableMessage =
            UniversalMessage_RoutableMessage().apply(block)
    }
}
