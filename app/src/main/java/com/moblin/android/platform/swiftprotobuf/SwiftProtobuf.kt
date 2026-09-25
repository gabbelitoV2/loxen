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
    if (this is GeneratedMessage) {
        _protobufMutated()
    }
}

abstract class GeneratedMessage : Message {
    private var protobufUnknownFields = ByteArray(0)

    override var unknownFields: ByteArray
        get() = protobufUnknownFields
        set(value) {
            protobufUnknownFields = value
            _protobufMutated()
        }

    private var protobufAttach: (() -> Unit)? = null
    private var protobufPendings: HashMap<Int, GeneratedMessage>? = null

    abstract fun copy(): GeneratedMessage

    override fun toString(): String {
        val name = javaClass.name.substringAfterLast('.').replace('$', '.')
        return "$name(${protobufHex(serializedData())})"
    }

    internal fun _protobufMutated() {
        val attach = protobufAttach ?: return
        protobufAttach = null
        attach()
    }

    internal fun <M : GeneratedMessage> _protobufPending(fieldNumber: Int, make: () -> M, attach: (M) -> Unit): M {
        val pendings = protobufPendings ?: HashMap<Int, GeneratedMessage>().also { protobufPendings = it }
        @Suppress("UNCHECKED_CAST")
        val existing = pendings[fieldNumber] as M?
        if (existing != null) {
            return existing
        }
        val pending = make()
        pendings[fieldNumber] = pending
        val child: GeneratedMessage = pending
        child.protobufAttach = {
            if (protobufPendings?.get(fieldNumber) === pending) {
                protobufPendings?.remove(fieldNumber)
                attach(pending)
                _protobufMutated()
            }
        }
        return pending
    }

    internal fun _protobufDropPending(fieldNumber: Int) {
        protobufPendings?.remove(fieldNumber)
    }

    internal fun _protobufForgetUnrecognized(vararg fieldNumbers: Int) {
        if (protobufUnknownFields.isNotEmpty()) {
            protobufUnknownFields = protobufWithoutVarints(protobufUnknownFields, fieldNumbers)
        }
    }
}

internal fun protobufWithoutVarints(bytes: ByteArray, fieldNumbers: IntArray): ByteArray =
    try {
        BinaryDecoder(bytes).withoutVarints(fieldNumbers)
    } catch (e: BinaryDecodingError) {
        bytes
    }

abstract class ProtobufOneofCase(private val protobufValue: Any) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || other.javaClass != javaClass) {
            return false
        }
        return protobufValueEquals(protobufValue, (other as ProtobufOneofCase).protobufValue)
    }

    override fun hashCode(): Int = 31 * javaClass.hashCode() + protobufValueHash(protobufValue)

    override fun toString(): String {
        val value = protobufValue
        val text = if (value is ByteArray) protobufHex(value) else value.toString()
        return "${javaClass.name.substringAfterLast('$')}($text)"
    }
}

internal class ProtobufList<T>(
    private val owner: GeneratedMessage?,
    private val unrecognized: T? = null,
    private val fieldNumber: Int = 0,
    private val copyElement: (T) -> T,
) : AbstractMutableList<T>() {
    private val storage = ArrayList<T>()

    override val size: Int
        get() = storage.size

    override fun get(index: Int): T = storage[index]

    override fun add(index: Int, element: T) {
        storage.add(index, copyElement(element))
        modCount += 1
        changed()
    }

    override fun removeAt(index: Int): T {
        val element = storage.removeAt(index)
        modCount += 1
        changed()
        return element
    }

    override fun set(index: Int, element: T): T {
        val previous = storage.set(index, copyElement(element))
        changed()
        return previous
    }

    override fun clear() {
        storage.clear()
        modCount += 1
        changed()
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val added = super.addAll(ArrayList(elements))
        changed()
        return added
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        val added = super.addAll(index, ArrayList(elements))
        changed()
        return added
    }

    private fun changed() {
        val owner = owner ?: return
        if (unrecognized != null && unrecognized !in storage) {
            owner._protobufForgetUnrecognized(fieldNumber)
        }
        owner._protobufMutated()
    }

    fun appendDecoded(element: T) {
        storage.add(element)
        modCount += 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is List<*> || other.size != size) {
            return false
        }
        for (index in 0 until size) {
            if (!protobufValueEquals(storage[index], other[index])) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        var hash = 1
        for (element in storage) {
            hash = 31 * hash + protobufValueHash(element)
        }
        return hash
    }
}

internal fun protobufHash(value: Float): Int = if (value == 0f) 0 else value.hashCode()

internal fun protobufHash(value: Float?): Int = if (value == null) 0 else protobufHash(value as Float)

internal fun protobufHash(value: Double): Int = if (value == 0.0) 0 else value.hashCode()

internal fun protobufHash(value: Double?): Int = if (value == null) 0 else protobufHash(value as Double)

internal fun protobufValueEquals(a: Any?, b: Any?): Boolean {
    if (a is ByteArray && b is ByteArray) {
        return a.contentEquals(b)
    }
    if (a is Float && b is Float) {
        val left: Float = a
        val right: Float = b
        return left == right
    }
    if (a is Double && b is Double) {
        val left: Double = a
        val right: Double = b
        return left == right
    }
    return a == b
}

internal fun protobufValueHash(value: Any?): Int = when (value) {
    null -> 0
    is ByteArray -> value.contentHashCode()
    is Float -> protobufHash(value as Float)
    is Double -> protobufHash(value as Double)
    else -> value.hashCode()
}

private fun protobufHex(bytes: ByteArray): String =
    bytes.joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

class Google_Protobuf_Timestamp() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    constructor(seconds: Long, nanos: Int = 0) : this() {
        this.seconds = seconds
        this.nanos = nanos
    }

    var seconds: Long = 0L
        set(value) {
            field = value
            _protobufMutated()
        }

    var nanos: Int = 0
        set(value) {
            field = value
            _protobufMutated()
        }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularInt64Field()?.let { seconds = it }
                2 -> decoder.decodeSingularInt32Field()?.let { nanos = it }
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (seconds != 0L) {
            visitor.visitSingularInt64Field(seconds, 1)
        }
        if (nanos != 0) {
            visitor.visitSingularInt32Field(nanos, 2)
        }
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Google_Protobuf_Timestamp) return false
        if (seconds != other.seconds) return false
        if (nanos != other.nanos) return false
        return unknownFields.contentEquals(other.unknownFields)
    }

    override fun hashCode(): Int {
        var hash = 0
        hash = 31 * hash + seconds.hashCode()
        hash = 31 * hash + nanos.hashCode()
        hash = 31 * hash + unknownFields.contentHashCode()
        return hash
    }

    override fun copy(): Google_Protobuf_Timestamp {
        val result = Google_Protobuf_Timestamp()
        result.seconds = seconds
        result.nanos = nanos
        result.unknownFields = unknownFields
        return result
    }

    companion object {
        const val protoMessageName: String = "google.protobuf.Timestamp"

        fun with(block: Google_Protobuf_Timestamp.() -> Unit): Google_Protobuf_Timestamp =
            Google_Protobuf_Timestamp().apply(block)
    }
}

private const val MESSAGE_DEPTH_LIMIT = 100

private fun ByteArrayOutputStream.putVarint(value: Long) {
    var remaining = value
    while ((remaining and 0x7FL.inv()) != 0L) {
        write(((remaining and 0x7FL) or 0x80L).toInt())
        remaining = remaining ushr 7
    }
    write(remaining.toInt())
}

private fun ByteArrayOutputStream.putFixed32(value: Int) {
    write(value and 0xFF)
    write((value ushr 8) and 0xFF)
    write((value ushr 16) and 0xFF)
    write((value ushr 24) and 0xFF)
}

private fun ByteArrayOutputStream.putFixed64(value: Long) {
    putFixed32(value.toInt())
    putFixed32((value ushr 32).toInt())
}

private fun zigZagEncoded32(value: Int): Long = ((value shl 1) xor (value shr 31)).toLong() and 0xFFFFFFFFL

private fun zigZagEncoded64(value: Long): Long = (value shl 1) xor (value shr 63)

private fun zigZagDecoded32(value: Int): Int = (value ushr 1) xor -(value and 1)

private fun zigZagDecoded64(value: Long): Long = (value ushr 1) xor -(value and 1L)

class BinaryDecoder(private val data: ByteArray, private val depth: Int = 0) {
    private var offset = 0
    private var fieldStart = 0
    private var fieldNumber = 0
    private var wireType = 0
    private var consumed = true
    private val unknown = ByteArrayOutputStream()
    private var unrecognized: LinkedHashMap<Int, ByteArray>? = null

    fun decodeFullObject(message: Message) {
        if (depth > MESSAGE_DEPTH_LIMIT) {
            throw BinaryDecodingError("messageDepthLimit")
        }
        message.decodeMessage(this)
        skipUnconsumedField()
        unrecognized?.values?.forEach { unknown.write(it) }
        if (unknown.size() > 0) {
            message.unknownFields += unknown.toByteArray()
        }
    }

    internal fun withoutVarints(fieldNumbers: IntArray): ByteArray {
        var removed = false
        while (true) {
            val number = nextFieldNumber() ?: break
            if (wireType == 0 && number in fieldNumbers) {
                decodeVarint()
                consumed = true
                removed = true
            }
        }
        return if (removed) unknown.toByteArray() else data
    }

    internal fun forgetUnrecognized(vararg fieldNumbers: Int) {
        val map = unrecognized ?: return
        for (number in fieldNumbers) {
            map.remove(number)
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
        return decodeString(decodeLengthDelimited())
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
        BinaryDecoder(decodeLengthDelimited(), depth + 1).decodeFullObject(message)
        appendElement(value, message)
    }

    internal fun decodeSingularInt64Field(): Long? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        return decodeVarint()
    }

    internal fun decodeSingularUInt32Field(): UInt? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        return decodeVarint().toUInt()
    }

    internal fun decodeSingularUInt64Field(): ULong? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        return decodeVarint().toULong()
    }

    internal fun decodeSingularSInt32Field(): Int? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        return zigZagDecoded32(decodeVarint().toInt())
    }

    internal fun decodeSingularSInt64Field(): Long? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        return zigZagDecoded64(decodeVarint())
    }

    internal fun decodeSingularFixed32Field(): UInt? {
        if (wireType != 5) {
            return null
        }
        consumed = true
        return decodeFixed32().toUInt()
    }

    internal fun decodeSingularFixed64Field(): ULong? {
        if (wireType != 1) {
            return null
        }
        consumed = true
        return decodeFixed64().toULong()
    }

    internal fun decodeSingularSFixed32Field(): Int? {
        if (wireType != 5) {
            return null
        }
        consumed = true
        return decodeFixed32()
    }

    internal fun decodeSingularSFixed64Field(): Long? {
        if (wireType != 1) {
            return null
        }
        consumed = true
        return decodeFixed64()
    }

    internal fun decodeSingularFloatField(): Float? {
        if (wireType != 5) {
            return null
        }
        consumed = true
        return Float.fromBits(decodeFixed32())
    }

    internal fun decodeSingularDoubleField(): Double? {
        if (wireType != 1) {
            return null
        }
        consumed = true
        return Double.fromBits(decodeFixed64())
    }

    internal fun <E> decodeSingularOpenEnumField(unrecognizedValue: E, fromRawValue: (Int) -> E): E? {
        if (wireType != 0) {
            return null
        }
        consumed = true
        val value = fromRawValue(decodeVarint().toInt())
        unrecognized?.remove(fieldNumber)
        if (value == unrecognizedValue) {
            val map = unrecognized ?: LinkedHashMap<Int, ByteArray>().also { unrecognized = it }
            map[fieldNumber] = data.copyOfRange(fieldStart, offset)
        }
        return value
    }

    internal fun <M : Message> decodeSingularMessageField(value: M?, makeMessage: () -> M): M? {
        if (wireType != 2) {
            return null
        }
        consumed = true
        val message = value ?: makeMessage()
        BinaryDecoder(decodeLengthDelimited(), depth + 1).decodeFullObject(message)
        return message
    }

    internal fun decodeRepeatedInt32Field(value: MutableList<Int>) {
        decodeRepeatedVarints { appendElement(value, it.toInt()) }
    }

    internal fun decodeRepeatedInt64Field(value: MutableList<Long>) {
        decodeRepeatedVarints { appendElement(value, it) }
    }

    internal fun decodeRepeatedUInt32Field(value: MutableList<UInt>) {
        decodeRepeatedVarints { appendElement(value, it.toUInt()) }
    }

    internal fun decodeRepeatedUInt64Field(value: MutableList<ULong>) {
        decodeRepeatedVarints { appendElement(value, it.toULong()) }
    }

    internal fun decodeRepeatedSInt32Field(value: MutableList<Int>) {
        decodeRepeatedVarints { appendElement(value, zigZagDecoded32(it.toInt())) }
    }

    internal fun decodeRepeatedSInt64Field(value: MutableList<Long>) {
        decodeRepeatedVarints { appendElement(value, zigZagDecoded64(it)) }
    }

    internal fun decodeRepeatedBoolField(value: MutableList<Boolean>) {
        decodeRepeatedVarints { appendElement(value, it != 0L) }
    }

    internal fun decodeRepeatedFixed32Field(value: MutableList<UInt>) {
        decodeRepeatedFixed32s { appendElement(value, it.toUInt()) }
    }

    internal fun decodeRepeatedSFixed32Field(value: MutableList<Int>) {
        decodeRepeatedFixed32s { appendElement(value, it) }
    }

    internal fun decodeRepeatedFloatField(value: MutableList<Float>) {
        decodeRepeatedFixed32s { appendElement(value, Float.fromBits(it)) }
    }

    internal fun decodeRepeatedFixed64Field(value: MutableList<ULong>) {
        decodeRepeatedFixed64s { appendElement(value, it.toULong()) }
    }

    internal fun decodeRepeatedSFixed64Field(value: MutableList<Long>) {
        decodeRepeatedFixed64s { appendElement(value, it) }
    }

    internal fun decodeRepeatedDoubleField(value: MutableList<Double>) {
        decodeRepeatedFixed64s { appendElement(value, Double.fromBits(it)) }
    }

    internal fun decodeRepeatedStringField(value: MutableList<String>) {
        decodeSingularStringField()?.let { appendElement(value, it) }
    }

    internal fun decodeRepeatedBytesField(value: MutableList<ByteArray>) {
        decodeSingularBytesField()?.let { appendElement(value, it) }
    }

    internal fun <E> decodeRepeatedOpenEnumField(value: MutableList<E>, unrecognizedValue: E, fromRawValue: (Int) -> E) {
        when (wireType) {
            0 -> {
                consumed = true
                val element = fromRawValue(decodeVarint().toInt())
                if (element == unrecognizedValue) {
                    unknown.write(data, fieldStart, offset - fieldStart)
                }
                appendElement(value, element)
            }
            2 -> {
                consumed = true
                val packed = BinaryDecoder(decodeLengthDelimited(), depth)
                while (packed.offset < packed.data.size) {
                    val rawValue = packed.decodeVarint().toInt()
                    val element = fromRawValue(rawValue)
                    if (element == unrecognizedValue) {
                        unknown.putVarint((fieldNumber shl 3).toLong())
                        unknown.putVarint(rawValue.toLong())
                    }
                    appendElement(value, element)
                }
            }
        }
    }

    private fun <T> appendElement(value: MutableList<T>, element: T) {
        if (value is ProtobufList<T>) {
            value.appendDecoded(element)
        } else {
            value.add(element)
        }
    }

    private inline fun decodeRepeatedVarints(append: (Long) -> Unit) {
        when (wireType) {
            0 -> {
                consumed = true
                append(decodeVarint())
            }
            2 -> {
                consumed = true
                val packed = BinaryDecoder(decodeLengthDelimited(), depth)
                while (packed.offset < packed.data.size) {
                    append(packed.decodeVarint())
                }
            }
        }
    }

    private inline fun decodeRepeatedFixed32s(append: (Int) -> Unit) {
        when (wireType) {
            5 -> {
                consumed = true
                append(decodeFixed32())
            }
            2 -> {
                consumed = true
                val packed = BinaryDecoder(decodeLengthDelimited(), depth)
                if (packed.data.size % 4 != 0) {
                    throw BinaryDecodingError("truncated")
                }
                while (packed.offset < packed.data.size) {
                    append(packed.decodeFixed32())
                }
            }
        }
    }

    private inline fun decodeRepeatedFixed64s(append: (Long) -> Unit) {
        when (wireType) {
            1 -> {
                consumed = true
                append(decodeFixed64())
            }
            2 -> {
                consumed = true
                val packed = BinaryDecoder(decodeLengthDelimited(), depth)
                if (packed.data.size % 8 != 0) {
                    throw BinaryDecodingError("truncated")
                }
                while (packed.offset < packed.data.size) {
                    append(packed.decodeFixed64())
                }
            }
        }
    }

    private fun skipUnconsumedField() {
        if (consumed) {
            return
        }
        skipField(wireType, fieldNumber, 0)
        unknown.write(data, fieldStart, offset - fieldStart)
        consumed = true
    }

    private fun skipField(wireType: Int, fieldNumber: Int, groupDepth: Int) {
        when (wireType) {
            0 -> decodeVarint()
            1 -> skip(8)
            2 -> skip(decodeLength())
            3 -> skipGroup(fieldNumber, groupDepth + 1)
            5 -> skip(4)
            else -> throw BinaryDecodingError("malformedProtobuf")
        }
    }

    private fun skipGroup(fieldNumber: Int, groupDepth: Int) {
        if (depth + groupDepth > MESSAGE_DEPTH_LIMIT) {
            throw BinaryDecodingError("messageDepthLimit")
        }
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
            skipField(type, number, groupDepth)
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

    private fun decodeFixed32(): Int {
        if (data.size - offset < 4) {
            throw BinaryDecodingError("truncated")
        }
        var value = 0
        for (index in 0 until 4) {
            value = value or ((data[offset + index].toInt() and 0xFF) shl (8 * index))
        }
        offset += 4
        return value
    }

    private fun decodeFixed64(): Long {
        if (data.size - offset < 8) {
            throw BinaryDecodingError("truncated")
        }
        var value = 0L
        for (index in 0 until 8) {
            value = value or ((data[offset + index].toLong() and 0xFFL) shl (8 * index))
        }
        offset += 8
        return value
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

    private fun decodeString(bytes: ByteArray): String {
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: CharacterCodingException) {
            throw BinaryDecodingError("invalidUTF8")
        }
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
            visitSingularMessageField(message, fieldNumber)
        }
    }

    fun visitUnknown(bytes: ByteArray) {
        output.write(bytes)
    }

    internal fun visitSingularInt64Field(value: Long, fieldNumber: Int) {
        putTag(fieldNumber, 0)
        output.putVarint(value)
    }

    internal fun visitSingularUInt32Field(value: UInt, fieldNumber: Int) {
        putTag(fieldNumber, 0)
        output.putVarint(value.toLong())
    }

    internal fun visitSingularUInt64Field(value: ULong, fieldNumber: Int) {
        putTag(fieldNumber, 0)
        output.putVarint(value.toLong())
    }

    internal fun visitSingularSInt32Field(value: Int, fieldNumber: Int) {
        putTag(fieldNumber, 0)
        output.putVarint(zigZagEncoded32(value))
    }

    internal fun visitSingularSInt64Field(value: Long, fieldNumber: Int) {
        putTag(fieldNumber, 0)
        output.putVarint(zigZagEncoded64(value))
    }

    internal fun visitSingularFixed32Field(value: UInt, fieldNumber: Int) {
        putTag(fieldNumber, 5)
        output.putFixed32(value.toInt())
    }

    internal fun visitSingularFixed64Field(value: ULong, fieldNumber: Int) {
        putTag(fieldNumber, 1)
        output.putFixed64(value.toLong())
    }

    internal fun visitSingularSFixed32Field(value: Int, fieldNumber: Int) {
        putTag(fieldNumber, 5)
        output.putFixed32(value)
    }

    internal fun visitSingularSFixed64Field(value: Long, fieldNumber: Int) {
        putTag(fieldNumber, 1)
        output.putFixed64(value)
    }

    internal fun visitSingularFloatField(value: Float, fieldNumber: Int) {
        putTag(fieldNumber, 5)
        output.putFixed32(value.toRawBits())
    }

    internal fun visitSingularDoubleField(value: Double, fieldNumber: Int) {
        putTag(fieldNumber, 1)
        output.putFixed64(value.toRawBits())
    }

    internal fun visitSingularMessageField(value: Message, fieldNumber: Int) {
        val visitor = BinaryEncodingVisitor()
        value.traverse(visitor)
        visitSingularBytesField(visitor.data, fieldNumber)
    }

    internal fun visitPackedInt32Field(value: List<Int>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putVarint(it.toLong()) }
    }

    internal fun visitPackedInt64Field(value: List<Long>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putVarint(it) }
    }

    internal fun visitPackedUInt32Field(value: List<UInt>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putVarint(it.toLong()) }
    }

    internal fun visitPackedUInt64Field(value: List<ULong>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putVarint(it.toLong()) }
    }

    internal fun visitPackedSInt32Field(value: List<Int>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putVarint(zigZagEncoded32(it)) }
    }

    internal fun visitPackedSInt64Field(value: List<Long>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putVarint(zigZagEncoded64(it)) }
    }

    internal fun visitPackedBoolField(value: List<Boolean>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putVarint(if (it) 1 else 0) }
    }

    internal fun visitPackedEnumField(value: List<Int>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putVarint(it.toLong()) }
    }

    internal fun visitPackedFixed32Field(value: List<UInt>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putFixed32(it.toInt()) }
    }

    internal fun visitPackedSFixed32Field(value: List<Int>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putFixed32(it) }
    }

    internal fun visitPackedFloatField(value: List<Float>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putFixed32(it.toRawBits()) }
    }

    internal fun visitPackedFixed64Field(value: List<ULong>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putFixed64(it.toLong()) }
    }

    internal fun visitPackedSFixed64Field(value: List<Long>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putFixed64(it) }
    }

    internal fun visitPackedDoubleField(value: List<Double>, fieldNumber: Int) {
        putPacked(value, fieldNumber) { putFixed64(it.toRawBits()) }
    }

    internal fun visitRepeatedInt32Field(value: List<Int>, fieldNumber: Int) {
        value.forEach { visitSingularInt32Field(it, fieldNumber) }
    }

    internal fun visitRepeatedInt64Field(value: List<Long>, fieldNumber: Int) {
        value.forEach { visitSingularInt64Field(it, fieldNumber) }
    }

    internal fun visitRepeatedUInt32Field(value: List<UInt>, fieldNumber: Int) {
        value.forEach { visitSingularUInt32Field(it, fieldNumber) }
    }

    internal fun visitRepeatedUInt64Field(value: List<ULong>, fieldNumber: Int) {
        value.forEach { visitSingularUInt64Field(it, fieldNumber) }
    }

    internal fun visitRepeatedSInt32Field(value: List<Int>, fieldNumber: Int) {
        value.forEach { visitSingularSInt32Field(it, fieldNumber) }
    }

    internal fun visitRepeatedSInt64Field(value: List<Long>, fieldNumber: Int) {
        value.forEach { visitSingularSInt64Field(it, fieldNumber) }
    }

    internal fun visitRepeatedBoolField(value: List<Boolean>, fieldNumber: Int) {
        value.forEach { visitSingularBoolField(it, fieldNumber) }
    }

    internal fun visitRepeatedFixed32Field(value: List<UInt>, fieldNumber: Int) {
        value.forEach { visitSingularFixed32Field(it, fieldNumber) }
    }

    internal fun visitRepeatedSFixed32Field(value: List<Int>, fieldNumber: Int) {
        value.forEach { visitSingularSFixed32Field(it, fieldNumber) }
    }

    internal fun visitRepeatedFloatField(value: List<Float>, fieldNumber: Int) {
        value.forEach { visitSingularFloatField(it, fieldNumber) }
    }

    internal fun visitRepeatedFixed64Field(value: List<ULong>, fieldNumber: Int) {
        value.forEach { visitSingularFixed64Field(it, fieldNumber) }
    }

    internal fun visitRepeatedSFixed64Field(value: List<Long>, fieldNumber: Int) {
        value.forEach { visitSingularSFixed64Field(it, fieldNumber) }
    }

    internal fun visitRepeatedDoubleField(value: List<Double>, fieldNumber: Int) {
        value.forEach { visitSingularDoubleField(it, fieldNumber) }
    }

    internal fun visitRepeatedStringField(value: List<String>, fieldNumber: Int) {
        value.forEach { visitSingularStringField(it, fieldNumber) }
    }

    internal fun visitRepeatedBytesField(value: List<ByteArray>, fieldNumber: Int) {
        value.forEach { visitSingularBytesField(it, fieldNumber) }
    }

    private inline fun <T> putPacked(value: List<T>, fieldNumber: Int, put: ByteArrayOutputStream.(T) -> Unit) {
        if (value.isEmpty()) {
            return
        }
        val packed = ByteArrayOutputStream()
        for (element in value) {
            packed.put(element)
        }
        putTag(fieldNumber, 2)
        output.putVarint(packed.size().toLong())
        packed.writeTo(output)
    }

    private fun putTag(fieldNumber: Int, wireType: Int) {
        output.putVarint(((fieldNumber shl 3) or wireType).toLong())
    }
}
