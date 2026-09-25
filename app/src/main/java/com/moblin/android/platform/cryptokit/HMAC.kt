package com.moblin.android.platform.cryptokit

import java.security.MessageDigest
import java.security.SecureRandom

class SymmetricKeySize(val bitCount: Int) {
    init {
        require(bitCount > 0 && bitCount % 8 == 0)
    }

    companion object {
        val bits128 = SymmetricKeySize(128)
        val bits192 = SymmetricKeySize(192)
        val bits256 = SymmetricKeySize(256)
    }
}

class SymmetricKey : ContiguousBytes {
    internal val bytes: ByteArray

    constructor(data: ByteArray) {
        bytes = data.copyOf()
    }

    constructor(data: ContiguousBytes) {
        bytes = data.withUnsafeBytes { it.copyOf() }
    }

    constructor(size: SymmetricKeySize) {
        bytes = ByteArray(size.bitCount / 8)
        SecureRandom().nextBytes(bytes)
    }

    val bitCount: Int
        get() = bytes.size * 8

    override fun <R> withUnsafeBytes(body: (ByteArray) -> R): R = body(bytes.copyOf())

    override fun equals(other: Any?): Boolean = other is SymmetricKey && MessageDigest.isEqual(bytes, other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()
}

class HashedAuthenticationCode<H : HashFunction<*>> internal constructor(bytes: ByteArray, name: String) :
    ContiguousBytes,
    Iterable<UByte> {
    private val bytes = bytes.copyOf()
    private val name = name

    val byteCount: Int
        get() = bytes.size

    fun toByteArray(): ByteArray = bytes.copyOf()

    fun prefix(maxLength: Int): ByteArray = bytes.copyOf(minOf(maxLength, bytes.size))

    fun contentEquals(other: ByteArray): Boolean = MessageDigest.isEqual(bytes, other)

    override fun <R> withUnsafeBytes(body: (ByteArray) -> R): R = body(bytes.copyOf())

    override fun iterator(): Iterator<UByte> = bytes.map { it.toUByte() }.iterator()

    override fun equals(other: Any?): Boolean {
        return other is HashedAuthenticationCode<*> && other.name == name && MessageDigest.isEqual(bytes, other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "HMAC with $name: ${hexString(bytes)}"
}

class HMAC<H : HashFunction<*>>(hashFunction: HashFunctionType<H, *>, key: SymmetricKey) {
    private val hashFunction = hashFunction
    private val inner: H = hashFunction.create()
    private val outerKeyPad: ByteArray

    init {
        var keyBytes = key.bytes
        if (keyBytes.size > hashFunction.blockByteCount) {
            keyBytes = hashFunction.hash(keyBytes).toByteArray()
        }
        val paddedKey = keyBytes.copyOf(hashFunction.blockByteCount)
        inner.update(ByteArray(paddedKey.size) { (paddedKey[it].toInt() xor 0x36).toByte() })
        outerKeyPad = ByteArray(paddedKey.size) { (paddedKey[it].toInt() xor 0x5C).toByte() }
    }

    fun update(data: ByteArray) {
        inner.update(data)
    }

    fun finalize(): HashedAuthenticationCode<H> {
        val outer = hashFunction.create()
        outer.update(outerKeyPad)
        outer.update(inner.finalize().toByteArray())
        return HashedAuthenticationCode(outer.finalize().toByteArray(), hashFunction.name)
    }

    companion object {
        fun <H : HashFunction<*>> authenticationCode(
            hashFunction: HashFunctionType<H, *>,
            `for`: ByteArray,
            using: SymmetricKey,
        ): HashedAuthenticationCode<H> {
            val hmac = HMAC(hashFunction, using)
            hmac.update(`for`)
            return hmac.finalize()
        }

        fun <H : HashFunction<*>> isValidAuthenticationCode(
            hashFunction: HashFunctionType<H, *>,
            authenticationCode: HashedAuthenticationCode<H>,
            authenticating: ByteArray,
            using: SymmetricKey,
        ): Boolean {
            return HMAC.authenticationCode(hashFunction, authenticating, using) == authenticationCode
        }

        fun <H : HashFunction<*>> isValidAuthenticationCode(
            hashFunction: HashFunctionType<H, *>,
            authenticationCode: ByteArray,
            authenticating: ByteArray,
            using: SymmetricKey,
        ): Boolean {
            return HMAC.authenticationCode(hashFunction, authenticating, using).contentEquals(authenticationCode)
        }
    }
}
