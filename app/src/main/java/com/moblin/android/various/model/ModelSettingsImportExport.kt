package com.moblin.android.various.model

import android.content.ClipboardManager
import android.content.Context
import com.moblin.android.localized
import com.moblin.android.various.utils.moblinSettingsFileType
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val settingsImportScope = CoroutineScope(Dispatchers.IO)

fun Model.importSettingsWithConfirmation(action: () -> Unit) {
    pendingSettingsImportAction = action
    presentingSettingsImportConfirmation.value = true
}

fun Model.importSettingsFromFile(url: String, completion: (Boolean) -> Unit) {
    cleanupBeforeImport()
    settings.importFromFile(File(url).toURI()) { message ->
        importDone(message)
        completion(message == null)
    }
}

fun Model.importSettingsFromData(
    context: Context,
    settings: ByteArray,
    completion: (Boolean) -> Unit,
) {
    val settingsFile = File(context.cacheDir, "data_import.moblinSettings")
    runCatching { settingsFile.writeBytes(settings) }
    importSettingsFromFile(settingsFile.absolutePath) { success ->
        runCatching { settingsFile.delete() }
        completion(success)
    }
}

fun Model.importSettingsFromClipboard(context: Context, completion: () -> Unit) {
    val typeIdentifier = moblinSettingsFileType
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val clip = clipboard?.primaryClip
    val settingsUri = if (clip != null && clip.itemCount > 0) {
        (0 until clip.itemCount)
            .map { clip.getItemAt(it).uri }
            .firstOrNull { it != null && context.contentResolver.getType(it) == typeIdentifier }
    } else {
        null
    }
    val clipboardText = if (clip != null && clip.itemCount > 0) {
        clip.getItemAt(0).text?.toString()
    } else {
        null
    }
    if (settingsUri != null) {
        settingsImportScope.launch {
            val data = runCatching {
                context.contentResolver.openInputStream(settingsUri)?.use { it.readBytes() }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (data == null) {
                    importFailed(localized("No settings found in clipboard"))
                    completion()
                } else {
                    importSettingsFromData(context, data) {
                        completion()
                    }
                }
            }
        }
    } else if (clipboardText != null) {
        cleanupBeforeImport()
        settings.importFromClipboard(clipboardText) { message ->
            importDone(message)
            completion()
        }
    } else {
        importFailed(localized("No settings found in clipboard"))
        completion()
    }
}

fun Model.exportToFile(completion: (String?) -> Unit) {
    settings.exportToFile { uri -> completion(uri?.path) }
}

private fun Model.cleanupBeforeImport() {
    stopAllMacros()
}

private fun Model.importDone(message: String?) {
    if (message != null) {
        importFailed(message)
    } else {
        importSucceeded()
    }
}

private fun Model.importSucceeded() {
    setDebugLogging(database.debug.debugLogging.value)
    setCurrentStream()
    updateIconImageFromDatabase()
    updateMicsList()
    show.chatPhone.value = isChatPhone()
    updateScreenAutoOff()
    reloadStream()
    lutUpdated()
    chatBotCustomCommandsTextChanged()
    macrosTextFormatChanged()
    resetSelectedScene()
    setupAudioAfterSettingsImport()
    updateQuickButtonPairs()
    loadStealthModeImage()
    loadControlBarBackgroundImage()
    loadFaceBackgroundImage()
    reloadDjiDevicesAfterSettingsImport()
    reloadGoProDevicesAfterSettingsImport()
    reloadHttpProxyServer()
    makeToast(localized("Settings imported"))
}

private fun Model.importFailed(message: String) {
    makeErrorToast(localized("Import settings failed"), subTitle = message)
}
