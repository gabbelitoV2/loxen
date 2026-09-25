package com.moblin.android.platform.swiftprotobuf

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class ScalarsMessage() : GeneratedMessage() {
    constructor(serializedBytes: ByteArray) : this() {
        merge(serializedBytes)
    }

    var sint64: Long = 0L
    var fixed64: ULong = 0uL
    var sfixed32: Int = 0
    var sfixed64: Long = 0L
    val int32s = ProtobufList<Int>(this) { it }
    val sint64s = ProtobufList<Long>(this) { it }
    val doubles = ProtobufList<Double>(this) { it }
    val bools = ProtobufList<Boolean>(this) { it }
    val strings = ProtobufList<String>(this) { it }
    val bytes = ProtobufList<ByteArray>(this) { it }
    var child: ScalarsMessage? = null
    val fixed32s = ProtobufList<UInt>(this) { it }

    override fun decodeMessage(decoder: BinaryDecoder) {
        while (true) {
            when (decoder.nextFieldNumber() ?: return) {
                1 -> decoder.decodeSingularSInt64Field()?.let { sint64 = it }
                2 -> decoder.decodeSingularFixed64Field()?.let { fixed64 = it }
                3 -> decoder.decodeSingularSFixed32Field()?.let { sfixed32 = it }
                4 -> decoder.decodeSingularSFixed64Field()?.let { sfixed64 = it }
                5 -> decoder.decodeRepeatedInt32Field(int32s)
                6 -> decoder.decodeRepeatedSInt64Field(sint64s)
                7 -> decoder.decodeRepeatedDoubleField(doubles)
                8 -> decoder.decodeRepeatedBoolField(bools)
                9 -> decoder.decodeRepeatedStringField(strings)
                10 -> decoder.decodeRepeatedBytesField(bytes)
                11 -> decoder.decodeSingularMessageField(child) { ScalarsMessage() }?.let { child = it }
                12 -> decoder.decodeRepeatedFixed32Field(fixed32s)
            }
        }
    }

    override fun traverse(visitor: BinaryEncodingVisitor) {
        if (sint64 != 0L) visitor.visitSingularSInt64Field(sint64, 1)
        if (fixed64 != 0uL) visitor.visitSingularFixed64Field(fixed64, 2)
        if (sfixed32 != 0) visitor.visitSingularSFixed32Field(sfixed32, 3)
        if (sfixed64 != 0L) visitor.visitSingularSFixed64Field(sfixed64, 4)
        visitor.visitPackedInt32Field(int32s, 5)
        visitor.visitPackedSInt64Field(sint64s, 6)
        visitor.visitPackedDoubleField(doubles, 7)
        visitor.visitPackedBoolField(bools, 8)
        visitor.visitRepeatedStringField(strings, 9)
        visitor.visitRepeatedBytesField(bytes, 10)
        child?.let { visitor.visitSingularMessageField(it, 11) }
        visitor.visitPackedFixed32Field(fixed32s, 12)
        visitor.visitUnknown(unknownFields)
    }

    override fun equals(other: Any?): Boolean =
        other is ScalarsMessage && sint64 == other.sint64 && fixed64 == other.fixed64 && sfixed32 == other.sfixed32 &&
            sfixed64 == other.sfixed64 && int32s == other.int32s && sint64s == other.sint64s &&
            doubles == other.doubles && bools == other.bools && strings == other.strings && bytes == other.bytes &&
            child == other.child && fixed32s == other.fixed32s && unknownFields.contentEquals(other.unknownFields)

    override fun hashCode(): Int = sint64.hashCode()

    override fun copy(): ScalarsMessage = ScalarsMessage(serializedData())
}

@RunWith(RobolectricTestRunner::class)
class SwiftProtobufShimSuite {
    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    private fun unhex(text: String): ByteArray = ByteArray(text.length / 2) { text.substring(2 * it, 2 * it + 2).toInt(16).toByte() }

    private fun roundTrip(expected: String, block: ScalarsMessage.() -> Unit) {
        val message = ScalarsMessage().apply(block)
        assertEquals(expected, hex(message.serializedData()))
        assertEquals(message, ScalarsMessage(unhex(expected)))
    }

    @Test
    fun sint64EdgeValues() {
        roundTrip("") { sint64 = 0 }
        roundTrip("0801") { sint64 = -1 }
        roundTrip("0802") { sint64 = 1 }
        roundTrip("08feffffffffffffffff01") { sint64 = Long.MAX_VALUE }
        roundTrip("08ffffffffffffffffff01") { sint64 = Long.MIN_VALUE }
    }

    @Test
    fun fixedWidthEdgeValues() {
        roundTrip("110100000000000000") { fixed64 = 1uL }
        roundTrip("11ffffffffffffffff") { fixed64 = ULong.MAX_VALUE }
        roundTrip("1dffffffff") { sfixed32 = -1 }
        roundTrip("1d00000080") { sfixed32 = Int.MIN_VALUE }
        roundTrip("1dffffff7f") { sfixed32 = Int.MAX_VALUE }
        roundTrip("21ffffffffffffffff") { sfixed64 = -1 }
        roundTrip("210000000000000080") { sfixed64 = Long.MIN_VALUE }
        roundTrip("21ffffffffffffff7f") { sfixed64 = Long.MAX_VALUE }
    }

    @Test
    fun packedAndRepeatedScalars() {
        roundTrip("2a0b01ffffffffffffffffff01") { int32s.addAll(listOf(1, -1)) }
        roundTrip("32020102") { sint64s.addAll(listOf(-1L, 1L)) }
        roundTrip("3a08000000000000f03f") { doubles.add(1.0) }
        roundTrip("42020100") { bools.addAll(listOf(true, false)) }
        roundTrip("4a01614a00") { strings.addAll(listOf("a", "")) }
        roundTrip("520101") { bytes.add(byteArrayOf(1)) }
        roundTrip("62080100000002000000") { fixed32s.addAll(listOf(1u, 2u)) }
        val unpacked = ScalarsMessage(unhex("2801" + "2a0102" + "6501000000" + "310100000000000000" + "4001"))
        assertEquals(listOf(1, 2), unpacked.int32s)
        assertEquals(listOf(1u), unpacked.fixed32s)
        assertEquals(listOf(true), unpacked.bools)
        assertEquals(listOf<Long>(), unpacked.sint64s)
        assertEquals("310100000000000000", hex(unpacked.unknownFields))
        assertFailsWith<BinaryDecodingError> { ScalarsMessage(unhex("6203010203")) }
        assertFailsWith<BinaryDecodingError> { ScalarsMessage(unhex("3a0400000000")) }
        assertFailsWith<BinaryDecodingError> { ScalarsMessage(unhex("2a0280")) }
    }

    @Test
    fun repeatedFloatsCompareLikeSwift() {
        val positive = ScalarsMessage().apply { doubles.add(0.0) }
        val negative = ScalarsMessage().apply { doubles.add(-0.0) }
        assertEquals(positive.doubles, negative.doubles)
        assertEquals(positive.doubles.hashCode(), negative.doubles.hashCode())
        val nan = ScalarsMessage().apply { doubles.add(Double.NaN) }
        assertNotEquals<List<Double>>(nan.doubles, ScalarsMessage().apply { doubles.add(Double.NaN) }.doubles)
    }

    @Test
    fun nestingIsLimited() {
        fun nested(depth: Int): ScalarsMessage {
            val root = ScalarsMessage()
            var current = root
            repeat(depth) {
                val next = ScalarsMessage()
                current.child = next
                current = next
            }
            current.sint64 = -1
            return root
        }
        val shallow = nested(50)
        assertEquals(shallow, ScalarsMessage(shallow.serializedData()))
        assertFailsWith<BinaryDecodingError> { ScalarsMessage(nested(150).serializedData()) }
    }

    @Test
    fun timestampBehavesLikeAGeneratedMessage() {
        val timestamp = Google_Protobuf_Timestamp.with {
            seconds = -1
            nanos = 999_999_999
        }
        assertEquals("08ffffffffffffffffff0110ff93ebdc03", hex(timestamp.serializedData()))
        assertEquals(timestamp, Google_Protobuf_Timestamp(serializedBytes = timestamp.serializedData()))
        assertEquals(timestamp, timestamp.copy())
        assertEquals(Google_Protobuf_Timestamp(seconds = -1, nanos = 999_999_999), timestamp)
        assertEquals("google.protobuf.Timestamp", Google_Protobuf_Timestamp.protoMessageName)
    }
}
