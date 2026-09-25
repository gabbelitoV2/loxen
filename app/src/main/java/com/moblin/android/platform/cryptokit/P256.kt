package com.moblin.android.platform.cryptokit

import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class SharedSecret internal constructor(bytes: ByteArray) : ContiguousBytes {
    private val bytes = bytes.copyOf()

    fun contentEquals(other: ByteArray): Boolean = MessageDigest.isEqual(bytes, other)

    override fun <R> withUnsafeBytes(body: (ByteArray) -> R): R = body(bytes.copyOf())

    override fun equals(other: Any?): Boolean = other is SharedSecret && MessageDigest.isEqual(bytes, other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "SharedSecret: ${hexString(bytes)}"
}

object P256 {
    object KeyAgreement {
        class PublicKey {
            internal val point: P256Point

            internal constructor(point: P256Point) {
                this.point = point
            }

            constructor(x963Representation: ByteArray) {
                point = P256Curve.point(x963Representation)
            }

            @Suppress("UNUSED_PARAMETER")
            constructor(rawRepresentation: ByteArray, raw: Unit = Unit) {
                point = P256Curve.rawPoint(rawRepresentation)
            }

            @Suppress("UNUSED_PARAMETER")
            constructor(derRepresentation: ByteArray, der: Unit = Unit, representation: Unit = Unit) {
                point = P256Curve.point(subjectPublicKeyInfoKeyBytes(derRepresentation))
            }

            constructor(pemRepresentation: String) {
                val pem = pemDecode(pemRepresentation)
                if (pem.type != "PUBLIC KEY") {
                    throw CryptoKitASN1Error.invalidPEMDocument
                }
                point = P256Curve.point(subjectPublicKeyInfoKeyBytes(pem.derBytes))
            }

            val rawRepresentation: ByteArray
                get() = point.rawRepresentation()

            val x963Representation: ByteArray
                get() = point.x963Representation()

            val derRepresentation: ByteArray
                get() = p256PublicKeyDer(point.x963Representation())

            val pemRepresentation: String
                get() = pemEncode("PUBLIC KEY", derRepresentation)

            companion object
        }

        class PrivateKey {
            private val scalar: BigInteger
            val publicKey: PublicKey

            constructor(compactRepresentable: Boolean = true) {
                val keyPair = try {
                    val generator = KeyPairGenerator.getInstance("EC")
                    generator.initialize(ECGenParameterSpec("secp256r1"))
                    generator.generateKeyPair()
                } catch (_: GeneralSecurityException) {
                    throw coreCryptoError()
                }
                var scalar = (keyPair.private as ECPrivateKey).s
                val w = (keyPair.public as ECPublicKey).w
                var point = P256Point(w.affineX, w.affineY)
                if (compactRepresentable && !point.isCompactRepresentable) {
                    scalar = P256Curve.n - scalar
                    point = point.negated()
                }
                this.scalar = scalar
                publicKey = PublicKey(point)
            }

            constructor(rawRepresentation: ByteArray) {
                scalar = P256Curve.scalar(rawRepresentation)
                publicKey = PublicKey(P256Curve.multiplyGenerator(scalar))
            }

            @Suppress("UNUSED_PARAMETER")
            constructor(derRepresentation: ByteArray, der: Unit = Unit) {
                scalar = try {
                    P256Curve.scalar(pkcs8PrivateKeyBytes(derRepresentation))
                } catch (_: Exception) {
                    P256Curve.scalar(sec1PrivateKeyBytes(derRepresentation))
                }
                publicKey = PublicKey(P256Curve.multiplyGenerator(scalar))
            }

            constructor(pemRepresentation: String) {
                val pem = pemDecode(pemRepresentation)
                val rawRepresentation = when (pem.type) {
                    "EC PRIVATE KEY" -> sec1PrivateKeyBytes(pem.derBytes)
                    "PRIVATE KEY" -> pkcs8PrivateKeyBytes(pem.derBytes)
                    else -> throw CryptoKitASN1Error.invalidPEMDocument
                }
                scalar = P256Curve.scalar(rawRepresentation)
                publicKey = PublicKey(P256Curve.multiplyGenerator(scalar))
            }

            val rawRepresentation: ByteArray
                get() = P256Curve.toBytes(scalar)

            val x963Representation: ByteArray
                get() = publicKey.x963Representation + rawRepresentation

            val derRepresentation: ByteArray
                get() = p256PrivateKeyDer(rawRepresentation, publicKey.x963Representation)

            val pemRepresentation: String
                get() = pemEncode("PRIVATE KEY", derRepresentation)

            fun sharedSecretFromKeyAgreement(with: PublicKey): SharedSecret {
                try {
                    val keyFactory = KeyFactory.getInstance("EC")
                    val agreement = javax.crypto.KeyAgreement.getInstance("ECDH")
                    agreement.init(keyFactory.generatePrivate(PKCS8EncodedKeySpec(derRepresentation)))
                    agreement.doPhase(keyFactory.generatePublic(X509EncodedKeySpec(with.derRepresentation)), true)
                    val secret = agreement.generateSecret()
                    return SharedSecret(P256Curve.toBytes(BigInteger(1, secret)))
                } catch (_: GeneralSecurityException) {
                    throw coreCryptoError()
                }
            }

            companion object
        }
    }
}
