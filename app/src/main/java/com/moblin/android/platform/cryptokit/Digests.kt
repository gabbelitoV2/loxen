package com.moblin.android.platform.cryptokit

import java.security.MessageDigest

interface ContiguousBytes {
    fun <R> withUnsafeBytes(body: (ByteArray) -> R): R
}

sealed class Digest(bytes: ByteArray, name: String) : ContiguousBytes, Iterable<UByte> {
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
        return other is Digest && other.javaClass == javaClass && MessageDigest.isEqual(bytes, other.bytes)
    }

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "$name digest: ${hexString(bytes)}"
}

sealed class HashFunction<out D : Digest>(algorithm: String) {
    private val messageDigest = MessageDigest.getInstance(algorithm)

    fun update(data: ByteArray) {
        messageDigest.update(data)
    }

    fun finalize(): D = makeDigest((messageDigest.clone() as MessageDigest).digest())

    protected abstract fun makeDigest(bytes: ByteArray): D
}

sealed class HashFunctionType<H : HashFunction<D>, D : Digest>(name: String, byteCount: Int, blockByteCount: Int) {
    internal val name = name
    val byteCount = byteCount
    val blockByteCount = blockByteCount

    internal abstract fun create(): H

    fun hash(data: ByteArray): D {
        val function = create()
        function.update(data)
        return function.finalize()
    }
}

class SHA256 : HashFunction<SHA256Digest>("SHA-256") {
    override fun makeDigest(bytes: ByteArray) = SHA256Digest(bytes)

    companion object : HashFunctionType<SHA256, SHA256Digest>("SHA256", 32, 64) {
        override fun create() = SHA256()
    }
}

class SHA256Digest internal constructor(bytes: ByteArray) : Digest(bytes, "SHA256")

class SHA384 : HashFunction<SHA384Digest>("SHA-384") {
    override fun makeDigest(bytes: ByteArray) = SHA384Digest(bytes)

    companion object : HashFunctionType<SHA384, SHA384Digest>("SHA384", 48, 128) {
        override fun create() = SHA384()
    }
}

class SHA384Digest internal constructor(bytes: ByteArray) : Digest(bytes, "SHA384")

class SHA512 : HashFunction<SHA512Digest>("SHA-512") {
    override fun makeDigest(bytes: ByteArray) = SHA512Digest(bytes)

    companion object : HashFunctionType<SHA512, SHA512Digest>("SHA512", 64, 128) {
        override fun create() = SHA512()
    }
}

class SHA512Digest internal constructor(bytes: ByteArray) : Digest(bytes, "SHA512")

object Insecure {
    class SHA1 : HashFunction<SHA1Digest>("SHA-1") {
        override fun makeDigest(bytes: ByteArray) = SHA1Digest(bytes)

        companion object : HashFunctionType<SHA1, SHA1Digest>("SHA1", 20, 64) {
            override fun create() = SHA1()
        }
    }

    class SHA1Digest internal constructor(bytes: ByteArray) : Digest(bytes, "SHA1")

    class MD5 : HashFunction<MD5Digest>("MD5") {
        override fun makeDigest(bytes: ByteArray) = MD5Digest(bytes)

        companion object : HashFunctionType<MD5, MD5Digest>("MD5", 16, 64) {
            override fun create() = MD5()
        }
    }

    class MD5Digest internal constructor(bytes: ByteArray) : Digest(bytes, "MD5")
}
