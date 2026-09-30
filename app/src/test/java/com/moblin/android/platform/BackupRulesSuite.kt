package com.moblin.android.platform

import android.content.Context
import android.util.Xml
import com.moblin.android.AppDelegate
import com.moblin.android.R
import com.moblin.android.streamingplatforms.kick.loadKickAccessTokenFromKeychain
import com.moblin.android.streamingplatforms.kick.storeKickAccessTokenInKeychain
import com.moblin.android.streamingplatforms.twitch.loadTwitchAccessTokenFromKeychain
import com.moblin.android.streamingplatforms.twitch.storeTwitchAccessTokenInKeychain
import com.moblin.android.streamingplatforms.youtube.loadYouTubeAuthStateFromKeychain
import com.moblin.android.streamingplatforms.youtube.storeYouTubeAuthStateInKeychain
import com.moblin.android.various.Keychain
import com.moblin.android.various.model.controlBarBackgroundImagePath
import com.moblin.android.various.model.stealthModeImagePath
import com.moblin.android.various.settings.Settings
import com.moblin.android.various.settings.SettingsStream
import com.moblin.android.various.storages.AlertMediaStorage
import com.moblin.android.various.storages.ImageStorage
import com.moblin.android.various.storages.MediaPlayerStorage
import com.moblin.android.various.storages.PngTuberStorage
import com.moblin.android.various.storages.ReplayTransitionsStorage
import com.moblin.android.various.storages.SimpleStringStorage
import com.moblin.android.various.storages.VTuberStorage
import com.moblin.android.various.storages.pngTuberStorageDirectory
import com.moblin.android.various.utils.createAndGetDirectory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import java.util.UUID
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

private const val androidNamespace = "http://schemas.android.com/apk/res/android"
private const val masterKeyAlias = "_androidx_security_master_key_"

class FakeAndroidKeyStoreSpi : KeyStoreSpi() {
    override fun engineGetKey(alias: String?, password: CharArray?): Key? = fakeAndroidKeyStoreKeys[alias]

    override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null

    override fun engineGetCertificate(alias: String?): Certificate? = null

    override fun engineGetCreationDate(alias: String?): Date? = if (engineContainsAlias(alias)) Date() else null

    override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<out Certificate>?) {
        fakeAndroidKeyStoreKeys[checkNotNull(alias)] = checkNotNull(key)
    }

    override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) {
        throw UnsupportedOperationException()
    }

    override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) {
        throw UnsupportedOperationException()
    }

    override fun engineDeleteEntry(alias: String?) {
        fakeAndroidKeyStoreKeys.remove(alias)
    }

    override fun engineAliases(): Enumeration<String> = Collections.enumeration(fakeAndroidKeyStoreKeys.keys.toList())

    override fun engineContainsAlias(alias: String?): Boolean = fakeAndroidKeyStoreKeys.containsKey(alias)

    override fun engineSize(): Int = fakeAndroidKeyStoreKeys.size

    override fun engineIsKeyEntry(alias: String?): Boolean = engineContainsAlias(alias)

    override fun engineIsCertificateEntry(alias: String?): Boolean = false

    override fun engineGetCertificateAlias(cert: Certificate?): String? = null

    override fun engineStore(stream: OutputStream?, password: CharArray?) = Unit

    override fun engineLoad(stream: InputStream?, password: CharArray?) = Unit
}

private val fakeAndroidKeyStoreKeys: MutableMap<String, Key> = Collections.synchronizedMap(mutableMapOf())

private class FakeAndroidKeyStoreProvider : Provider("AndroidKeyStore", 1.0, "Android Keystore stand-in for tests") {
    init {
        put("KeyStore.AndroidKeyStore", FakeAndroidKeyStoreSpi::class.java.name)
    }
}

private enum class BackupKind {
    beforeAndroid12,
    cloudBackup,
    deviceTransfer,
}

private data class BackupRule(val include: Boolean, val domain: String, val path: String)

@RunWith(RobolectricTestRunner::class)
class BackupRulesSuite {
    private val context: Context
        get() = AppDelegate.context

    private val dataDirectory: File
        get() = context.dataDir.canonicalFile

    @Before
    fun setUp() {
        Keychain.appContext = null
        clearKeychainCache()
    }

    @After
    fun tearDown() {
        Security.removeProvider("AndroidKeyStore")
        fakeAndroidKeyStoreKeys.clear()
        clearKeychainCache()
    }

    private fun clearKeychainCache() {
        @Suppress("UNCHECKED_CAST")
        val cache = Keychain::class.java.getMethod("access\$getPreferencesCache\$cp").invoke(null) as MutableMap<String, *>
        synchronized(cache) {
            cache.clear()
        }
    }

    private fun installKeystoreWithNewMasterKey() {
        if (Security.getProvider("AndroidKeyStore") == null) {
            Security.addProvider(FakeAndroidKeyStoreProvider())
        }
        fakeAndroidKeyStoreKeys.clear()
        fakeAndroidKeyStoreKeys[masterKeyAlias] = SecretKeySpec(ByteArray(32).also { SecureRandom().nextBytes(it) }, "AES")
        clearKeychainCache()
    }

    private fun sourceFile(path: String): File {
        return listOf(File(path), File("app", path)).firstOrNull { it.isFile } ?: fail("Missing $path")
    }

    private fun manifestApplicationAttributes(): Map<String, String> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        sourceFile("src/main/AndroidManifest.xml").inputStream().use { stream ->
            parser.setInput(stream, "utf-8")
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "application") {
                    return (0 until parser.attributeCount)
                        .filter { parser.getAttributeNamespace(it) == androidNamespace }
                        .associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
                }
            }
        }
        fail("No application element in the manifest")
    }

    private fun xmlResourceFile(reference: String?): File {
        assertNotNull(reference)
        assertTrue(reference.startsWith("@xml/"), reference)
        return sourceFile("src/main/res/xml/${reference.removePrefix("@xml/")}.xml")
    }

    private fun parseRules(file: File): Map<String, List<BackupRule>> {
        val sections = mutableMapOf<String, MutableList<BackupRule>>()
        val parser = Xml.newPullParser()
        file.inputStream().use { stream ->
            parser.setInput(stream, "utf-8")
            var section: String? = null
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                when (parser.name) {
                    "full-backup-content", "cloud-backup", "device-transfer" -> {
                        section = parser.name
                        sections[parser.name] = mutableListOf()
                    }
                    "data-extraction-rules" -> Unit
                    "include", "exclude" -> {
                        val attributes = (0 until parser.attributeCount).associate {
                            parser.getAttributeName(it) to parser.getAttributeValue(it)
                        }
                        assertEquals(setOf("domain", "path"), attributes.keys, "${file.name}: $attributes")
                        sections.getValue(checkNotNull(section)).add(
                            BackupRule(
                                include = parser.name == "include",
                                domain = attributes.getValue("domain"),
                                path = attributes.getValue("path"),
                            ),
                        )
                    }
                    else -> fail("${file.name}: unexpected element ${parser.name}")
                }
            }
        }
        return sections
    }

    private fun rules(kind: BackupKind): List<BackupRule> {
        val attributes = manifestApplicationAttributes()
        return when (kind) {
            BackupKind.beforeAndroid12 ->
                parseRules(xmlResourceFile(attributes["fullBackupContent"])).getValue("full-backup-content")
            BackupKind.cloudBackup ->
                parseRules(xmlResourceFile(attributes["dataExtractionRules"])).getValue("cloud-backup")
            BackupKind.deviceTransfer ->
                parseRules(xmlResourceFile(attributes["dataExtractionRules"])).getValue("device-transfer")
        }
    }

    private fun domainDirectory(domain: String, dataDirectory: File): File {
        return when (domain) {
            "root" -> dataDirectory
            "file" -> File(dataDirectory, "files")
            "database" -> File(dataDirectory, "databases")
            "sharedpref" -> File(dataDirectory, "shared_prefs")
            else -> fail("Unexpected backup domain $domain")
        }
    }

    private fun File.isInside(directory: File): Boolean {
        return canonicalFile.toPath().startsWith(directory.canonicalFile.toPath())
    }

    private fun isBackedUp(rules: List<BackupRule>, file: File, dataDirectory: File = this.dataDirectory): Boolean {
        assertTrue(file.isInside(dataDirectory), "$file")
        val includes = rules.filter { it.include }.map { File(domainDirectory(it.domain, dataDirectory), it.path) }
        val excludes = rules.filter { !it.include }.map { File(domainDirectory(it.domain, dataDirectory), it.path) }
        val never = listOf("cache", "code_cache", "no_backup").map { File(dataDirectory, it) }
        val included = if (includes.isEmpty()) {
            never.none { file.isInside(it) }
        } else {
            includes.any { file.isInside(it) }
        }
        return included && excludes.none { file.isInside(it) }
    }

    private fun simpleStorageDirectory(): File {
        val storages = Class.forName("com.moblin.android.various.storages.SimpleStorageKt")
        val field = storages.getDeclaredField("defaultDirectory\$delegate")
        field.isAccessible = true
        val directory = (field.get(null) as Lazy<*>).value as File
        directory.mkdirs()
        return directory.canonicalFile
    }

    private fun write(file: File, text: String): File {
        file.parentFile?.mkdirs()
        file.writeText(text)
        return file
    }

    private fun keychainFiles(): List<File> {
        return dataDirectory.walkTopDown().filter { it.isFile && it.name.startsWith("moblin_keychain_") }.toList()
    }

    private fun storeSecrets(streamId: UUID) {
        storeTwitchAccessTokenInKeychain(streamId, "twitch-token")
        storeKickAccessTokenInKeychain(streamId, "kick-token")
        storeYouTubeAuthStateInKeychain(streamId, "youtube-state")
    }

    private fun documentsFiles(): List<File> {
        val id = UUID.randomUUID()
        ImageStorage().write(id, byteArrayOf(1))
        PngTuberStorage().write(id, byteArrayOf(1))
        VTuberStorage().write(id, byteArrayOf(1))
        AlertMediaStorage().write(id, byteArrayOf(1))
        return listOf(
            write(File(createAndGetDirectory("Recordings"), "recording.mp4"), "recording"),
            write(File(createAndGetDirectory("Replays"), "replay.mp4"), "replay"),
            write(File(createAndGetDirectory("Logs"), "log.txt"), "log"),
            write(ReplayTransitionsStorage().makePath("$id.mov"), "stinger"),
            write(MediaPlayerStorage().makePath(id), "media"),
            write(AlertMediaStorage().videos.makePath("$id.mp4"), "alert video"),
            write(stealthModeImagePath, "stealth"),
            write(controlBarBackgroundImagePath, "control bar"),
            ImageStorage().makePath(id),
            File(createAndGetDirectory(pngTuberStorageDirectory), id.toString()),
            VTuberStorage().path(id),
            AlertMediaStorage().makePath(id),
        )
    }

    @Test
    fun manifestPointsToBothRuleFormats() {
        val attributes = manifestApplicationAttributes()
        assertEquals("true", attributes["allowBackup"])
        assertEquals("@xml/backup_rules", attributes["fullBackupContent"])
        assertEquals("@xml/data_extraction_rules", attributes["dataExtractionRules"])
        assertTrue(R.xml.backup_rules != 0)
        assertTrue(R.xml.data_extraction_rules != 0)
        for (kind in BackupKind.entries) {
            assertTrue(rules(kind).any { it.include }, "$kind")
        }
    }

    @Test
    fun androidLayoutMatchesTheRuleDomains() {
        assertEquals(File(dataDirectory, "files"), context.filesDir.canonicalFile)
        assertEquals(File(dataDirectory, "databases"), context.getDatabasePath("probe").parentFile?.canonicalFile)
        context.getSharedPreferences("backup_rules_probe", Context.MODE_PRIVATE).edit().putString("a", "b").commit()
        val probe = dataDirectory.walkTopDown().single { it.name == "backup_rules_probe.xml" }
        assertEquals(File(dataDirectory, "shared_prefs"), probe.parentFile?.canonicalFile)
    }

    @Test
    fun settingsAreBackedUp() {
        val setup = Class.forName("com.moblin.android.various.storages.SimpleStorageKt").getDeclaredMethod("setup")
        setup.isAccessible = true
        val directory = (setup.invoke(null) as File).canonicalFile
        assertEquals(File(context.filesDir, "SimpleStorage").canonicalFile, directory)
        val files = listOf("settings", "replays", "streamingHistory", "srtlaRelayId", "moblinkServerId")
            .map { write(File(directory, it), "{}") }
        for (kind in BackupKind.entries) {
            for (file in files) {
                assertTrue(isBackedUp(rules(kind), file), "$kind: $file")
            }
        }
        val used = simpleStorageDirectory()
        assertEquals("SimpleStorage", used.name)
        assertEquals(context.filesDir.name, used.parentFile?.name)
        SimpleStringStorage("backupRulesProbe").set("value")
        assertTrue(File(used, "backupRulesProbe").isFile)
        for (kind in BackupKind.entries) {
            assertTrue(isBackedUp(rules(kind), File(used, "settings"), checkNotNull(used.parentFile?.parentFile)))
        }
    }

    @Test
    fun keychainAndDocumentsAreNotBackedUp() {
        installKeystoreWithNewMasterKey()
        val streamId = UUID.randomUUID()
        storeSecrets(streamId)
        assertEquals("twitch-token", loadTwitchAccessTokenFromKeychain(streamId))
        assertEquals("kick-token", loadKickAccessTokenFromKeychain(streamId))
        assertEquals("youtube-state", loadYouTubeAuthStateFromKeychain(streamId))
        val keychain = keychainFiles()
        assertEquals(3, keychain.size, "$keychain")
        val documents = documentsFiles()
        for (file in documents) {
            assertTrue(file.isFile, "$file")
            assertTrue(file.isInside(Documents.directory), "$file")
        }
        val inbox = write(DocumentPicker.inboxFile(context, "stinger.mov"), "picked")
        for (kind in BackupKind.entries) {
            val rules = rules(kind)
            for (file in keychain + documents + inbox) {
                assertFalse(isBackedUp(rules, file), "$kind: $file")
            }
            assertFalse(isBackedUp(rules, Documents.directory), "$kind")
            assertFalse(isBackedUp(rules, File(dataDirectory, "shared_prefs")), "$kind")
        }
    }

    @Test
    fun restoredSettingsWithoutKeychainHaveEmptySecrets() {
        installKeystoreWithNewMasterKey()
        val storageDirectory = simpleStorageDirectory()
        val storageDataDirectory = checkNotNull(storageDirectory.parentFile?.parentFile)
        val streamId = UUID.randomUUID()
        val settings = Settings()
        settings.load()
        settings.database.streams.add(SettingsStream(name = "Restored stream", id = streamId, url = "srt://example.com:4000"))
        settings.store()
        storeSecrets(streamId)
        val beforeBackup = Settings()
        beforeBackup.load()
        val stream = beforeBackup.database.streams.single { it.id == streamId }
        assertEquals("twitch-token", stream.twitchAccessToken)
        assertEquals("kick-token", stream.kickAccessToken)
        val stored = File(storageDirectory, "settings").readText()
        assertFalse(stored.contains("twitch-token"))
        assertFalse(stored.contains("kick-token"))
        documentsFiles()
        val rules = rules(BackupKind.cloudBackup)
        val deviceFiles = (dataDirectory.walkTopDown() + storageDirectory.walkTopDown())
            .filter { it.isFile }
            .map { it.canonicalFile }
            .distinct()
            .toList()
        val backup = deviceFiles.filter {
            isBackedUp(rules, it, if (it.isInside(storageDataDirectory)) storageDataDirectory else dataDirectory)
        }.associateWith { it.readBytes() }
        assertTrue(File(storageDirectory, "settings").canonicalFile in backup.keys)
        assertTrue(backup.keys.all { it.isInside(storageDirectory) }, "${backup.keys}")
        val keychain = keychainFiles().associateWith { it.readBytes() }
        assertEquals(3, keychain.size)

        for (name in File(dataDirectory, "shared_prefs").list().orEmpty()) {
            context.deleteSharedPreferences(name.removeSuffix(".xml"))
        }
        for (file in deviceFiles) {
            file.delete()
        }
        installKeystoreWithNewMasterKey()
        for ((file, bytes) in backup) {
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }
        assertTrue(keychainFiles().isEmpty())

        val restored = Settings()
        restored.load()
        val restoredStream = restored.database.streams.single { it.id == streamId }
        assertEquals("Restored stream", restoredStream.name)
        assertEquals("srt://example.com:4000", restoredStream.url)
        assertEquals("", restoredStream.twitchAccessToken)
        assertEquals("", restoredStream.kickAccessToken)
        assertNull(loadTwitchAccessTokenFromKeychain(streamId))
        assertNull(loadKickAccessTokenFromKeychain(streamId))
        assertNull(loadYouTubeAuthStateFromKeychain(streamId))
        storeTwitchAccessTokenInKeychain(streamId, "new-twitch-token")
        assertEquals("new-twitch-token", loadTwitchAccessTokenFromKeychain(streamId))

        for (name in File(dataDirectory, "shared_prefs").list().orEmpty()) {
            context.deleteSharedPreferences(name.removeSuffix(".xml"))
        }
        clearKeychainCache()
        for ((file, bytes) in keychain) {
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
        }
        assertNull(loadKickAccessTokenFromKeychain(streamId))
        assertNull(loadYouTubeAuthStateFromKeychain(streamId))
    }

    @Test
    fun restoredSettingsLoadWhenTheKeystoreFails() {
        assertNull(Security.getProvider("AndroidKeyStore"))
        simpleStorageDirectory()
        val streamId = UUID.randomUUID()
        val settings = Settings()
        settings.load()
        settings.database.streams.add(SettingsStream(name = "Restored stream", id = streamId))
        settings.store()
        storeTwitchAccessTokenInKeychain(streamId, "twitch-token")
        assertNull(loadTwitchAccessTokenFromKeychain(streamId))
        val restored = Settings()
        restored.load()
        val stream = restored.database.streams.single { it.id == streamId }
        assertEquals("Restored stream", stream.name)
        assertEquals("", stream.twitchAccessToken)
        assertEquals("", stream.kickAccessToken)
    }
}
