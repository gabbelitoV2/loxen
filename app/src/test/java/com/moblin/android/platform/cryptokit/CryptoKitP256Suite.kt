package com.moblin.android.platform.cryptokit

import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Test

private val teslaDerHeader = hex("3059301306072a8648ce3d020106082a8648ce3d030107034200")

private operator fun P256.KeyAgreement.PublicKey.Companion.invoke(bytes: ByteArray): P256.KeyAgreement.PublicKey {
    return P256.KeyAgreement.PublicKey(derRepresentation = teslaDerHeader + bytes)
}

private fun P256.KeyAgreement.PublicKey.toBytes(): ByteArray = derRepresentation.copyOfRange(26, derRepresentation.size)

class CryptoKitP256Suite {
    private val initiatorPrivate = hex("c88f01f510d9ac3f70a292daa2316de544e9aab8afe84049c62a9c57862d1433")
    private val initiatorPublic = hex(
        "04dad0b65394221cf9b051e1feca5787d098dfe637fc90b9ef945d0c3772581180" +
            "5271a0461cdb8252d61f1c456fa3e59ab1f45b33accf5f58389e0577b8990bb3",
    )
    private val responderPrivate = hex("c6ef9c5d78ae012a011164acb397ce2088685d8f06bf9be0b283ab46476bee53")
    private val responderPublic = hex(
        "04d12dfb5289c8d4f81208b70270398c342296970a0bccb74c736fc7554494bf63" +
            "56fbf3ca366cc23e8157854c13c58d6aac23f046ada30f8353e74f33039872ab",
    )
    private val sharedX = "d6840f6b42f6edafd13116e0e12565202fef8e9ece7dce03812464d04b9442de"
    private val initiatorPem = """
        -----BEGIN PRIVATE KEY-----
        MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgyI8B9RDZrD9wopLa
        ojFt5UTpqriv6EBJxiqcV4YtFDOhRANCAATa0LZTlCIc+bBR4f7KV4fQmN/mN/yQ
        ue+UXQw3clgRgFJxoEYc24JS1h8cRW+j5Zqx9FszrM9fWDieBXe4mQuz
        -----END PRIVATE KEY-----
    """.trimIndent()
    private val initiatorSec1Pem = """
        -----BEGIN EC PRIVATE KEY-----
        MHcCAQEEIMiPAfUQ2aw/cKKS2qIxbeVE6aq4r+hAScYqnFeGLRQzoAoGCCqGSM49
        AwEHoUQDQgAE2tC2U5QiHPmwUeH+yleH0Jjf5jf8kLnvlF0MN3JYEYBScaBGHNuC
        UtYfHEVvo+WasfRbM6zPX1g4ngV3uJkLsw==
        -----END EC PRIVATE KEY-----
    """.trimIndent()
    private val initiatorPublicPem = """
        -----BEGIN PUBLIC KEY-----
        MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE2tC2U5QiHPmwUeH+yleH0Jjf5jf8
        kLnvlF0MN3JYEYBScaBGHNuCUtYfHEVvo+WasfRbM6zPX1g4ngV3uJkLsw==
        -----END PUBLIC KEY-----
    """.trimIndent()

    @Test
    fun publicKeysAndSharedSecretMatchRfc5903() {
        val initiator = P256.KeyAgreement.PrivateKey(rawRepresentation = initiatorPrivate)
        val responder = P256.KeyAgreement.PrivateKey(rawRepresentation = responderPrivate)
        assertContentEquals(initiatorPublic, initiator.publicKey.x963Representation)
        assertContentEquals(responderPublic, responder.publicKey.x963Representation)
        assertContentEquals(initiatorPublic.copyOfRange(1, 65), initiator.publicKey.rawRepresentation)
        assertContentEquals(initiatorPrivate, initiator.rawRepresentation)
        assertContentEquals(initiatorPublic + initiatorPrivate, initiator.x963Representation)
        val shared = initiator.sharedSecretFromKeyAgreement(with = P256.KeyAgreement.PublicKey(x963Representation = responderPublic))
        assertEquals(sharedX, hex(shared.withUnsafeBytes { buffer -> buffer.copyOf(buffer.size) }))
        assertEquals(shared, responder.sharedSecretFromKeyAgreement(with = initiator.publicKey))
        assertTrue(shared.contentEquals(hex(sharedX)))
        assertEquals("SharedSecret: $sharedX", shared.toString())
    }

    @Test
    fun sharedSecretMatchesWycheproofNormalCase() {
        val privateKey = P256.KeyAgreement.PrivateKey(
            rawRepresentation = hex("0612465c89a023ab17855b0a6bcebfd3febb53aef84138647b5352e02c10c346"),
        )
        val expected = "53020d908b0219328b658b525f26780e3ae12bcd952bb25a93bc0895e1714285"
        val der = P256.KeyAgreement.PublicKey(
            derRepresentation = hex(
                "3059301306072a8648ce3d020106082a8648ce3d030107034200" +
                    "0462d5bd3372af75fe85a040715d0f502428e07046868b0bfdfa61d731afe44f26" +
                    "ac333a93a9e70a81cd5a95b5bf8d13990eb741c8c38872b4a07d275a014e30cf",
            ),
        )
        assertTrue(privateKey.sharedSecretFromKeyAgreement(with = der).contentEquals(hex(expected)))
        val compressed = P256.KeyAgreement.PublicKey(
            x963Representation = hex("0362d5bd3372af75fe85a040715d0f502428e07046868b0bfdfa61d731afe44f26"),
        )
        assertContentEquals(der.x963Representation, compressed.x963Representation)
        assertTrue(privateKey.sharedSecretFromKeyAgreement(with = compressed).contentEquals(hex(expected)))
        val raw = P256.KeyAgreement.PublicKey(rawRepresentation = der.rawRepresentation)
        assertContentEquals(der.x963Representation, raw.x963Representation)
    }

    @Test
    fun invalidPublicKeysAreRejected() {
        val offCurve = responderPublic.copyOf()
        offCurve[64] = (offCurve[64].toInt() xor 1).toByte()
        assertFailsWith<CryptoKitError.underlyingCoreCryptoError> {
            P256.KeyAgreement.PublicKey(x963Representation = offCurve)
        }
        assertFailsWith<CryptoKitError.underlyingCoreCryptoError> {
            P256.KeyAgreement.PublicKey(derRepresentation = teslaDerHeader + offCurve)
        }
        val xTooLarge = hex("04ffffffff00000001000000000000000000000000ffffffffffffffffffffffff") + ByteArray(32)
        assertFailsWith<CryptoKitError.underlyingCoreCryptoError> {
            P256.KeyAgreement.PublicKey(x963Representation = xTooLarge)
        }
        val wrongPrefix = responderPublic.copyOf()
        wrongPrefix[0] = 0x05
        assertFailsWith<CryptoKitError.underlyingCoreCryptoError> {
            P256.KeyAgreement.PublicKey(x963Representation = wrongPrefix)
        }
        assertSame(
            CryptoKitError.incorrectKeySize,
            assertFailsWith<CryptoKitError> { P256.KeyAgreement.PublicKey(x963Representation = responderPublic.copyOf(64)) },
        )
        assertSame(
            CryptoKitError.incorrectKeySize,
            assertFailsWith<CryptoKitError> { P256.KeyAgreement.PublicKey(x963Representation = byteArrayOf(0)) },
        )
        assertSame(
            CryptoKitError.incorrectKeySize,
            assertFailsWith<CryptoKitError> { P256.KeyAgreement.PublicKey(rawRepresentation = ByteArray(63)) },
        )
        assertSame(
            CryptoKitASN1Error.truncatedASN1Field,
            assertFailsWith<CryptoKitASN1Error> {
                P256.KeyAgreement.PublicKey(derRepresentation = (teslaDerHeader + responderPublic).copyOf(90))
            },
        )
        assertSame(
            CryptoKitASN1Error.invalidASN1Object,
            assertFailsWith<CryptoKitASN1Error> {
                P256.KeyAgreement.PublicKey(derRepresentation = teslaDerHeader + responderPublic + byteArrayOf(0x05, 0x00))
            },
        )
    }

    @Test
    fun invalidPrivateKeysAreRejected() {
        assertSame(
            CryptoKitError.incorrectKeySize,
            assertFailsWith<CryptoKitError> { P256.KeyAgreement.PrivateKey(rawRepresentation = ByteArray(31)) },
        )
        assertFailsWith<CryptoKitError.underlyingCoreCryptoError> {
            P256.KeyAgreement.PrivateKey(rawRepresentation = ByteArray(32))
        }
        assertFailsWith<CryptoKitError.underlyingCoreCryptoError> {
            P256.KeyAgreement.PrivateKey(
                rawRepresentation = hex("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551"),
            )
        }
        val largest = P256.KeyAgreement.PrivateKey(
            rawRepresentation = hex("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632550"),
        )
        assertEquals(
            "046b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296" +
                "b01cbd1c01e58065711814b583f061e9d431cca994cea1313449bf97c840ae0a",
            hex(largest.publicKey.x963Representation),
        )
        val one = P256.KeyAgreement.PrivateKey(rawRepresentation = ByteArray(31) + byteArrayOf(1))
        assertEquals(
            "046b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296" +
                "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5",
            hex(one.publicKey.x963Representation),
        )
        assertContentEquals(ByteArray(31) + byteArrayOf(1), one.rawRepresentation)
    }

    @Test
    fun pemRepresentationIsByteCompatibleWithApple() {
        val initiator = P256.KeyAgreement.PrivateKey(rawRepresentation = initiatorPrivate)
        assertEquals(initiatorPem, initiator.pemRepresentation)
        assertFalse(initiator.pemRepresentation.endsWith("\n"))
        assertTrue(initiator.pemRepresentation.lines()[1].startsWith("MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg"))
        assertEquals(138, initiator.derRepresentation.size)
        assertContentEquals(Base64.getDecoder().decode(initiatorPem.lines().drop(1).dropLast(1).joinToString("")), initiator.derRepresentation)
        assertEquals(initiatorPublicPem, initiator.publicKey.pemRepresentation)
        assertEquals(91, initiator.publicKey.derRepresentation.size)
        assertContentEquals(teslaDerHeader + initiatorPublic, initiator.publicKey.derRepresentation)
    }

    @Test
    fun pemAndDerImportAcceptWhatCryptoKitAccepts() {
        for (pem in listOf(initiatorPem, initiatorSec1Pem, initiatorPem + "\n", initiatorPem.replace("\n", "\r\n"))) {
            val key = P256.KeyAgreement.PrivateKey(pemRepresentation = pem)
            assertContentEquals(initiatorPrivate, key.rawRepresentation)
            assertContentEquals(initiatorPublic, key.publicKey.x963Representation)
        }
        val fromDer = P256.KeyAgreement.PrivateKey(derRepresentation = P256.KeyAgreement.PrivateKey(pemRepresentation = initiatorPem).derRepresentation)
        assertContentEquals(initiatorPrivate, fromDer.rawRepresentation)
        val sec1Der = Base64.getDecoder().decode(initiatorSec1Pem.lines().drop(1).dropLast(1).joinToString(""))
        assertContentEquals(initiatorPrivate, P256.KeyAgreement.PrivateKey(derRepresentation = sec1Der).rawRepresentation)
        assertContentEquals(initiatorPublic, P256.KeyAgreement.PublicKey(pemRepresentation = initiatorPublicPem).x963Representation)
    }

    @Test
    fun malformedPemIsRejected() {
        val lines = initiatorPem.lines()
        val cases = listOf(
            initiatorPem.replace("-----END PRIVATE KEY-----", "-----END EC PRIVATE KEY-----"),
            listOf(lines[0], lines[1] + lines[2], lines[3], lines[4]).joinToString("\n"),
            listOf(lines[0], lines[1].substring(0, 60), lines[1].substring(60) + lines[2], lines[3], lines[4]).joinToString("\n"),
            listOf(lines[0], lines[4]).joinToString("\n"),
            initiatorPem.replace("MIGH", "MIG!"),
            initiatorPublicPem.replace("PUBLIC KEY", "CERTIFICATE"),
            "",
        )
        for (pem in cases) {
            assertSame(
                CryptoKitASN1Error.invalidPEMDocument,
                assertFailsWith<CryptoKitASN1Error> { P256.KeyAgreement.PrivateKey(pemRepresentation = pem) },
                pem,
            )
        }
        assertSame(
            CryptoKitASN1Error.invalidPEMDocument,
            assertFailsWith<CryptoKitASN1Error> { P256.KeyAgreement.PublicKey(pemRepresentation = initiatorPem) },
        )
        assertSame(
            CryptoKitASN1Error.invalidPEMDocument,
            assertFailsWith<CryptoKitASN1Error> { P256.KeyAgreement.PrivateKey(pemRepresentation = initiatorPublicPem) },
        )
    }

    @Test
    fun malformedDerIsRejected() {
        var deep = byteArrayOf(0x05, 0x00)
        repeat(12) {
            deep = byteArrayOf(0x30, deep.size.toByte()) + deep
        }
        assertSame(
            CryptoKitASN1Error.invalidASN1Object,
            assertFailsWith<CryptoKitASN1Error> { P256.KeyAgreement.PrivateKey(derRepresentation = deep) },
        )
        val spki = teslaDerHeader + responderPublic
        assertSame(
            CryptoKitASN1Error.unsupportedFieldLength,
            assertFailsWith<CryptoKitASN1Error> {
                P256.KeyAgreement.PublicKey(derRepresentation = byteArrayOf(0x30, 0x81.toByte()) + spki.copyOfRange(1, spki.size))
            },
        )
        assertSame(
            CryptoKitASN1Error.unsupportedFieldLength,
            assertFailsWith<CryptoKitASN1Error> {
                P256.KeyAgreement.PublicKey(derRepresentation = byteArrayOf(0x30, 0x80.toByte()) + spki.copyOfRange(2, spki.size))
            },
        )
        val der = P256.KeyAgreement.PrivateKey(rawRepresentation = initiatorPrivate).derRepresentation
        val versionOne = der.copyOf()
        versionOne[5] = 1
        assertSame(
            CryptoKitASN1Error.invalidASN1Object,
            assertFailsWith<CryptoKitASN1Error> { pkcs8PrivateKeyBytes(versionOne) },
        )
        assertSame(
            CryptoKitASN1Error.unexpectedFieldType,
            assertFailsWith<CryptoKitASN1Error> { P256.KeyAgreement.PrivateKey(derRepresentation = versionOne) },
        )
        val sec1Content = hex("0201010420") + initiatorPrivate + hex("a00706052b81040022a144034200") + initiatorPublic
        val sec1WithSecp384r1 = byteArrayOf(0x30, sec1Content.size.toByte()) + sec1Content
        val pkcs8Content = hex("020100301306072a8648ce3d020106082a8648ce3d030107") +
            byteArrayOf(0x04, sec1WithSecp384r1.size.toByte()) + sec1WithSecp384r1
        val mismatchedCurve = byteArrayOf(0x30, 0x81.toByte(), pkcs8Content.size.toByte()) + pkcs8Content
        assertSame(
            CryptoKitASN1Error.invalidASN1Object,
            assertFailsWith<CryptoKitASN1Error> { pkcs8PrivateKeyBytes(mismatchedCurve) },
        )
        assertContentEquals(initiatorPrivate, sec1PrivateKeyBytes(sec1WithSecp384r1))
    }

    @Test
    fun generatedKeysRoundTripAndAreCompactRepresentable() {
        repeat(20) {
            val key = P256.KeyAgreement.PrivateKey()
            val y = BigInteger(1, key.publicKey.rawRepresentation.copyOfRange(32, 64))
            assertTrue(y < P256Curve.p - y)
            val imported = P256.KeyAgreement.PrivateKey(pemRepresentation = key.pemRepresentation)
            assertContentEquals(key.rawRepresentation, imported.rawRepresentation)
            assertContentEquals(key.publicKey.x963Representation, imported.publicKey.x963Representation)
            val other = P256.KeyAgreement.PrivateKey()
            assertEquals(
                key.sharedSecretFromKeyAgreement(with = other.publicKey),
                other.sharedSecretFromKeyAgreement(with = key.publicKey),
            )
        }
        val notCompact = P256.KeyAgreement.PrivateKey(compactRepresentable = false)
        assertContentEquals(
            notCompact.publicKey.x963Representation,
            P256.KeyAgreement.PrivateKey(rawRepresentation = notCompact.rawRepresentation).publicKey.x963Representation,
        )
    }

    @Test
    fun javaEncodedKeysImportWithTheProvidersPublicKey() {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        repeat(10) {
            val pair = generator.generateKeyPair()
            val body = Base64.getEncoder().encodeToString(pair.private.encoded).chunked(64).joinToString("\n")
            val pem = "-----BEGIN PRIVATE KEY-----\n$body\n-----END PRIVATE KEY-----\n"
            val key = P256.KeyAgreement.PrivateKey(pemRepresentation = pem)
            val w = (pair.public as ECPublicKey).w
            assertContentEquals(P256Curve.toBytes(w.affineX) + P256Curve.toBytes(w.affineY), key.publicKey.rawRepresentation)
            assertContentEquals(pair.public.encoded, key.publicKey.derRepresentation)
        }
    }

    @Test
    fun teslaVehicleKeyDerivationWorksOnTheShim() {
        val client = P256.KeyAgreement.PrivateKey(pemRepresentation = initiatorPem)
        val vehiclePublicKey = P256.KeyAgreement.PublicKey(bytes = responderPublic)
        assertContentEquals(responderPublic, vehiclePublicKey.toBytes())
        assertContentEquals(initiatorPublic, client.publicKey.toBytes())
        val shared = client.sharedSecretFromKeyAgreement(with = vehiclePublicKey)
        val sharedData = shared.withUnsafeBytes { buffer -> buffer.copyOf(buffer.size) }
        val sharedSecret = Insecure.SHA1.hash(data = sharedData).prefix(16)
        val key = SymmetricKey(data = sharedSecret)
        assertContentEquals(hex("f18d89be1f0206d14f29f942842be1c5"), key.withUnsafeBytes { it })
        val metadataHash = SHA256.hash(data = "metadata".encodeToByteArray()).toByteArray()
        val encrypted = AES.GCM.seal("payload".encodeToByteArray(), using = key, authenticating = metadataHash)
        val sealedBox = AES.GCM.SealedBox(
            nonce = AES.GCM.Nonce(data = encrypted.nonce.toByteArray()),
            ciphertext = encrypted.ciphertext,
            tag = encrypted.tag,
        )
        assertEquals("payload", AES.GCM.open(sealedBox, using = key, authenticating = metadataHash).decodeToString())
    }
}
