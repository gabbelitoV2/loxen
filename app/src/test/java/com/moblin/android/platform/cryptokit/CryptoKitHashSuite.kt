package com.moblin.android.platform.cryptokit

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.Test

internal fun hex(value: String): ByteArray {
    val clean = value.filter { !it.isWhitespace() }
    return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

internal fun hex(value: ByteArray): String = value.joinToString("") { "%02x".format(it.toInt() and 0xFF) }

class CryptoKitHashSuite {
    private val abc = "abc".encodeToByteArray()
    private val twoBlocks = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()

    @Test
    fun sha256MatchesTheNistVectors() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hex(SHA256.hash(data = abc).toByteArray()),
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hex(SHA256.hash(data = ByteArray(0)).toByteArray()),
        )
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hex(SHA256.hash(data = twoBlocks).toByteArray()),
        )
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            hex(SHA256.hash(data = ByteArray(1_000_000) { 'a'.code.toByte() }).toByteArray()),
        )
    }

    @Test
    fun sha384AndSha512MatchTheNistVectors() {
        assertEquals(
            "cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7",
            hex(SHA384.hash(data = abc).toByteArray()),
        )
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            hex(SHA512.hash(data = abc).toByteArray()),
        )
        assertEquals(48, SHA384.byteCount)
        assertEquals(128, SHA512.blockByteCount)
    }

    @Test
    fun insecureSha1AndMd5MatchTheirVectors() {
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hex(Insecure.SHA1.hash(data = abc).toByteArray()))
        assertEquals(
            "84983e441c3bd26ebaae4aa1f95129e5e54670f1",
            hex(Insecure.SHA1.hash(data = twoBlocks).toByteArray()),
        )
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hex(Insecure.MD5.hash(data = ByteArray(0)).toByteArray()))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", hex(Insecure.MD5.hash(data = abc).toByteArray()))
        assertEquals(
            "f96b697d7cb7938d525a2f31aaf161d0",
            hex(Insecure.MD5.hash(data = "message digest".encodeToByteArray()).toByteArray()),
        )
    }

    @Test
    fun incrementalHashingMatchesOneShotAndFinalizeDoesNotReset() {
        val md5 = Insecure.MD5()
        md5.update(data = "message ".encodeToByteArray())
        md5.update(data = "digest".encodeToByteArray())
        assertEquals("f96b697d7cb7938d525a2f31aaf161d0", hex(md5.finalize().toByteArray()))
        assertEquals("f96b697d7cb7938d525a2f31aaf161d0", hex(md5.finalize().toByteArray()))
        val sha256 = SHA256()
        sha256.update(data = "ab".encodeToByteArray())
        val partial = sha256.finalize()
        sha256.update(data = "c".encodeToByteArray())
        assertEquals(SHA256.hash(data = "ab".encodeToByteArray()), partial)
        assertEquals(SHA256.hash(data = abc), sha256.finalize())
    }

    @Test
    fun digestsCompareConvertAndDescribeLikeCryptoKit() {
        val digest = SHA256.hash(data = abc)
        assertEquals(SHA256.hash(data = abc), digest)
        assertEquals(SHA256.hash(data = abc).hashCode(), digest.hashCode())
        assertNotEquals(SHA256.hash(data = twoBlocks), digest)
        assertTrue(digest.contentEquals(hex("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")))
        assertFalse(digest.contentEquals(hex("ba7816bf")))
        assertEquals(32, digest.byteCount)
        assertEquals(
            "SHA256 digest: ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            digest.toString(),
        )
        assertEquals("SHA1 digest: a9993e364706816aba3e25717850c26c9cd0d89d", Insecure.SHA1.hash(data = abc).toString())
        assertEquals("MD5 digest: 900150983cd24fb0d6963f7d28e17f72", "${Insecure.MD5.hash(data = abc)}")
        assertContentEquals(hex("ba7816bf8f01cfea414140de5dae2223"), digest.prefix(16))
        assertContentEquals(digest.toByteArray(), digest.prefix(100))
        assertContentEquals(digest.toByteArray(), digest.withUnsafeBytes { it })
        assertEquals(listOf(0xBAu.toUByte(), 0x78u.toUByte()), digest.take(2))
        val copy = digest.toByteArray()
        copy[0] = 0
        assertTrue(digest.contentEquals(hex("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")))
    }

    @Test
    fun hmacMatchesRfc4231() {
        val key1 = SymmetricKey(data = ByteArray(20) { 0x0B })
        val hiThere = "Hi There".encodeToByteArray()
        assertEquals(
            "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            hex(HMAC.authenticationCode(SHA256, `for` = hiThere, using = key1).toByteArray()),
        )
        assertEquals(
            "87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cde" +
                "daa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854",
            hex(HMAC.authenticationCode(SHA512, `for` = hiThere, using = key1).toByteArray()),
        )
        assertEquals(
            "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
            hex(
                HMAC.authenticationCode(
                    SHA256,
                    `for` = "what do ya want for nothing?".encodeToByteArray(),
                    using = SymmetricKey(data = "Jefe".encodeToByteArray()),
                ).toByteArray(),
            ),
        )
        val longKey = SymmetricKey(data = ByteArray(131) { 0xAA.toByte() })
        val longKeyMessage = "Test Using Larger Than Block-Size Key - Hash Key First".encodeToByteArray()
        assertEquals(
            "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54",
            hex(HMAC.authenticationCode(SHA256, `for` = longKeyMessage, using = longKey).toByteArray()),
        )
        assertEquals(
            "80b24263c7c1a3ebb71493c1dd7be8b49b46d1f41b4aeec1121b013783f8f352" +
                "6b56d037e05f2598bd0fd2215d6a1e5295e64f73f63f0aec8b915a985d786598",
            hex(HMAC.authenticationCode(SHA512, `for` = longKeyMessage, using = longKey).toByteArray()),
        )
        assertEquals(
            "b613679a0814d9ec772f95d778c35fc5ff1697c493715653c6c712144292c5ad",
            hex(HMAC.authenticationCode(SHA256, `for` = ByteArray(0), using = SymmetricKey(data = ByteArray(0))).toByteArray()),
        )
    }

    @Test
    fun hmacValidatesIncrementsAndDescribes() {
        val key = SymmetricKey(data = ByteArray(20) { 0x0B })
        val message = "Hi There".encodeToByteArray()
        val mac = HMAC.authenticationCode(SHA256, `for` = message, using = key)
        assertTrue(HMAC.isValidAuthenticationCode(SHA256, mac, authenticating = message, using = key))
        assertTrue(HMAC.isValidAuthenticationCode(SHA256, mac.toByteArray(), authenticating = message, using = key))
        assertFalse(HMAC.isValidAuthenticationCode(SHA256, mac, authenticating = "Hi there".encodeToByteArray(), using = key))
        assertFalse(HMAC.isValidAuthenticationCode(SHA256, mac.prefix(16), authenticating = message, using = key))
        val tampered = mac.toByteArray()
        tampered[31] = (tampered[31].toInt() xor 1).toByte()
        assertFalse(HMAC.isValidAuthenticationCode(SHA256, tampered, authenticating = message, using = key))
        val hmac = HMAC(SHA256, key = key)
        hmac.update(data = "Hi ".encodeToByteArray())
        hmac.update(data = "There".encodeToByteArray())
        assertEquals(mac, hmac.finalize())
        assertEquals(mac, hmac.finalize())
        assertEquals(32, mac.byteCount)
        assertEquals(
            "HMAC with SHA256: b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            mac.toString(),
        )
        val sha1Mac: HashedAuthenticationCode<Insecure.SHA1> = HMAC.authenticationCode(Insecure.SHA1, `for` = message, using = key)
        assertEquals("b617318655057264e28bc0b6fb378c8ef146be00", hex(sha1Mac.toByteArray()))
    }

    @Test
    fun symmetricKeysKeepTheirBytes() {
        val key = SymmetricKey(data = hex("000102030405060708090a0b0c0d0e0f"))
        assertEquals(128, key.bitCount)
        assertContentEquals(hex("000102030405060708090a0b0c0d0e0f"), key.withUnsafeBytes { it })
        assertEquals(SymmetricKey(data = hex("000102030405060708090a0b0c0d0e0f")), key)
        assertEquals(256, SymmetricKey(size = SymmetricKeySize.bits256).bitCount)
        assertEquals(192, SymmetricKey(size = SymmetricKeySize(bitCount = 192)).bitCount)
        assertNotEquals(SymmetricKey(size = SymmetricKeySize.bits128), SymmetricKey(size = SymmetricKeySize.bits128))
        val digestKey = SymmetricKey(data = SHA256.hash(data = abc))
        assertContentEquals(SHA256.hash(data = abc).toByteArray(), digestKey.withUnsafeBytes { it })
    }
}
