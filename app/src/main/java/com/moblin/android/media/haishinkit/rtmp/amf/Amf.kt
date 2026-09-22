package com.moblin.android.media.haishinkit.rtmp.amf

import com.moblin.android.media.haishinkit.util.ByteReader
import com.moblin.android.media.haishinkit.util.ByteWriter
import java.time.Instant
import kotlin.math.roundToLong
import kotlin.math.truncate

typealias AsObject = Map<String, AsValue>

data class AsTypedObject(
    val type: String,
    val value: AsObject,
)

data class AsEcmaArray(
    val items: Map<String, AsValue> = emptyMap(),
) {
    fun get(key: String): AsValue {
        return items[key] ?: throw NoSuchElementException("Not found")
    }
}

data class AsXmlDocument(
    val data: String,
)

sealed class AsValue {
    data class Number(val value: Double) : AsValue()

    data class Bool(val value: Boolean) : AsValue()

    data class String(val value: kotlin.String) : AsValue()

    data class Object(val value: AsObject) : AsValue()

    data object Null : AsValue()

    data object Undefined : AsValue()

    data object Reference : AsValue()

    data class EcmaArray(val value: AsEcmaArray) : AsValue()

    data class StrictArray(val value: List<AsValue>) : AsValue()

    data class Date(val value: Instant) : AsValue()

    data object Unsupported : AsValue()

    data class XmlDocument(val value: AsXmlDocument) : AsValue()

    data class TypedObject(val value: AsTypedObject) : AsValue()

    data object Avmplush : AsValue()
}

sealed class AmfError(message: String) : Exception(message) {
    object ArrayTooBig : AmfError("arrayTooBig")

    object NotObjectEnd : AmfError("notObjectEnd")

    object UnexpectedObjectEnd : AmfError("unexpectedObjectEnd")

    object NotNumber : AmfError("notNumber")

    object NotString : AmfError("notString")

    object NotObject : AmfError("notObject")

    object NotAmf0 : AmfError("notAmf0")

    object TooDeep : AmfError("tooDeep")
}

private enum class Amf0Type(val rawValue: UByte) {
    Number(0x00.toUByte()),
    Bool(0x01.toUByte()),
    String(0x02.toUByte()),
    Object(0x03.toUByte()),
    Null(0x05.toUByte()),
    Undefined(0x06.toUByte()),
    Reference(0x07.toUByte()),
    EcmaArray(0x08.toUByte()),
    ObjectEnd(0x09.toUByte()),
    StrictArray(0x0A.toUByte()),
    Date(0x0B.toUByte()),
    LongString(0x0C.toUByte()),
    Unsupported(0x0D.toUByte()),
    XmlDocument(0x0F.toUByte()),
    TypedObject(0x10.toUByte()),
    Avmplush(0x11.toUByte()),
    ;

    companion object {
        fun fromRawValue(rawValue: UByte): Amf0Type? {
            return entries.firstOrNull { it.rawValue == rawValue }
        }
    }
}

class Amf0Encoder : ByteWriter {
    fun encode(value: AsValue) {
        when (value) {
            is AsValue.Number -> encodeDouble(value.value)
            is AsValue.Date -> encodeDate(value.value)
            is AsValue.String -> encodeString(value.value)
            is AsValue.Bool -> encodeBool(value.value)
            is AsValue.EcmaArray -> encodeEcmaArray(value.value)
            is AsValue.Object -> encodeAsObject(value.value)
            AsValue.Null -> writeAmf0Type(Amf0Type.Null)
            else -> writeAmf0Type(Amf0Type.Undefined)
        }
    }

    private fun encodeDouble(value: Double) {
        writeAmf0Type(Amf0Type.Number)
        writeDouble(value)
    }

    private fun encodeBool(value: Boolean) {
        writeBytes(
            byteArrayOf(
                Amf0Type.Bool.rawValue.toByte(),
                if (value) 0x01.toByte() else 0x00.toByte(),
            ),
        )
    }

    private fun encodeString(value: String) {
        val data = value.toByteArray(Charsets.UTF_8)
        if (data.size.toUInt() > UShort.MAX_VALUE.toUInt()) {
            writeAmf0Type(Amf0Type.LongString)
            encodeLongString(data)
        } else {
            writeAmf0Type(Amf0Type.String)
            encodeShortString(data)
        }
    }

    private fun encodeAsObject(value: AsObject) {
        writeAmf0Type(Amf0Type.Object)
        for ((key, data) in value) {
            encodeShortString(key)
            encode(data)
        }
        encodeShortString("")
        writeAmf0Type(Amf0Type.ObjectEnd)
    }

    private fun encodeEcmaArray(value: AsEcmaArray) {
    }

    private fun encodeDate(value: Instant) {
        writeAmf0Type(Amf0Type.Date)
        writeDouble(value.epochSecond * 1000.0 + value.nano / 1_000_000.0)
        writeUInt16(0.toUShort())
    }

    private fun encodeShortString(value: String) {
        encodeShortString(value.toByteArray(Charsets.UTF_8))
    }

    private fun encodeShortString(data: ByteArray) {
        writeUInt16(data.size.toUShort())
        writeBytes(data)
    }

    private fun encodeLongString(data: ByteArray) {
        writeUInt32(data.size.toUInt())
        writeBytes(data)
    }

    private fun writeAmf0Type(value: Amf0Type) {
        writeUInt8(value.rawValue)
    }
}

class Amf0Decoder : ByteReader {
    private var depth = 0

    fun decode(): AsValue {
        if (depth >= 32) {
            throw AmfError.TooDeep
        }
        depth += 1
        try {
            val type = readAmf0Type()
            return when (type) {
                Amf0Type.Number -> AsValue.Number(decodeDoubleValue())
                Amf0Type.Bool -> AsValue.Bool(decodeBoolValue())
                Amf0Type.String -> AsValue.String(decodeStringValue())
                Amf0Type.Object -> AsValue.Object(decodeObjectValue())
                Amf0Type.Null -> AsValue.Null
                Amf0Type.Undefined -> AsValue.Undefined
                Amf0Type.Reference -> AsValue.Reference
                Amf0Type.EcmaArray -> AsValue.EcmaArray(decodeEcmaArrayValue())
                Amf0Type.StrictArray -> AsValue.StrictArray(decodeStrictArrayValue())
                Amf0Type.Date -> AsValue.Date(decodeDateValue())
                Amf0Type.LongString -> AsValue.String(decodeLongStringValue())
                Amf0Type.Unsupported -> AsValue.Unsupported
                Amf0Type.XmlDocument -> AsValue.XmlDocument(decodeXmlDocumentValue())
                Amf0Type.TypedObject -> AsValue.TypedObject(decodeTypedObjectValue())
                Amf0Type.Avmplush -> AsValue.Avmplush
                Amf0Type.ObjectEnd -> throw AmfError.UnexpectedObjectEnd
            }
        } finally {
            depth -= 1
        }
    }

    fun decodeInt(): Int {
        if (readAmf0Type() != Amf0Type.Number) {
            throw AmfError.NotNumber
        }
        val rounded = truncate(decodeDoubleValue())
        if (rounded.isNaN() || rounded < Int.MIN_VALUE.toDouble() || rounded > Int.MAX_VALUE.toDouble()) {
            throw AmfError.NotNumber
        }
        return rounded.toInt()
    }

    fun decodeString(): String {
        return when (readAmf0Type()) {
            Amf0Type.String -> decodeStringValue()
            Amf0Type.LongString -> decodeLongStringValue()
            else -> throw AmfError.NotString
        }
    }

    fun decodeObject(): AsObject {
        return when (readAmf0Type()) {
            Amf0Type.Null -> emptyMap<String, AsValue>()
            Amf0Type.Object -> decodeObjectValue()
            else -> throw AmfError.NotObject
        }
    }

    private fun decodeObjectValue(): AsObject {
        val obj = mutableMapOf<String, AsValue>()
        while (true) {
            val key = decodeStringValue()
            if (key.isEmpty()) {
                break
            }
            obj[key] = decode()
        }
        parseObjectEnd()
        return obj
    }

    private fun decodeDoubleValue(): Double {
        return readDouble()
    }

    private fun decodeBoolValue(): Boolean {
        return readUInt8() == 0x01.toUByte()
    }

    private fun decodeEcmaArrayValue(): AsEcmaArray {
        readNumberOfArrayElements()
        return AsEcmaArray(decodeObjectValue())
    }

    private fun decodeStrictArrayValue(): List<AsValue> {
        val numberOfElements = readNumberOfArrayElements()
        val array = mutableListOf<AsValue>()
        repeat(numberOfElements.toInt()) {
            array.add(decode())
        }
        return array
    }

    private fun readNumberOfArrayElements(): UInt {
        val numberOfElements = readUInt32()
        if (numberOfElements >= 128u) {
            throw AmfError.ArrayTooBig
        }
        return numberOfElements
    }

    private fun decodeDateValue(): Instant {
        val seconds = readDouble() / 1000.0
        val wholeSeconds = truncate(seconds).toLong()
        val nanos = ((seconds - wholeSeconds) * 1_000_000_000.0).roundToLong()
        readUInt16()
        return Instant.ofEpochSecond(wholeSeconds, nanos)
    }

    private fun decodeXmlDocumentValue(): AsXmlDocument {
        return AsXmlDocument(decodeLongStringValue())
    }

    private fun decodeTypedObjectValue(): AsTypedObject {
        val type = decodeStringValue()
        val value = decodeObjectValue()
        return AsTypedObject(type = type, value = value)
    }

    private fun decodeStringValue(): String {
        return readUtf8Bytes(readUInt16().toInt())
    }

    private fun decodeLongStringValue(): String {
        return readUtf8Bytes(readUInt32().toInt())
    }

    private fun readAmf0Type(): Amf0Type {
        val value = readUInt8()
        return Amf0Type.fromRawValue(value) ?: throw AmfError.NotAmf0
    }

    private fun parseObjectEnd() {
        if (readUInt8() != Amf0Type.ObjectEnd.rawValue) {
            throw AmfError.NotObjectEnd
        }
    }
}
