package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecoder
import com.moblin.android.platform.swiftprotobuf.BinaryEncodingVisitor
import com.moblin.android.platform.swiftprotobuf.GeneratedMessage
import com.moblin.android.platform.swiftprotobuf.ProtobufOneofCase
import com.moblin.android.platform.swiftprotobuf.merge

enum class Signatures_Tag(val rawValue: Int) {
    signatureType(0),
    domain(1),
    personalization(2),
    epoch(3),
    expiresAt(4),
    counter(5),
    challenge(6),
    flags(7),
    requestHash(8),
    fault(9),
    end(255),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<Signatures_Tag> = listOf(
            signatureType,
            domain,
            personalization,
            epoch,
            expiresAt,
            counter,
            challenge,
            flags,
            requestHash,
            fault,
            end,
        )

        fun fromRawValue(rawValue: Int): Signatures_Tag =
            when (rawValue) {
                0 -> signatureType
                1 -> domain
                2 -> personalization
                3 -> epoch
                4 -> expiresAt
                5 -> counter
                6 -> challenge
                7 -> flags
                8 -> requestHash
                9 -> fault
                255 -> end
                else -> UNRECOGNIZED
            }
    }
}

enum class Signatures_SignatureType(val rawValue: Int) {
    aesGcm(0),
    aesGcmPersonalized(5),
    hmac(6),
    hmacPersonalized(8),
    aesGcmResponse(9),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<Signatures_SignatureType> = listOf(
            aesGcm,
            aesGcmPersonalized,
            hmac,
            hmacPersonalized,
            aesGcmResponse,
        )

        fun fromRawValue(rawValue: Int): Signatures_SignatureType =
            when (rawValue) {
                0 -> aesGcm
                5 -> aesGcmPersonalized
                6 -> hmac
                8 -> hmacPersonalized
                9 -> aesGcmResponse
                else -> UNRECOGNIZED
            }
    }
}

enum class Signatures_Session_Info_Status(val rawValue: Int) {
    ok(0),
    keyNotOnWhitelist(1),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<Signatures_Session_Info_Status> = listOf(
            ok,
            keyNotOnWhitelist,
        )

        fun fromRawValue(rawValue: Int): Signatures_Session_Info_Status =
            when (rawValue) {
                0 -> ok
                1 -> keyNotOnWhitelist
                else -> UNRECOGNIZED
            }
    }
}

class Signatures_KeyIdentity() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var identityType: Signatures_KeyIdentity.OneOf_IdentityType?
        get() = this._identityType
        set(value) {
            this._identityType = value
            _protobufMutated()
        }

    var publicKey: ByteArray
        get() = (this._identityType as? Signatures_KeyIdentity.OneOf_IdentityType.publicKey)?.value ?: ByteArray(0)
        set(value) {
            this.identityType = Signatures_KeyIdentity.OneOf_IdentityType.publicKey(value)
        }

    var handle: UInt
        get() = (this._identityType as? Signatures_KeyIdentity.OneOf_IdentityType.handle)?.value ?: 0u
        set(value) {
            this.identityType = Signatures_KeyIdentity.OneOf_IdentityType.handle(value)
        }

    sealed class OneOf_IdentityType(value: Any) : ProtobufOneofCase(value) {
        class publicKey(val value: ByteArray) : OneOf_IdentityType(value)
        class handle(val value: UInt) : OneOf_IdentityType(value)
    }

    private var _identityType: Signatures_KeyIdentity.OneOf_IdentityType? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBytesField()?.let { this._identityType = Signatures_KeyIdentity.OneOf_IdentityType.publicKey(it) }
                3 -> decoder.decodeSingularUInt32Field()?.let { this._identityType = Signatures_KeyIdentity.OneOf_IdentityType.handle(it) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        (this._identityType as? Signatures_KeyIdentity.OneOf_IdentityType.publicKey)?.let {
            visitor.visitSingularBytesField(it.value, 1)
        }
        (this._identityType as? Signatures_KeyIdentity.OneOf_IdentityType.handle)?.let {
            visitor.visitSingularUInt32Field(it.value, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signatures_KeyIdentity) return false
        if (this._identityType != other._identityType) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._identityType?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Signatures_KeyIdentity {
        val result = Signatures_KeyIdentity()
        result._identityType = this._identityType
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Signatures.KeyIdentity"

        fun with(block: Signatures_KeyIdentity.() -> Unit): Signatures_KeyIdentity =
            Signatures_KeyIdentity().apply(block)
    }
}

class Signatures_AES_GCM_Personalized_Signature_Data() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var epoch: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var nonce: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var counter: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var expiresAt: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var tag: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBytesField()?.let { this.epoch = it }
                2 -> decoder.decodeSingularBytesField()?.let { this.nonce = it }
                3 -> decoder.decodeSingularUInt32Field()?.let { this.counter = it }
                4 -> decoder.decodeSingularFixed32Field()?.let { this.expiresAt = it }
                5 -> decoder.decodeSingularBytesField()?.let { this.tag = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.epoch.isNotEmpty()) {
            visitor.visitSingularBytesField(this.epoch, 1)
        }
        if (this.nonce.isNotEmpty()) {
            visitor.visitSingularBytesField(this.nonce, 2)
        }
        if (this.counter != 0u) {
            visitor.visitSingularUInt32Field(this.counter, 3)
        }
        if (this.expiresAt != 0u) {
            visitor.visitSingularFixed32Field(this.expiresAt, 4)
        }
        if (this.tag.isNotEmpty()) {
            visitor.visitSingularBytesField(this.tag, 5)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signatures_AES_GCM_Personalized_Signature_Data) return false
        if (!this.epoch.contentEquals(other.epoch)) return false
        if (!this.nonce.contentEquals(other.nonce)) return false
        if (this.counter != other.counter) return false
        if (this.expiresAt != other.expiresAt) return false
        if (!this.tag.contentEquals(other.tag)) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.epoch.contentHashCode()
        hash = 31 * hash + this.nonce.contentHashCode()
        hash = 31 * hash + this.counter.hashCode()
        hash = 31 * hash + this.expiresAt.hashCode()
        hash = 31 * hash + this.tag.contentHashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Signatures_AES_GCM_Personalized_Signature_Data {
        val result = Signatures_AES_GCM_Personalized_Signature_Data()
        result.epoch = this.epoch
        result.nonce = this.nonce
        result.counter = this.counter
        result.expiresAt = this.expiresAt
        result.tag = this.tag
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Signatures.AES_GCM_Personalized_Signature_Data"

        fun with(block: Signatures_AES_GCM_Personalized_Signature_Data.() -> Unit): Signatures_AES_GCM_Personalized_Signature_Data =
            Signatures_AES_GCM_Personalized_Signature_Data().apply(block)
    }
}

class Signatures_AES_GCM_Response_Signature_Data() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var nonce: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var counter: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var tag: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBytesField()?.let { this.nonce = it }
                2 -> decoder.decodeSingularUInt32Field()?.let { this.counter = it }
                3 -> decoder.decodeSingularBytesField()?.let { this.tag = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.nonce.isNotEmpty()) {
            visitor.visitSingularBytesField(this.nonce, 1)
        }
        if (this.counter != 0u) {
            visitor.visitSingularUInt32Field(this.counter, 2)
        }
        if (this.tag.isNotEmpty()) {
            visitor.visitSingularBytesField(this.tag, 3)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signatures_AES_GCM_Response_Signature_Data) return false
        if (!this.nonce.contentEquals(other.nonce)) return false
        if (this.counter != other.counter) return false
        if (!this.tag.contentEquals(other.tag)) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.nonce.contentHashCode()
        hash = 31 * hash + this.counter.hashCode()
        hash = 31 * hash + this.tag.contentHashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Signatures_AES_GCM_Response_Signature_Data {
        val result = Signatures_AES_GCM_Response_Signature_Data()
        result.nonce = this.nonce
        result.counter = this.counter
        result.tag = this.tag
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Signatures.AES_GCM_Response_Signature_Data"

        fun with(block: Signatures_AES_GCM_Response_Signature_Data.() -> Unit): Signatures_AES_GCM_Response_Signature_Data =
            Signatures_AES_GCM_Response_Signature_Data().apply(block)
    }
}

class Signatures_HMAC_Signature_Data() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var tag: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBytesField()?.let { this.tag = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.tag.isNotEmpty()) {
            visitor.visitSingularBytesField(this.tag, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signatures_HMAC_Signature_Data) return false
        if (!this.tag.contentEquals(other.tag)) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.tag.contentHashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Signatures_HMAC_Signature_Data {
        val result = Signatures_HMAC_Signature_Data()
        result.tag = this.tag
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Signatures.HMAC_Signature_Data"

        fun with(block: Signatures_HMAC_Signature_Data.() -> Unit): Signatures_HMAC_Signature_Data =
            Signatures_HMAC_Signature_Data().apply(block)
    }
}

class Signatures_HMAC_Personalized_Signature_Data() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var epoch: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var counter: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var expiresAt: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var tag: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularBytesField()?.let { this.epoch = it }
                2 -> decoder.decodeSingularUInt32Field()?.let { this.counter = it }
                3 -> decoder.decodeSingularFixed32Field()?.let { this.expiresAt = it }
                4 -> decoder.decodeSingularBytesField()?.let { this.tag = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.epoch.isNotEmpty()) {
            visitor.visitSingularBytesField(this.epoch, 1)
        }
        if (this.counter != 0u) {
            visitor.visitSingularUInt32Field(this.counter, 2)
        }
        if (this.expiresAt != 0u) {
            visitor.visitSingularFixed32Field(this.expiresAt, 3)
        }
        if (this.tag.isNotEmpty()) {
            visitor.visitSingularBytesField(this.tag, 4)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signatures_HMAC_Personalized_Signature_Data) return false
        if (!this.epoch.contentEquals(other.epoch)) return false
        if (this.counter != other.counter) return false
        if (this.expiresAt != other.expiresAt) return false
        if (!this.tag.contentEquals(other.tag)) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.epoch.contentHashCode()
        hash = 31 * hash + this.counter.hashCode()
        hash = 31 * hash + this.expiresAt.hashCode()
        hash = 31 * hash + this.tag.contentHashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Signatures_HMAC_Personalized_Signature_Data {
        val result = Signatures_HMAC_Personalized_Signature_Data()
        result.epoch = this.epoch
        result.counter = this.counter
        result.expiresAt = this.expiresAt
        result.tag = this.tag
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Signatures.HMAC_Personalized_Signature_Data"

        fun with(block: Signatures_HMAC_Personalized_Signature_Data.() -> Unit): Signatures_HMAC_Personalized_Signature_Data =
            Signatures_HMAC_Personalized_Signature_Data().apply(block)
    }
}

class Signatures_SignatureData() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var signerIdentity: Signatures_KeyIdentity
        get() = this._signerIdentity ?: _protobufPending(1, { Signatures_KeyIdentity() }) { this._signerIdentity = it }
        set(value) {
            _protobufDropPending(1)
            this._signerIdentity = value.copy()
            _protobufMutated()
        }

    val hasSignerIdentity: Boolean
        get() = this._signerIdentity != null

    fun clearSignerIdentity() {
        _protobufDropPending(1)
        this._signerIdentity = null
        _protobufMutated()
    }

    var sigType: Signatures_SignatureData.OneOf_SigType?
        get() = this._sigType
        set(value) {
            this._protobufStore_sigType(this._protobufCopy_sigType(value))
            _protobufMutated()
        }

    var aesGcmPersonalizedData: Signatures_AES_GCM_Personalized_Signature_Data
        get() = (this._sigType as? Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData)?.value
            ?: _protobufPending(5, { Signatures_AES_GCM_Personalized_Signature_Data() }) { this._protobufStore_sigType(Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData(it)) }
        set(value) {
            this.sigType = Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData(value)
        }

    var sessionInfoTag: Signatures_HMAC_Signature_Data
        get() = (this._sigType as? Signatures_SignatureData.OneOf_SigType.sessionInfoTag)?.value
            ?: _protobufPending(6, { Signatures_HMAC_Signature_Data() }) { this._protobufStore_sigType(Signatures_SignatureData.OneOf_SigType.sessionInfoTag(it)) }
        set(value) {
            this.sigType = Signatures_SignatureData.OneOf_SigType.sessionInfoTag(value)
        }

    var hmacPersonalizedData: Signatures_HMAC_Personalized_Signature_Data
        get() = (this._sigType as? Signatures_SignatureData.OneOf_SigType.hmacPersonalizedData)?.value
            ?: _protobufPending(8, { Signatures_HMAC_Personalized_Signature_Data() }) { this._protobufStore_sigType(Signatures_SignatureData.OneOf_SigType.hmacPersonalizedData(it)) }
        set(value) {
            this.sigType = Signatures_SignatureData.OneOf_SigType.hmacPersonalizedData(value)
        }

    var aesGcmResponseData: Signatures_AES_GCM_Response_Signature_Data
        get() = (this._sigType as? Signatures_SignatureData.OneOf_SigType.aesGcmResponseData)?.value
            ?: _protobufPending(9, { Signatures_AES_GCM_Response_Signature_Data() }) { this._protobufStore_sigType(Signatures_SignatureData.OneOf_SigType.aesGcmResponseData(it)) }
        set(value) {
            this.sigType = Signatures_SignatureData.OneOf_SigType.aesGcmResponseData(value)
        }

    sealed class OneOf_SigType(value: Any) : ProtobufOneofCase(value) {
        class aesGcmPersonalizedData(val value: Signatures_AES_GCM_Personalized_Signature_Data) : OneOf_SigType(value)
        class sessionInfoTag(val value: Signatures_HMAC_Signature_Data) : OneOf_SigType(value)
        class hmacPersonalizedData(val value: Signatures_HMAC_Personalized_Signature_Data) : OneOf_SigType(value)
        class aesGcmResponseData(val value: Signatures_AES_GCM_Response_Signature_Data) : OneOf_SigType(value)
    }

    private var _signerIdentity: Signatures_KeyIdentity? = null
    private var _sigType: Signatures_SignatureData.OneOf_SigType? = null

    private fun _protobufStore_sigType(value: Signatures_SignatureData.OneOf_SigType?) {
        _protobufDropPending(5)
        _protobufDropPending(6)
        _protobufDropPending(8)
        _protobufDropPending(9)
        this._sigType = value
    }

    private fun _protobufCopy_sigType(value: Signatures_SignatureData.OneOf_SigType?): Signatures_SignatureData.OneOf_SigType? =
        when (value) {
            is Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData -> Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData(value.value.copy())
            is Signatures_SignatureData.OneOf_SigType.sessionInfoTag -> Signatures_SignatureData.OneOf_SigType.sessionInfoTag(value.value.copy())
            is Signatures_SignatureData.OneOf_SigType.hmacPersonalizedData -> Signatures_SignatureData.OneOf_SigType.hmacPersonalizedData(value.value.copy())
            is Signatures_SignatureData.OneOf_SigType.aesGcmResponseData -> Signatures_SignatureData.OneOf_SigType.aesGcmResponseData(value.value.copy())
            else -> value
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._signerIdentity) { Signatures_KeyIdentity() }?.let {
                    _protobufDropPending(1)
                    this._signerIdentity = it
                }
                5 -> decoder.decodeSingularMessageField((this._sigType as? Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData)?.value) { Signatures_AES_GCM_Personalized_Signature_Data() }
                    ?.let { this._protobufStore_sigType(Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData(it)) }
                6 -> decoder.decodeSingularMessageField((this._sigType as? Signatures_SignatureData.OneOf_SigType.sessionInfoTag)?.value) { Signatures_HMAC_Signature_Data() }
                    ?.let { this._protobufStore_sigType(Signatures_SignatureData.OneOf_SigType.sessionInfoTag(it)) }
                8 -> decoder.decodeSingularMessageField((this._sigType as? Signatures_SignatureData.OneOf_SigType.hmacPersonalizedData)?.value) { Signatures_HMAC_Personalized_Signature_Data() }
                    ?.let { this._protobufStore_sigType(Signatures_SignatureData.OneOf_SigType.hmacPersonalizedData(it)) }
                9 -> decoder.decodeSingularMessageField((this._sigType as? Signatures_SignatureData.OneOf_SigType.aesGcmResponseData)?.value) { Signatures_AES_GCM_Response_Signature_Data() }
                    ?.let { this._protobufStore_sigType(Signatures_SignatureData.OneOf_SigType.aesGcmResponseData(it)) }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._signerIdentity?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        (this._sigType as? Signatures_SignatureData.OneOf_SigType.aesGcmPersonalizedData)?.let {
            visitor.visitSingularMessageField(it.value, 5)
        }
        (this._sigType as? Signatures_SignatureData.OneOf_SigType.sessionInfoTag)?.let {
            visitor.visitSingularMessageField(it.value, 6)
        }
        (this._sigType as? Signatures_SignatureData.OneOf_SigType.hmacPersonalizedData)?.let {
            visitor.visitSingularMessageField(it.value, 8)
        }
        (this._sigType as? Signatures_SignatureData.OneOf_SigType.aesGcmResponseData)?.let {
            visitor.visitSingularMessageField(it.value, 9)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signatures_SignatureData) return false
        if (this._signerIdentity != other._signerIdentity) return false
        if (this._sigType != other._sigType) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._signerIdentity?.hashCode() ?: 0)
        hash = 31 * hash + (this._sigType?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Signatures_SignatureData {
        val result = Signatures_SignatureData()
        result._signerIdentity = this._signerIdentity?.copy()
        result._sigType = this._protobufCopy_sigType(this._sigType)
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Signatures.SignatureData"

        fun with(block: Signatures_SignatureData.() -> Unit): Signatures_SignatureData =
            Signatures_SignatureData().apply(block)
    }
}

class Signatures_GetSessionInfoRequest() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var keyIdentity: Signatures_KeyIdentity
        get() = this._keyIdentity ?: _protobufPending(1, { Signatures_KeyIdentity() }) { this._keyIdentity = it }
        set(value) {
            _protobufDropPending(1)
            this._keyIdentity = value.copy()
            _protobufMutated()
        }

    val hasKeyIdentity: Boolean
        get() = this._keyIdentity != null

    fun clearKeyIdentity() {
        _protobufDropPending(1)
        this._keyIdentity = null
        _protobufMutated()
    }

    private var _keyIdentity: Signatures_KeyIdentity? = null

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularMessageField(this._keyIdentity) { Signatures_KeyIdentity() }?.let {
                    _protobufDropPending(1)
                    this._keyIdentity = it
                }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        this._keyIdentity?.let {
            visitor.visitSingularMessageField(it, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signatures_GetSessionInfoRequest) return false
        if (this._keyIdentity != other._keyIdentity) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + (this._keyIdentity?.hashCode() ?: 0)
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Signatures_GetSessionInfoRequest {
        val result = Signatures_GetSessionInfoRequest()
        result._keyIdentity = this._keyIdentity?.copy()
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Signatures.GetSessionInfoRequest"

        fun with(block: Signatures_GetSessionInfoRequest.() -> Unit): Signatures_GetSessionInfoRequest =
            Signatures_GetSessionInfoRequest().apply(block)
    }
}

class Signatures_SessionInfo() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var counter: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var publicKey: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var epoch: ByteArray = ByteArray(0)
        set(value) {
            field = value
            _protobufMutated()
        }

    var clockTime: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    var status: Signatures_Session_Info_Status = Signatures_Session_Info_Status.ok
        set(value) {
            field = value
            _protobufForgetUnrecognized(5)
            _protobufMutated()
        }

    var handle: UInt = 0u
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularUInt32Field()?.let { this.counter = it }
                2 -> decoder.decodeSingularBytesField()?.let { this.publicKey = it }
                3 -> decoder.decodeSingularBytesField()?.let { this.epoch = it }
                4 -> decoder.decodeSingularFixed32Field()?.let { this.clockTime = it }
                5 -> decoder.decodeSingularOpenEnumField(Signatures_Session_Info_Status.UNRECOGNIZED) { Signatures_Session_Info_Status.fromRawValue(it) }
                    ?.let { this.status = it }
                6 -> decoder.decodeSingularUInt32Field()?.let { this.handle = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.counter != 0u) {
            visitor.visitSingularUInt32Field(this.counter, 1)
        }
        if (this.publicKey.isNotEmpty()) {
            visitor.visitSingularBytesField(this.publicKey, 2)
        }
        if (this.epoch.isNotEmpty()) {
            visitor.visitSingularBytesField(this.epoch, 3)
        }
        if (this.clockTime != 0u) {
            visitor.visitSingularFixed32Field(this.clockTime, 4)
        }
        if (this.status != Signatures_Session_Info_Status.ok && this.status != Signatures_Session_Info_Status.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.status.rawValue, 5)
        }
        if (this.handle != 0u) {
            visitor.visitSingularUInt32Field(this.handle, 6)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Signatures_SessionInfo) return false
        if (this.counter != other.counter) return false
        if (!this.publicKey.contentEquals(other.publicKey)) return false
        if (!this.epoch.contentEquals(other.epoch)) return false
        if (this.clockTime != other.clockTime) return false
        if (this.status != other.status) return false
        if (this.handle != other.handle) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.counter.hashCode()
        hash = 31 * hash + this.publicKey.contentHashCode()
        hash = 31 * hash + this.epoch.contentHashCode()
        hash = 31 * hash + this.clockTime.hashCode()
        hash = 31 * hash + this.status.hashCode()
        hash = 31 * hash + this.handle.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Signatures_SessionInfo {
        val result = Signatures_SessionInfo()
        result.counter = this.counter
        result.publicKey = this.publicKey
        result.epoch = this.epoch
        result.clockTime = this.clockTime
        result.status = this.status
        result.handle = this.handle
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Signatures.SessionInfo"

        fun with(block: Signatures_SessionInfo.() -> Unit): Signatures_SessionInfo =
            Signatures_SessionInfo().apply(block)
    }
}
