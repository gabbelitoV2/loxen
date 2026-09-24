package com.moblin.android.platform

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.DeterministicAead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import com.google.crypto.tink.subtle.Base64
import com.google.crypto.tink.subtle.Hex
import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

object KeychainPreferences {
    private const val TAG = "KeychainPreferences"
    internal const val keyKeysetName = "__androidx_security_crypto_encrypted_prefs_key_keyset__"
    internal const val valueKeysetName = "__androidx_security_crypto_encrypted_prefs_value_keyset__"

    private enum class KeysetState {
        readable,
        unreadable,
        unknown,
    }

    fun create(
        context: Context,
        fileName: String,
        masterKey: MasterKey,
        prefKeyEncryptionScheme: EncryptedSharedPreferences.PrefKeyEncryptionScheme,
        prefValueEncryptionScheme: EncryptedSharedPreferences.PrefValueEncryptionScheme,
        masterKeyAlias: String = MasterKey.DEFAULT_MASTER_KEY_ALIAS,
    ): SharedPreferences {
        val open = {
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                prefKeyEncryptionScheme,
                prefValueEncryptionScheme,
            )
        }
        val preferences = try {
            open()
        } catch (exception: Exception) {
            if (!isLostForGood(context, fileName, masterKeyAlias)) {
                throw exception
            }
            Log.w(TAG, "Recreating $fileName, the Keystore key that encrypted it is gone: ${exception.message}")
            if (!rawPreferences(context, fileName).edit().clear().commit()) {
                throw exception
            }
            open()
        }
        dropUnreadableItems(context, fileName, masterKeyAlias, preferences)
        return preferences
    }

    private fun rawPreferences(context: Context, fileName: String): SharedPreferences {
        return (context.applicationContext ?: context).getSharedPreferences(fileName, Context.MODE_PRIVATE)
    }

    private fun isKeysetName(name: String): Boolean {
        return name == keyKeysetName || name == valueKeysetName
    }

    private fun isLostForGood(context: Context, fileName: String, masterKeyAlias: String): Boolean {
        val keysets = try {
            rawPreferences(context, fileName).all.filterKeys { isKeysetName(it) }.values
        } catch (exception: Exception) {
            return false
        }
        if (keysets.isEmpty()) {
            return false
        }
        val masterAead = workingMasterAead(masterKeyAlias) ?: return false
        val states = keysets.map { keysetState(it, masterAead) }
        return KeysetState.unknown !in states && KeysetState.unreadable in states
    }

    private fun workingMasterAead(masterKeyAlias: String): Aead? {
        return try {
            val masterAead = AndroidKeystoreKmsClient().getAead(AndroidKeystoreKmsClient.PREFIX + masterKeyAlias)
            val message = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val associatedData = ByteArray(0)
            val decrypted = masterAead.decrypt(masterAead.encrypt(message, associatedData), associatedData)
            if (decrypted.contentEquals(message)) masterAead else null
        } catch (exception: Exception) {
            Log.w(TAG, "The Keystore key $masterKeyAlias does not work: ${exception.message}")
            null
        }
    }

    private fun keysetState(keyset: Any?, masterAead: Aead): KeysetState {
        val bytes = (keyset as? String)?.let { runCatching { Hex.decode(it) }.getOrNull() }
        if (bytes == null || bytes.isEmpty()) {
            return KeysetState.unreadable
        }
        return try {
            readKeyset(bytes, masterAead)
            KeysetState.readable
        } catch (exception: AEADBadTagException) {
            KeysetState.unreadable
        } catch (exception: IOException) {
            KeysetState.unreadable
        } catch (exception: Exception) {
            KeysetState.unknown
        }
    }

    private fun readKeyset(bytes: ByteArray, masterAead: Aead): KeysetHandle {
        return try {
            KeysetHandle.read(BinaryKeysetReader.withBytes(bytes), masterAead)
        } catch (exception: Exception) {
            runCatching { CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(bytes)) }.getOrElse { throw exception }
        }
    }

    private fun isUnreadableValue(exception: Exception): Boolean {
        return exception is SecurityException ||
            exception is IllegalArgumentException ||
            exception is BufferUnderflowException ||
            exception is ClassCastException
    }

    private fun dropUnreadableItems(
        context: Context,
        fileName: String,
        masterKeyAlias: String,
        preferences: SharedPreferences,
    ) {
        try {
            preferences.all
            return
        } catch (exception: Exception) {
            Log.w(TAG, "Some items of $fileName cannot be decrypted: ${exception.message}")
        }
        val raw = rawPreferences(context, fileName)
        val keyKeyset = raw.getString(keyKeysetName, null) ?: throw GeneralSecurityException("$fileName has no key keyset")
        val masterAead = AndroidKeystoreKmsClient().getAead(AndroidKeystoreKmsClient.PREFIX + masterKeyAlias)
        val nameAead = readKeyset(Hex.decode(keyKeyset), masterAead).getPrimitive(DeterministicAead::class.java)
        val associatedData = fileName.toByteArray(Charset.defaultCharset())
        val unreadable = mutableListOf<String>()
        for (encryptedName in raw.all.keys) {
            if (isKeysetName(encryptedName)) {
                continue
            }
            val name = try {
                val decrypted = nameAead.decryptDeterministically(Base64.decode(encryptedName, Base64.DEFAULT), associatedData)
                String(decrypted, Charsets.UTF_8)
            } catch (exception: GeneralSecurityException) {
                unreadable.add(encryptedName)
                continue
            } catch (exception: IllegalArgumentException) {
                unreadable.add(encryptedName)
                continue
            }
            try {
                preferences.getString(name, null)
            } catch (exception: Exception) {
                if (!isUnreadableValue(exception)) {
                    throw exception
                }
                unreadable.add(encryptedName)
            }
        }
        val editor = raw.edit()
        for (encryptedName in unreadable) {
            editor.remove(encryptedName)
        }
        if (!editor.commit()) {
            throw IOException("Failed to drop ${unreadable.size} unreadable items of $fileName")
        }
        Log.w(TAG, "Dropped ${unreadable.size} unreadable items of $fileName")
        preferences.all
    }
}
