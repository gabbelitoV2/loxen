package com.moblin.android.platform.cryptokit

import com.moblin.android.remotecontrol.RemoteControlEncryption
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.Test

class CryptoKitAesGcmSuite {
    private class Vector(
        val key: String,
        val nonce: String,
        val plaintext: String,
        val aad: String,
        val ciphertext: String,
        val tag: String,
    )

    private val plaintext64 = "d9313225f88406e5a55909c5aff5269a86a7a9531534f7da2e4c303d8a318a72" +
        "1c3c0c95956809532fcf0e2449a6b525b16aedf5aa0de657ba637b391aafd255"
    private val plaintext60 = plaintext64.substring(0, 120)
    private val aad = "feedfacedeadbeeffeedfacedeadbeefabaddad2"

    private val vectors = listOf(
        Vector("00000000000000000000000000000000", "000000000000000000000000", "", "", "", "58e2fccefa7e3061367f1d57a4e7455a"),
        Vector(
            "00000000000000000000000000000000",
            "000000000000000000000000",
            "00000000000000000000000000000000",
            "",
            "0388dace60b6a392f328c2b971b2fe78",
            "ab6e47d42cec13bdf53a67b21257bddf",
        ),
        Vector(
            "feffe9928665731c6d6a8f9467308308",
            "cafebabefacedbaddecaf888",
            plaintext64,
            "",
            "42831ec2217774244b7221b784d0d49ce3aa212f2c02a4e035c17e2329aca12e" +
                "21d514b25466931c7d8f6a5aac84aa051ba30b396a0aac973d58e091473f5985",
            "4d5c2af327cd64a62cf35abd2ba6fab4",
        ),
        Vector(
            "feffe9928665731c6d6a8f9467308308",
            "cafebabefacedbaddecaf888",
            plaintext60,
            aad,
            "42831ec2217774244b7221b784d0d49ce3aa212f2c02a4e035c17e2329aca12e" +
                "21d514b25466931c7d8f6a5aac84aa051ba30b396a0aac973d58e091",
            "5bc94fbc3221a5db94fae95ae7121a47",
        ),
        Vector(
            "0000000000000000000000000000000000000000000000000000000000000000",
            "000000000000000000000000",
            "",
            "",
            "",
            "530f8afbc74536b9a963b4f1c4cb738b",
        ),
        Vector(
            "0000000000000000000000000000000000000000000000000000000000000000",
            "000000000000000000000000",
            "00000000000000000000000000000000",
            "",
            "cea7403d4d606b6e074ec5d3baf39d18",
            "d0d1c8a799996bf0265b98b5d48ab919",
        ),
        Vector(
            "feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308",
            "cafebabefacedbaddecaf888",
            plaintext60,
            aad,
            "522dc1f099567d07f47f37a32a84427d643a8cdcbfe5c0c97598a2bd2555d1aa" +
                "8cb08e48590dbb3da7b08b1056828838c5f61e6393ba7a0abcc9f662",
            "76fc6ece0f4e1768cddf8853bb2d551b",
        ),
    )

    @Test
    fun sealMatchesTheGcmSpecificationVectors() {
        for (vector in vectors) {
            val sealedBox = AES.GCM.seal(
                hex(vector.plaintext),
                using = SymmetricKey(data = hex(vector.key)),
                nonce = AES.GCM.Nonce(data = hex(vector.nonce)),
                authenticating = hex(vector.aad),
            )
            assertEquals(vector.ciphertext, hex(sealedBox.ciphertext))
            assertEquals(vector.tag, hex(sealedBox.tag))
            assertEquals(vector.nonce, hex(sealedBox.nonce.toByteArray()))
            assertEquals(vector.nonce + vector.ciphertext + vector.tag, hex(sealedBox.combined!!))
        }
    }

    @Test
    fun openAuthenticatesTheGcmSpecificationVectors() {
        for (vector in vectors) {
            val sealedBox = AES.GCM.SealedBox(
                nonce = AES.GCM.Nonce(data = hex(vector.nonce)),
                ciphertext = hex(vector.ciphertext),
                tag = hex(vector.tag),
            )
            val key = SymmetricKey(data = hex(vector.key))
            assertEquals(vector.plaintext, hex(AES.GCM.open(sealedBox, using = key, authenticating = hex(vector.aad))))
            val tampered = hex(vector.tag)
            tampered[0] = (tampered[0].toInt() xor 1).toByte()
            val tamperedBox = AES.GCM.SealedBox(
                nonce = AES.GCM.Nonce(data = hex(vector.nonce)),
                ciphertext = hex(vector.ciphertext),
                tag = tampered,
            )
            assertSame(
                CryptoKitError.authenticationFailure,
                assertFailsWith<CryptoKitError> { AES.GCM.open(tamperedBox, using = key, authenticating = hex(vector.aad)) },
            )
            assertSame(
                CryptoKitError.authenticationFailure,
                assertFailsWith<CryptoKitError> {
                    AES.GCM.open(sealedBox, using = key, authenticating = "other".encodeToByteArray())
                },
            )
        }
    }

    @Test
    fun longNoncesWorkButHaveNoCombinedRepresentation() {
        val sealedBox = AES.GCM.seal(
            hex(plaintext60),
            using = SymmetricKey(data = hex("feffe9928665731c6d6a8f9467308308")),
            nonce = AES.GCM.Nonce(
                data = hex(
                    "9313225df88406e555909c5aff5269aa6a7a9538534f7da1e4c303d2a318a728" +
                        "c3c0c95156809539fcf0e2429a6b525416aedbf5a0de6a57a637b39b",
                ),
            ),
            authenticating = hex(aad),
        )
        assertEquals(
            "8ce24998625615b603a033aca13fb894be9112a5c3a211a8ba262a3cca7e2ca7" +
                "01e4a9a4fba43c90ccdcb281d48c7c6fd62875d2aca417034c34aee5",
            hex(sealedBox.ciphertext),
        )
        assertEquals("619cc5aefffe0bfa462af43c1699d050", hex(sealedBox.tag))
        assertNull(sealedBox.combined)
        assertEquals(
            plaintext60,
            hex(AES.GCM.open(sealedBox, using = SymmetricKey(data = hex("feffe9928665731c6d6a8f9467308308")), authenticating = hex(aad))),
        )
    }

    @Test
    fun combinedRoundTripsWithARandomNonce() {
        val key = SymmetricKey(size = SymmetricKeySize.bits256)
        val message = "Moblin remote control".encodeToByteArray()
        val first = AES.GCM.seal(message, using = key)
        val second = AES.GCM.seal(message, using = key)
        val combined = assertNotNull(first.combined)
        assertEquals(12 + message.size + 16, combined.size)
        assertContentEquals(first.nonce.toByteArray(), combined.copyOfRange(0, 12))
        assertContentEquals(first.ciphertext, combined.copyOfRange(12, 12 + message.size))
        assertContentEquals(first.tag, combined.copyOfRange(12 + message.size, combined.size))
        assertContentEquals(message, AES.GCM.open(AES.GCM.SealedBox(combined = combined), using = key))
        kotlin.test.assertNotEquals(hex(first.nonce.toByteArray()), hex(second.nonce.toByteArray()))
        assertEquals(12, AES.GCM.Nonce().toByteArray().size)
    }

    @Test
    fun invalidSizesThrowLikeCryptoKit() {
        assertSame(
            CryptoKitError.incorrectParameterSize,
            assertFailsWith<CryptoKitError> { AES.GCM.Nonce(data = ByteArray(11)) },
        )
        assertSame(
            CryptoKitError.incorrectParameterSize,
            assertFailsWith<CryptoKitError> {
                AES.GCM.SealedBox(nonce = AES.GCM.Nonce(), ciphertext = ByteArray(4), tag = ByteArray(15))
            },
        )
        assertSame(
            CryptoKitError.incorrectParameterSize,
            assertFailsWith<CryptoKitError> { AES.GCM.SealedBox(combined = ByteArray(27)) },
        )
        assertEquals(0, AES.GCM.SealedBox(combined = ByteArray(28)).ciphertext.size)
        assertSame(
            CryptoKitError.incorrectKeySize,
            assertFailsWith<CryptoKitError> { AES.GCM.seal(ByteArray(1), using = SymmetricKey(data = ByteArray(20))) },
        )
        assertSame(
            CryptoKitError.authenticationFailure,
            assertFailsWith<CryptoKitError> {
                AES.GCM.open(AES.GCM.SealedBox(combined = ByteArray(40)), using = SymmetricKey(data = ByteArray(16)))
            },
        )
        assertEquals("authenticationFailure", CryptoKitError.authenticationFailure.toString())
    }

    @Test
    fun remoteControlEncryptionIsCompatibleWithTheShim() {
        val password = "secret password"
        val key = SymmetricKey(data = SHA256.hash(data = password.encodeToByteArray()).toByteArray())
        val message = "{\"event\":\"state\"}".encodeToByteArray()
        val encryption = RemoteControlEncryption(password)
        val encrypted = assertNotNull(encryption.encrypt(message))
        assertContentEquals(message, AES.GCM.open(AES.GCM.SealedBox(combined = encrypted), using = key))
        val sealed = assertNotNull(AES.GCM.seal(message, using = key).combined)
        assertContentEquals(message, encryption.decrypt(sealed))
    }
}
