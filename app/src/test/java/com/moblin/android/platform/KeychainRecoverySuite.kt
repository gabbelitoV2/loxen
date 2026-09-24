package com.moblin.android.platform

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.MasterKey
import com.moblin.android.AppDelegate
import com.moblin.android.various.Keychain
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.MessageDigest
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.UnrecoverableKeyException
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val keystoreName = "AndroidKeyStore"
private const val twitchServer = "id.twitch.tv"
private const val kickServer = "kick.com"

private class StandInKeystore {
    val keys: MutableMap<String, Key> = Collections.synchronizedMap(mutableMapOf())

    @Volatile
    var failing = false

    fun generateMasterKey() {
        val material = ByteArray(32).also { SecureRandom().nextBytes(it) }
        keys[MasterKey.DEFAULT_MASTER_KEY_ALIAS] = SecretKeySpec(material, "AES")
    }
}

private class StandInKeystoreSpi(private val keystore: StandInKeystore) : KeyStoreSpi() {
    override fun engineGetKey(alias: String?, password: CharArray?): Key? {
        if (keystore.failing) {
            throw UnrecoverableKeyException("Keystore is unavailable")
        }
        return keystore.keys[alias]
    }

    override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null

    override fun engineGetCertificate(alias: String?): Certificate? = null

    override fun engineGetCreationDate(alias: String?): Date? = if (engineContainsAlias(alias)) Date() else null

    override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<out Certificate>?) {
        keystore.keys[checkNotNull(alias)] = checkNotNull(key)
    }

    override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) {
        throw UnsupportedOperationException()
    }

    override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) {
        throw UnsupportedOperationException()
    }

    override fun engineDeleteEntry(alias: String?) {
        keystore.keys.remove(alias)
    }

    override fun engineAliases(): Enumeration<String> = Collections.enumeration(keystore.keys.keys.toList())

    override fun engineContainsAlias(alias: String?): Boolean = keystore.keys.containsKey(alias)

    override fun engineSize(): Int = keystore.keys.size

    override fun engineIsKeyEntry(alias: String?): Boolean = engineContainsAlias(alias)

    override fun engineIsCertificateEntry(alias: String?): Boolean = false

    override fun engineGetCertificateAlias(cert: Certificate?): String? = null

    override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit

    override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
}

private class StandInKeystoreProvider(keystore: StandInKeystore) :
    Provider(keystoreName, 1.0, "Android Keystore stand-in for Keychain tests") {
    init {
        putService(
            object : Provider.Service(this, "KeyStore", keystoreName, StandInKeystoreSpi::class.java.name, null, null) {
                override fun newInstance(constructorParameter: Any?): Any = StandInKeystoreSpi(keystore)
            },
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class KeychainRecoverySuite {
    private val keystore = StandInKeystore()

    private val context: Context
        get() = AppDelegate.context

    @Before
    fun setUp() {
        Security.removeProvider(keystoreName)
        Security.addProvider(StandInKeystoreProvider(keystore))
        keystore.generateMasterKey()
        Keychain.appContext = null
        restart()
    }

    @After
    fun tearDown() {
        Security.removeProvider(keystoreName)
        restart()
    }

    private fun restart() {
        @Suppress("UNCHECKED_CAST")
        val cache = Keychain::class.java.getMethod("access\$getPreferencesCache\$cp").invoke(null) as MutableMap<String, *>
        synchronized(cache) {
            cache.clear()
        }
    }

    private fun keychain(server: String, streamId: String): Keychain {
        return Keychain(streamId = streamId, server = server, logPrefix = "test")
    }

    private fun fileName(server: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(server.toByteArray(Charsets.UTF_8))
        return "moblin_keychain_" + digest.joinToString("") { "%02x".format(it) }
    }

    private fun raw(server: String): SharedPreferences {
        return context.getSharedPreferences(fileName(server), Context.MODE_PRIVATE)
    }

    private fun encryptedNames(server: String): Set<String> {
        return raw(server).all.keys - setOf(KeychainPreferences.keyKeysetName, KeychainPreferences.valueKeysetName)
    }

    private fun file(server: String): File {
        return context.dataDir.walkTopDown().single { it.name == "${fileName(server)}.xml" }
    }

    private fun storeAndGetEncryptedName(server: String, streamId: String, value: String): String {
        val before = encryptedNames(server)
        keychain(server, streamId).store(value = value)
        return (encryptedNames(server) - before).single()
    }

    private fun tampered(text: String): String {
        val characters = text.toCharArray()
        val index = characters.size / 2
        characters[index] = if (characters[index] == 'A') 'B' else 'A'
        return String(characters)
    }

    @Test
    fun normalOperation() {
        val first = keychain(twitchServer, "first")
        first.store(value = "token-1")
        assertEquals("token-1", first.load())
        first.store(value = "token-2")
        assertEquals("token-2", first.load())
        keychain(twitchServer, "second").store(value = "token-3")
        keychain(kickServer, "first").store(value = "kick-token")
        assertEquals(setOf("first", "second"), Keychain.loadStreamIds(twitchServer).toSet())
        val stored = file(twitchServer).readBytes()
        assertFalse(String(stored).contains("token-"))
        restart()
        assertEquals("token-2", keychain(twitchServer, "first").load())
        assertEquals("token-3", keychain(twitchServer, "second").load())
        assertEquals("kick-token", keychain(kickServer, "first").load())
        assertEquals(setOf("first", "second"), Keychain.loadStreamIds(twitchServer).toSet())
        assertContentEquals(stored, file(twitchServer).readBytes())
        keychain(twitchServer, "first").remove()
        assertNull(keychain(twitchServer, "first").load())
        restart()
        assertNull(keychain(twitchServer, "first").load())
        assertEquals("token-3", keychain(twitchServer, "second").load())
        assertEquals(listOf("second"), Keychain.loadStreamIds(twitchServer))
        assertEquals("kick-token", keychain(kickServer, "first").load())
    }

    @Test
    fun lostKeyDropsTheOldItemsAndLaterSavesAndReadsWork() {
        keychain(twitchServer, "first").store(value = "old-1")
        keychain(twitchServer, "second").store(value = "old-2")
        keychain(kickServer, "first").store(value = "old-kick")
        val oldNames = encryptedNames(twitchServer)
        assertEquals(2, oldNames.size)
        keystore.generateMasterKey()
        restart()
        assertNull(keychain(twitchServer, "first").load())
        assertNull(keychain(twitchServer, "second").load())
        assertTrue(Keychain.loadStreamIds(twitchServer).isEmpty())
        assertTrue(encryptedNames(twitchServer).isEmpty())
        keychain(twitchServer, "first").store(value = "new-1")
        assertEquals("new-1", keychain(twitchServer, "first").load())
        restart()
        assertEquals("new-1", keychain(twitchServer, "first").load())
        assertNull(keychain(twitchServer, "second").load())
        assertEquals(listOf("first"), Keychain.loadStreamIds(twitchServer))
        assertTrue((encryptedNames(twitchServer) intersect oldNames).isEmpty())
        assertTrue(Keychain.loadStreamIds(kickServer).isEmpty())
        keychain(kickServer, "first").store(value = "new-kick")
        restart()
        assertEquals("new-kick", keychain(kickServer, "first").load())
        assertEquals("new-1", keychain(twitchServer, "first").load())
    }

    @Test
    fun partialCorruptionDropsOnlyTheUnreadableItems() {
        val corruptValue = storeAndGetEncryptedName(twitchServer, "corrupt-value", "a")
        val corruptName = storeAndGetEncryptedName(twitchServer, "corrupt-name", "b")
        val kept = storeAndGetEncryptedName(twitchServer, "kept", "c")
        val wrongType = storeAndGetEncryptedName(twitchServer, "wrong-type", "d")
        keychain(kickServer, "other").store(value = "e")
        val preferences = raw(twitchServer)
        val corruptNameValue = checkNotNull(preferences.getString(corruptName, null))
        val corruptValueValue = checkNotNull(preferences.getString(corruptValue, null))
        assertTrue(
            preferences.edit()
                .putString(corruptValue, tampered(corruptValueValue))
                .remove(corruptName)
                .putString(tampered(corruptName), corruptNameValue)
                .putInt(wrongType, 1)
                .putString("not-an-encrypted-name", "garbage")
                .commit(),
        )
        restart()
        assertEquals("c", keychain(twitchServer, "kept").load())
        assertNull(keychain(twitchServer, "corrupt-value").load())
        assertNull(keychain(twitchServer, "corrupt-name").load())
        assertNull(keychain(twitchServer, "wrong-type").load())
        assertEquals(listOf("kept"), Keychain.loadStreamIds(twitchServer))
        assertEquals(setOf(kept), encryptedNames(twitchServer))
        assertEquals("e", keychain(kickServer, "other").load())
        keychain(twitchServer, "corrupt-value").store(value = "new-a")
        assertEquals("new-a", keychain(twitchServer, "corrupt-value").load())
        restart()
        assertEquals("new-a", keychain(twitchServer, "corrupt-value").load())
        assertEquals("c", keychain(twitchServer, "kept").load())
        assertEquals(setOf("corrupt-value", "kept"), Keychain.loadStreamIds(twitchServer).toSet())
    }

    @Test
    fun unreadableKeysetRecreatesTheStore() {
        keychain(twitchServer, "first").store(value = "old")
        keychain(kickServer, "first").store(value = "kick")
        assertTrue(raw(twitchServer).edit().putString(KeychainPreferences.valueKeysetName, "not a keyset").commit())
        restart()
        assertNull(keychain(twitchServer, "first").load())
        assertTrue(encryptedNames(twitchServer).isEmpty())
        keychain(twitchServer, "first").store(value = "new")
        restart()
        assertEquals("new", keychain(twitchServer, "first").load())
        assertEquals("kick", keychain(kickServer, "first").load())
    }

    @Test
    fun failingKeystoreKeepsTheItems() {
        keychain(twitchServer, "first").store(value = "kept")
        val stored = file(twitchServer).readBytes()
        keystore.failing = true
        restart()
        assertNull(keychain(twitchServer, "first").load())
        keychain(twitchServer, "first").store(value = "not-saved")
        keychain(twitchServer, "second").store(value = "not-saved")
        assertTrue(Keychain.loadStreamIds(twitchServer).isEmpty())
        assertContentEquals(stored, file(twitchServer).readBytes())
        keystore.failing = false
        restart()
        assertEquals("kept", keychain(twitchServer, "first").load())
        assertNull(keychain(twitchServer, "second").load())
        assertEquals(listOf("first"), Keychain.loadStreamIds(twitchServer))
    }
}
