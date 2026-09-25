package com.moblin.android.integrations.tesla.protobuf

import com.moblin.android.platform.swiftprotobuf.BinaryDecoder
import com.moblin.android.platform.swiftprotobuf.BinaryEncodingVisitor
import com.moblin.android.platform.swiftprotobuf.GeneratedMessage
import com.moblin.android.platform.swiftprotobuf.merge

enum class Errors_GenericError_E(val rawValue: Int) {
    genericerrorNone(0),
    genericerrorUnknown(1),
    genericerrorClosuresOpen(2),
    genericerrorAlreadyOn(3),
    genericerrorDisabledForUserCommand(4),
    genericerrorVehicleNotInPark(5),
    genericerrorUnauthorized(6),
    genericerrorNotAllowedOverTransport(7),
    UNRECOGNIZED(-1),
    ;

    companion object {
        val allCases: List<Errors_GenericError_E> = listOf(
            genericerrorNone,
            genericerrorUnknown,
            genericerrorClosuresOpen,
            genericerrorAlreadyOn,
            genericerrorDisabledForUserCommand,
            genericerrorVehicleNotInPark,
            genericerrorUnauthorized,
            genericerrorNotAllowedOverTransport,
        )

        fun fromRawValue(rawValue: Int): Errors_GenericError_E =
            when (rawValue) {
                0 -> genericerrorNone
                1 -> genericerrorUnknown
                2 -> genericerrorClosuresOpen
                3 -> genericerrorAlreadyOn
                4 -> genericerrorDisabledForUserCommand
                5 -> genericerrorVehicleNotInPark
                6 -> genericerrorUnauthorized
                7 -> genericerrorNotAllowedOverTransport
                else -> UNRECOGNIZED
            }
    }
}

class Errors_NominalError() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var genericError: Errors_GenericError_E = Errors_GenericError_E.genericerrorNone
        set(value) {
            field = value
            _protobufForgetUnrecognized(1)
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularOpenEnumField(Errors_GenericError_E.UNRECOGNIZED) { Errors_GenericError_E.fromRawValue(it) }
                    ?.let { this.genericError = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (this.genericError != Errors_GenericError_E.genericerrorNone && this.genericError != Errors_GenericError_E.UNRECOGNIZED) {
            visitor.visitSingularEnumField(this.genericError.rawValue, 1)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Errors_NominalError) return false
        if (this.genericError != other.genericError) return false
        return this.unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + this.genericError.hashCode()
        hash = 31 * hash + this.unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Errors_NominalError {
        val result = Errors_NominalError()
        result.genericError = this.genericError
        result.unknownFields = this.unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "Errors.NominalError"

        fun with(block: Errors_NominalError.() -> Unit): Errors_NominalError =
            Errors_NominalError().apply(block)
    }
}
