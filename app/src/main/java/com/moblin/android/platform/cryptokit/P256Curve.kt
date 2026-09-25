package com.moblin.android.platform.cryptokit

import java.math.BigInteger

internal class P256Point(val x: BigInteger, val y: BigInteger) {
    val isCompactRepresentable: Boolean
        get() = y < P256Curve.p - y

    fun negated(): P256Point = P256Point(x, P256Curve.p - y)

    fun rawRepresentation(): ByteArray = P256Curve.toBytes(x) + P256Curve.toBytes(y)

    fun x963Representation(): ByteArray = byteArrayOf(0x04) + rawRepresentation()

    override fun equals(other: Any?): Boolean = other is P256Point && other.x == x && other.y == y

    override fun hashCode(): Int = x.hashCode() * 31 + y.hashCode()
}

internal object P256Curve {
    const val COORDINATE_BYTE_COUNT = 32
    val p = BigInteger("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16)
    private val a = p - BigInteger.valueOf(3)
    private val b = BigInteger("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16)
    val n = BigInteger("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16)
    private val generator = P256Point(
        BigInteger("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296", 16),
        BigInteger("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5", 16),
    )

    fun toBytes(value: BigInteger): ByteArray {
        val bytes = value.toByteArray()
        return when {
            bytes.size == COORDINATE_BYTE_COUNT -> bytes
            bytes.size > COORDINATE_BYTE_COUNT -> bytes.copyOfRange(bytes.size - COORDINATE_BYTE_COUNT, bytes.size)
            else -> ByteArray(COORDINATE_BYTE_COUNT - bytes.size) + bytes
        }
    }

    fun scalar(rawRepresentation: ByteArray): BigInteger {
        if (rawRepresentation.size != COORDINATE_BYTE_COUNT) {
            throw CryptoKitError.incorrectKeySize
        }
        val scalar = BigInteger(1, rawRepresentation)
        if (scalar.signum() == 0 || scalar >= n) {
            throw coreCryptoError()
        }
        return scalar
    }

    fun point(x963Representation: ByteArray): P256Point {
        when (x963Representation.size) {
            2 * COORDINATE_BYTE_COUNT + 1 -> {
                if (x963Representation[0].toInt() != 0x04) {
                    throw coreCryptoError()
                }
                return rawPoint(x963Representation.copyOfRange(1, x963Representation.size))
            }
            COORDINATE_BYTE_COUNT + 1 -> {
                val prefix = x963Representation[0].toInt()
                if (prefix != 0x02 && prefix != 0x03) {
                    throw coreCryptoError()
                }
                val x = BigInteger(1, x963Representation.copyOfRange(1, x963Representation.size))
                return decompress(x, odd = prefix == 0x03) ?: throw coreCryptoError()
            }
            else -> throw CryptoKitError.incorrectKeySize
        }
    }

    fun rawPoint(rawRepresentation: ByteArray): P256Point {
        if (rawRepresentation.size != 2 * COORDINATE_BYTE_COUNT) {
            throw CryptoKitError.incorrectKeySize
        }
        val x = BigInteger(1, rawRepresentation.copyOfRange(0, COORDINATE_BYTE_COUNT))
        val y = BigInteger(1, rawRepresentation.copyOfRange(COORDINATE_BYTE_COUNT, rawRepresentation.size))
        if (!isOnCurve(x, y)) {
            throw coreCryptoError()
        }
        return P256Point(x, y)
    }

    fun multiplyGenerator(scalar: BigInteger): P256Point {
        var r0: P256Point? = null
        var r1: P256Point? = generator
        for (bit in 255 downTo 0) {
            if (scalar.testBit(bit)) {
                r0 = add(r0, r1)
                r1 = double(r1)
            } else {
                r1 = add(r0, r1)
                r0 = double(r0)
            }
        }
        return r0 ?: throw coreCryptoError()
    }

    private fun isOnCurve(x: BigInteger, y: BigInteger): Boolean {
        if (x.signum() < 0 || x >= p || y.signum() < 0 || y >= p) {
            return false
        }
        return y.multiply(y).subtract(rightHandSide(x)).mod(p).signum() == 0
    }

    private fun rightHandSide(x: BigInteger): BigInteger = x.multiply(x).multiply(x).add(a.multiply(x)).add(b).mod(p)

    private fun decompress(x: BigInteger, odd: Boolean): P256Point? {
        if (x >= p) {
            return null
        }
        val rightHandSide = rightHandSide(x)
        var y = rightHandSide.modPow(p.add(BigInteger.ONE).shiftRight(2), p)
        if (y.multiply(y).mod(p) != rightHandSide) {
            return null
        }
        if (y.testBit(0) != odd) {
            y = p.subtract(y).mod(p)
        }
        return P256Point(x, y)
    }

    private fun add(first: P256Point?, second: P256Point?): P256Point? {
        if (first == null) {
            return second
        }
        if (second == null) {
            return first
        }
        if (first.x == second.x) {
            return if (first.y == second.y) double(first) else null
        }
        val slope = second.y.subtract(first.y).multiply(second.x.subtract(first.x).modInverse(p)).mod(p)
        val x = slope.multiply(slope).subtract(first.x).subtract(second.x).mod(p)
        val y = slope.multiply(first.x.subtract(x)).subtract(first.y).mod(p)
        return P256Point(x, y)
    }

    private fun double(point: P256Point?): P256Point? {
        if (point == null || point.y.signum() == 0) {
            return null
        }
        val numerator = point.x.multiply(point.x).multiply(BigInteger.valueOf(3)).add(a)
        val slope = numerator.multiply(point.y.shiftLeft(1).modInverse(p)).mod(p)
        val x = slope.multiply(slope).subtract(point.x.shiftLeft(1)).mod(p)
        val y = slope.multiply(point.x.subtract(x)).subtract(point.y).mod(p)
        return P256Point(x, y)
    }
}
