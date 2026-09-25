package com.moblin.android.platform.cryptokit

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val GCM_DEFAULT_NONCE_BYTE_COUNT = 12
private const val GCM_TAG_BYTE_COUNT = 16

object AES {
    object GCM {
        class Nonce : ContiguousBytes, Iterable<UByte> {
            internal val bytes: ByteArray

            constructor() {
                bytes = ByteArray(GCM_DEFAULT_NONCE_BYTE_COUNT)
                SecureRandom().nextBytes(bytes)
            }

            constructor(data: ByteArray) {
                if (data.size < GCM_DEFAULT_NONCE_BYTE_COUNT) {
                    throw CryptoKitError.incorrectParameterSize
                }
                bytes = data.copyOf()
            }

            fun toByteArray(): ByteArray = bytes.copyOf()

            override fun <R> withUnsafeBytes(body: (ByteArray) -> R): R = body(bytes.copyOf())

            override fun iterator(): Iterator<UByte> = bytes.map { it.toUByte() }.iterator()
        }

        class SealedBox {
            private val bytes: ByteArray
            private val nonceByteCount: Int

            constructor(nonce: Nonce, ciphertext: ByteArray, tag: ByteArray) {
                if (tag.size != GCM_TAG_BYTE_COUNT) {
                    throw CryptoKitError.incorrectParameterSize
                }
                bytes = nonce.bytes + ciphertext + tag
                nonceByteCount = nonce.bytes.size
            }

            constructor(combined: ByteArray) {
                if (combined.size < GCM_DEFAULT_NONCE_BYTE_COUNT + GCM_TAG_BYTE_COUNT) {
                    throw CryptoKitError.incorrectParameterSize
                }
                bytes = combined.copyOf()
                nonceByteCount = GCM_DEFAULT_NONCE_BYTE_COUNT
            }

            internal constructor(combined: ByteArray, nonceByteCount: Int) {
                bytes = combined
                this.nonceByteCount = nonceByteCount
            }

            val nonce: Nonce
                get() = Nonce(bytes.copyOfRange(0, nonceByteCount))

            val ciphertext: ByteArray
                get() = bytes.copyOfRange(nonceByteCount, bytes.size - GCM_TAG_BYTE_COUNT)

            val tag: ByteArray
                get() = bytes.copyOfRange(bytes.size - GCM_TAG_BYTE_COUNT, bytes.size)

            val combined: ByteArray?
                get() = if (nonceByteCount == GCM_DEFAULT_NONCE_BYTE_COUNT) bytes.copyOf() else null

            internal val nonceBytes: ByteArray
                get() = bytes.copyOfRange(0, nonceByteCount)

            internal val ciphertextAndTag: ByteArray
                get() = bytes.copyOfRange(nonceByteCount, bytes.size)
        }

        fun seal(
            message: ByteArray,
            using: SymmetricKey,
            nonce: Nonce? = null,
            authenticating: ByteArray = ByteArray(0),
        ): SealedBox {
            val nonceBytes = (nonce ?: Nonce()).bytes
            val cipher = gcmCipher(Cipher.ENCRYPT_MODE, using, nonceBytes, authenticating)
            val ciphertextAndTag = try {
                cipher.doFinal(message)
            } catch (_: GeneralSecurityException) {
                throw coreCryptoError()
            }
            return SealedBox(nonceBytes + ciphertextAndTag, nonceBytes.size)
        }

        fun open(sealedBox: SealedBox, using: SymmetricKey, authenticating: ByteArray = ByteArray(0)): ByteArray {
            val cipher = gcmCipher(Cipher.DECRYPT_MODE, using, sealedBox.nonceBytes, authenticating)
            return try {
                cipher.doFinal(sealedBox.ciphertextAndTag)
            } catch (_: AEADBadTagException) {
                throw CryptoKitError.authenticationFailure
            } catch (_: GeneralSecurityException) {
                throw coreCryptoError()
            }
        }

        private fun gcmCipher(mode: Int, key: SymmetricKey, nonce: ByteArray, authenticating: ByteArray): Cipher {
            if (key.bytes.size != 16 && key.bytes.size != 24 && key.bytes.size != 32) {
                throw CryptoKitError.incorrectKeySize
            }
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(mode, SecretKeySpec(key.bytes, "AES"), GCMParameterSpec(GCM_TAG_BYTE_COUNT * 8, nonce))
                if (authenticating.isNotEmpty()) {
                    cipher.updateAAD(authenticating)
                }
                return cipher
            } catch (_: GeneralSecurityException) {
                throw coreCryptoError()
            }
        }
    }
}
