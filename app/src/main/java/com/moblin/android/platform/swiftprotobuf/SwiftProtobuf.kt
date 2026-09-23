package com.moblin.android.platform.swiftprotobuf

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

class BinaryDecodingError(message: String) : Exception(message)

class BinaryEncodingError(message: String) : Exception(message)

interface Message {
    var unknownFields: ByteArray

    val isInitialized: Boolean
        get() = true

    fun decodeMessage(decoder: BinaryDecoder)

    fun traverse(visitor: BinaryEncodingVisitor)
}

fun Message.serializedData(): ByteArray {
    if (!isInitialized) {
        throw BinaryEncodingError("missingRequiredFields")
    }
    val visitor = BinaryEncodingVisitor()
    traverse(visitor)
    return visitor.data
}

fun Message.merge(serializedBytes: ByteArray) {
    BinaryDecoder(serializedBytes).decodeFullObject(this)
    if (!isInitialized) {
        throw BinaryDecodingError("missingRequiredFields")
    }
}

private fun ByteArrayOutputStream.putVarint(value: Long) {
    var remaining = value
    while ((remaining and 0x7FL.inv()) != 0L) {
        write(((remaining and 0x7FL) or 0x80L).toInt())
        remaining = remaining ushr 7
    }
    write(remaining.toInt())
}

class BinaryDecoder(private val data: ByteArray) {
    private var offset = 0
    private var fieldStart = 0
    private var fieldNumber = 0
    private var wireType = 0
    private var consumed = true
    private val unknown = ByteArrayOutputStream()

    fun decodeFullObject(message: Message) {
        message.decodeMessage(this)
        skipUnconsumedField()
        if (unknown.size() > 0) {
            message.unknownFields += unknown.toByteArray()
        }
    }

    fun nextFieldNumber(): Int? {
        skipUnconsumedField()
        if (offset == data.size) {
            return null
        }
        fieldStart = offset
        val tag = decodeVarint()
        fieldNumber = (tag ushr 3).toInt()
        wireType = (tag and 7).toInt()
        if ((tag ushr 32) != 0L || fieldNumber == 0 || wireType == 4 || wireType > 5) {
            throw BinaryDecodingError("malformedProtobuf")
        }
        consumed = false
        return fieldNumber
    }

    fun decodeSingularInt32Field(): Int? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        return decodeVarint().toInt()
    }

    fun decodeSingularBoolField(): Boolean? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        return decodeVarint() != 0L
    }

    fun decodeSingularStringField(): String? {
        if (wireType != 2) {
            return null
        }
        consumed = true
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(decodeLengthDelimited()))
                .toString()
        } catch (e: CharacterCodingException) {
            throw BinaryDecodingError("invalidUTF8")
        }
    }

    fun decodeSingularBytesField(): ByteArray? {
        if (wireType != 2) {
            return null
        }
        consumed = true
        return decodeLengthDelimited()
    }

    fun <E> decodeSingularEnumField(fromRawValue: (Int) -> E?): E? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        val value = fromRawValue(decodeVarint().toInt())
        if (value == null) {
            unknown.write(data, fieldStart, offset - fieldStart)
        }
        return value
    }

    fun <E> decodeRepeatedEnumField(value: MutableList<E>, fromRawValue: (Int) -> E?) {
        when (wireType) {
            0 -> decodeSingularEnumField(fromRawValue)?.let { value.add(it) }
            2 -> {
                consumed = true
                val packed = BinaryDecoder(decodeLengthDelimited())
                while (packed.offset < packed.data.size) {
                    val rawValue = packed.decodeVarint()
                    val element = fromRawValue(rawValue.toInt())
                    if (element != null) {
                        value.add(element)
                    } else {
                        unknown.putVarint((fieldNumber shl 3).toLong())
                        unknown.putVarint(rawValue)
                    }
                }
            }
        }
    }

    fun <M : Message> decodeRepeatedMessageField(value: MutableList<M>, makeMessage: () -> M) {
        if (wireType != 2) {
            return
        }
        consumed = true
        val message = makeMessage()
        BinaryDecoder(decodeLengthDelimited()).decodeFullObject(message)
        value.add(message)
    }

    private fun skipUnconsumedField() {
        if (consumed) {
            return
        }
        skipField(wireType, fieldNumber)
        unknown.write(data, fieldStart, offset - fieldStart)
        consumed = true
    }

    private fun skipField(wireType: Int, fieldNumber: Int) {
        when (wireType) {
            0 -> decodeVarint()
            1 -> skip(8)
            2 -> skip(decodeLength())
            3 -> skipGroup(fieldNumber)
            5 -> skip(4)
            else -> throw BinaryDecodingError("malformedProtobuf")
        }
    }

    private fun skipGroup(fieldNumber: Int) {
        while (true) {
            if (offset == data.size) {
                throw BinaryDecodingError("truncated")
            }
            val tag = decodeVarint()
            val number = (tag ushr 3).toInt()
            val type = (tag and 7).toInt()
            if (type == 4) {
                if (number != fieldNumber) {
                    throw BinaryDecodingError("malformedProtobuf")
                }
                return
            }
            skipField(type, number)
        }
    }

    private fun skip(count: Int) {
        if (count > data.size - offset) {
            throw BinaryDecodingError("truncated")
        }
        offset += count
    }

    private fun decodeVarint(): Long {
        var value = 0L
        var shift = 0
        while (true) {
            if (offset == data.size) {
                throw BinaryDecodingError("truncated")
            }
            if (shift >= 64) {
                throw BinaryDecodingError("malformedProtobuf")
            }
            val byte = data[offset].toInt() and 0xFF
            offset += 1
            value = value or ((byte and 0x7F).toLong() shl shift)
            if ((byte and 0x80) == 0) {
                return value
            }
            shift += 7
        }
    }

    private fun decodeLength(): Int {
        val length = decodeVarint()
        if (length < 0 || length > Int.MAX_VALUE) {
            throw BinaryDecodingError("malformedProtobuf")
        }
        if (length > data.size - offset) {
            throw BinaryDecodingError("truncated")
        }
        return length.toInt()
    }

    private fun decodeLengthDelimited(): ByteArray {
        val length = decodeLength()
        val bytes = data.copyOfRange(offset, offset + length)
        offset += length
        return bytes
    }
}

class BinaryEncodingVisitor {
    private val output = ByteArrayOutputStream()

    val data: ByteArray
        get() = output.toByteArray()

    fun visitSingularInt32Field(value: Int, fieldNumber: Int) {
        putTag(fieldNumber, 0)
        output.putVarint(value.toLong())
    }

    fun visitSingularBoolField(value: Boolean, fieldNumber: Int) {
        putTag(fieldNumber, 0)
        output.putVarint(if (value) 1 else 0)
    }

    fun visitSingularEnumField(value: Int, fieldNumber: Int) {
        visitSingularInt32Field(value, fieldNumber)
    }

    fun visitSingularStringField(value: String, fieldNumber: Int) {
        visitSingularBytesField(value.encodeToByteArray(), fieldNumber)
    }

    fun visitSingularBytesField(value: ByteArray, fieldNumber: Int) {
        putTag(fieldNumber, 2)
        output.putVarint(value.size.toLong())
        output.write(value)
    }

    fun visitRepeatedEnumField(value: List<Int>, fieldNumber: Int) {
        for (element in value) {
            visitSingularEnumField(element, fieldNumber)
        }
    }

    fun visitRepeatedMessageField(value: List<Message>, fieldNumber: Int) {
        for (message in value) {
            val visitor = BinaryEncodingVisitor()
            message.traverse(visitor)
            visitSingularBytesField(visitor.data, fieldNumber)
        }
    }

    fun visitUnknown(bytes: ByteArray) {
        output.write(bytes)
    }

    private fun putTag(fieldNumber: Int, wireType: Int) {
        output.putVarint(((fieldNumber shl 3) or wireType).toLong())
    }
}
