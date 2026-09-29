package com.moblin.android.various

import android.content.Context
import android.content.SharedPreferences
import com.moblin.android.platform.log.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class Keychain(
    private val streamId: String,
    private val server: String,
    private val logPrefix: String,
) {
    fun store(value: String) {
        if (!update(value = value)) {
            add(value = value)
        }
    }

    fun load(): String? {
        val preferences = preferences() ?: return null
        if (!preferences.contains(streamId)) {
            return null
        }
        val value = preferences.getString(streamId, null)
        if (value == null) {
            Log.i(tag, "$logPrefix: Failed to lookup attributes")
            return null
        }
        return value
    }

    fun remove() {
        val preferences = preferences() ?: return
        if (!preferences.edit().remove(streamId).commit()) {
            Log.i(tag, "$logPrefix: Keychain delete failed")
        }
    }

    private fun update(value: String): Boolean {
        val preferences = preferences() ?: return false
        if (!preferences.contains(streamId)) {
            return false
        }
        if (!preferences.edit().putString(streamId, value).commit()) {
            Log.i(tag, "$logPrefix: Failed to update item in keychain")
            return false
        }
        return true
    }

    private fun add(value: String) {
        val preferences = preferences() ?: return
        if (!preferences.edit().putString(streamId, value).commit()) {
            Log.i(tag, "$logPrefix: Failed to add item to keychain")
        }
    }

    private fun preferences(): SharedPreferences? {
        return preferences(server)
    }

    companion object {
        private const val tag = "Keychain"
        private const val fileNamePrefix = "moblin_keychain_"

        var appContext: Context? = null

        private val preferencesCache = mutableMapOf<String, SharedPreferences>()

        fun loadStreamIds(server: String): List<String> {
            val preferences = preferences(server)
            if (preferences == null) {
                Log.i(tag, "keychain: Failed to query items of server $server")
                return emptyList()
            }
            return preferences.all.keys.toList()
        }

        private fun preferences(server: String): SharedPreferences? {
            synchronized(preferencesCache) {
                val cached = preferencesCache[server]
                if (cached != null) {
                    return cached
                }
                val context = appContext ?: runCatching { com.moblin.android.AppDelegate.context }.getOrNull()
                if (context == null) {
                    Log.i(tag, "keychain: Failed to query items of server $server without an application context")
                    return null
                }
                val masterKey = runCatching { MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build() }.getOrElse { exception -> Log.i(tag, "keychain: Failed to open keychain of server $server: ${exception.message}"); return null }
                val preferences = try {
                    com.moblin.android.platform.KeychainPreferences.create(
                        context,
                        fileNamePrefix + sha256Hex(server),
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    )
                } catch (exception: Exception) {
                    Log.i(tag, "keychain: Failed to open keychain of server $server: ${exception.message}")
                    return null
                }
                preferencesCache[server] = preferences
                return preferences
            }
        }

        private fun sha256Hex(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
        }
    }
}
